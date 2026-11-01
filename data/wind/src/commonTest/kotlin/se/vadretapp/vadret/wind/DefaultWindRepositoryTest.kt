package se.vadretapp.vadret.wind

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.testing.FixedClock
import se.vadretapp.vadret.testing.LogLine
import se.vadretapp.vadret.testing.mockHttpClient
import se.vadretapp.vadret.testing.withRecordedLogs
import se.vadretapp.vadret.wind.local.DefaultWindLocalDataSource
import se.vadretapp.vadret.wind.model.WindForecast
import se.vadretapp.vadret.wind.model.WindFrame
import se.vadretapp.vadret.wind.model.WindGrid
import se.vadretapp.vadret.wind.remote.DefaultWindRemoteDataSource
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.update
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

private const val REFERENCE_TIME = "2026-10-24T21:30:00Z"

private val gridCoordinates: String = "[[10.0,55.0],[12.0,55.1],[14.0,55.2],[10.1,57.0],[12.1,57.1],[14.1,57.2]]"

private fun timeOf(path: String): String = path.substringAfter("/time/").substringBefore("/")

class DefaultWindRepositoryTest {

    private val recordedRequests: AtomicReference<List<HttpRequestData>> = AtomicReference(value = emptyList())

    private val requests: List<HttpRequestData>
        get() = recordedRequests.load()

    private val clock = FixedClock(now = Instant.parse(input = "2026-10-24T21:50:00Z"))

    private var lastForecastTime: String = "20261025T090000Z"
    private var coordinates: String = gridCoordinates
    private var speedValues: String = "[1.0,2.0,3.0,4.0,5.0,9999]"

    private fun MockRequestHandleScope.answer(request: HttpRequestData): HttpResponseData {
        recordedRequests.update { recorded: List<HttpRequestData> -> recorded + request }
        val path: String = request.url.encodedPath
        if (path.endsWith("createdtime.json")) {
            return respond(
                content = """{"createdTime":"2026-10-24T21:49:23Z","referenceTime":"$REFERENCE_TIME"}""",
                headers = jsonHeaders
            )
        }
        val time: String = timeOf(path = path)
        val parameter: String = path.substringAfter("/parameter/").substringBefore("/")
        val values: String = if (parameter == "wind_speed") {
            speedValues
        } else {
            "[90,180,270,0,45,9999]"
        }
        val geometry: String = if (request.url.parameters["with-geo"] == "true") {
            """"geometry":{"type":"MultiPoint","coordinates":$coordinates},"""
        } else {
            ""
        }
        val timeSeries: String = if (time <= lastForecastTime) {
            """[{"time":"$time","data":{"$parameter":$values}}]"""
        } else {
            "[]"
        }
        return respond(
            content = """{"referenceTime":"$REFERENCE_TIME",$geometry"timeSeries":$timeSeries}""",
            status = HttpStatusCode.OK,
            headers = jsonHeaders
        )
    }

    private val windRepository: WindRepository = DefaultWindRepository(
        windRemoteDataSource = DefaultWindRemoteDataSource(
            httpClient = lazy { mockHttpClient { request: HttpRequestData -> answer(request = request) } }
        ),
        windLocalDataSource = DefaultWindLocalDataSource(),
        clock = clock
    )

    private fun Either<Failure, WindForecast>.forecast(): WindForecast = (this as Either.Right).b

    @Test
    fun `twelve hours start at the first hour after the model run`() =
        runTest {
            val windForecast: WindForecast = windRepository.getWindForecast().forecast()

            assertEquals(expected = Instant.parse(input = REFERENCE_TIME), actual = windForecast.referenceTime)
            assertEquals(
                expected = (0 until 12).map { hour: Int -> Instant.parse(input = "2026-10-24T22:00:00Z") + hour.hours },
                actual = windForecast.frames.map { windFrame: WindFrame -> windFrame.validAt }
            )
        }

    @Test
    fun `an old model run starts at the current hour`() =
        runTest {
            clock.now = Instant.parse(input = "2026-10-25T01:20:00Z")

            assertEquals(
                expected = Instant.parse(input = "2026-10-25T01:00:00Z"),
                actual = windRepository.getWindForecast().forecast().frames.first().validAt
            )
        }

    @Test
    fun `the grid keeps its rows and missing values become not a number`() =
        runTest {
            val windForecast: WindForecast = windRepository.getWindForecast().forecast()

            assertEquals(
                expected = WindGrid(
                    columnCount = 3,
                    rowCount = 2,
                    longitudes = listOf(10.0, 12.0, 14.0, 10.1, 12.1, 14.1),
                    latitudes = listOf(55.0, 55.1, 55.2, 57.0, 57.1, 57.2)
                ),
                actual = windForecast.grid
            )
            val firstFrame: WindFrame = windForecast.frames.first()
            assertEquals(expected = listOf(1f, 2f, 3f, 4f, 5f), actual = firstFrame.windSpeeds.take(n = 5))
            assertTrue(actual = firstFrame.windSpeeds.last().isNaN())
            assertEquals(
                expected = listOf(90f, 180f, 270f, 0f, 45f),
                actual = firstFrame.windFromDirections.take(n = 5)
            )
        }

    @Test
    fun `only the first speed request asks for the geometry`() =
        runTest {
            windRepository.getWindForecast()

            val withGeometry: List<HttpRequestData> = requests.filter { request: HttpRequestData -> request.url.parameters["with-geo"] == "true" }
            assertEquals(
                expected = listOf("20261024T220000Z"),
                actual = withGeometry.map { request: HttpRequestData -> timeOf(path = request.url.encodedPath) }
            )
            assertTrue(
                actual = requests
                    .filter { request: HttpRequestData -> "/time/" in request.url.encodedPath }
                    .all { request: HttpRequestData -> request.url.parameters["downsample"] == "10" }
            )
        }

    @Test
    fun `the forecast ends where smhi has no more hours`() =
        runTest {
            lastForecastTime = "20261025T010000Z"

            assertEquals(expected = 4, actual = windRepository.getWindForecast().forecast().frames.size)
        }

    @Test
    fun `no hours at all is no wind`() =
        runTest {
            lastForecastTime = "20261024T000000Z"

            withRecordedLogs { recordingLogSink ->
                assertEquals(
                    expected = Failure(errorCode = ErrorCode.WIND_NOT_AVAILABLE).asLeft(),
                    actual = windRepository.getWindForecast()
                )
                assertEquals(
                    expected = listOf(LogLevel.WARN),
                    actual = recordingLogSink.lines.map { line: LogLine -> line.level }
                )
            }
        }

    @Test
    fun `the same model run is fetched once`() =
        runTest {
            windRepository.getWindForecast()
            val firstRequestCount: Int = requests.size

            windRepository.getWindForecast()

            assertEquals(expected = 25, actual = firstRequestCount)
            assertEquals(expected = firstRequestCount + 1, actual = requests.size)
        }

    @Test
    fun `a new hour fetches again`() =
        runTest {
            windRepository.getWindForecast()
            clock.now = Instant.parse(input = "2026-10-24T23:05:00Z")

            assertEquals(
                expected = Instant.parse(input = "2026-10-24T23:00:00Z"),
                actual = windRepository.getWindForecast().forecast().frames.first().validAt
            )
            assertEquals(expected = 50, actual = requests.size)
        }

    @Test
    fun `values that do not match the grid are an invalid response`() =
        runTest {
            speedValues = "[1.0,2.0]"

            withRecordedLogs { recordingLogSink ->
                assertEquals(
                    expected = Failure(errorCode = ErrorCode.INVALID_RESPONSE).asLeft(),
                    actual = windRepository.getWindForecast()
                )
                assertEquals(
                    expected = listOf(LogLevel.WARN),
                    actual = recordingLogSink.lines.map { line: LogLine -> line.level }
                )
            }
        }

    @Test
    fun `a grid without regular rows is an invalid response`() =
        runTest {
            coordinates = "[[10.0,55.0],[12.0,55.1],[10.1,57.0],[12.1,57.1],[14.1,57.2],[16.1,57.3]]"

            withRecordedLogs { recordingLogSink ->
                assertEquals(
                    expected = Failure(errorCode = ErrorCode.INVALID_RESPONSE).asLeft(),
                    actual = windRepository.getWindForecast()
                )
                assertEquals(
                    expected = listOf(LogLevel.WARN),
                    actual = recordingLogSink.lines.map { line: LogLine -> line.level }
                )
            }
        }

    @Test
    fun `coordinates that are not pairs are an invalid response`() =
        runTest {
            coordinates = "[[10.0],[12.0,55.1],[14.0,55.2],[10.1,57.0],[12.1,57.1],[14.1,57.2]]"

            withRecordedLogs { recordingLogSink ->
                assertEquals(
                    expected = Failure(errorCode = ErrorCode.INVALID_RESPONSE).asLeft(),
                    actual = windRepository.getWindForecast()
                )
                assertEquals(
                    expected = listOf(LogLevel.WARN),
                    actual = recordingLogSink.lines.map { line: LogLine -> line.level }
                )
            }
        }
}