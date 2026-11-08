package se.vadretapp.vadret.aurora

import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.aurora.model.AuroraCellForecast
import se.vadretapp.vadret.aurora.model.AuroraHour
import se.vadretapp.vadret.aurora.remote.DefaultAuroraRemoteDataSource
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.testing.LogLine
import se.vadretapp.vadret.testing.mockHttpClient
import se.vadretapp.vadret.testing.withRecordedLogs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DefaultAuroraRepositoryTest {

    private val recordedRequests: MutableList<HttpRequestData> = mutableListOf()
    private var answer: String = AURORA_FORECAST
    private var status: HttpStatusCode = HttpStatusCode.OK

    private fun MockRequestHandleScope.answer(request: HttpRequestData): HttpResponseData {
        recordedRequests += request
        return respond(
            content = answer,
            status = status,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }

    private val auroraRepository: AuroraRepository = DefaultAuroraRepository(
        auroraRemoteDataSource = DefaultAuroraRemoteDataSource(
            httpClient = lazy { mockHttpClient { request: HttpRequestData -> answer(request = request) } }
        )
    )

    private suspend fun getAuroraForecast() =
        auroraRepository.getAuroraForecast(
            installationToken = INSTALLATION_TOKEN,
            geohashes = listOf(
                Geohash(value = "u3chr"),
                Geohash(value = "u6sce")
            )
        )

    @Test
    fun `the aurora forecast is asked with the installation token and the cells and read hour by hour`() =
        runTest {
            val auroraForecast = getAuroraForecast()

            assertEquals(
                expected = listOf(
                    AuroraCellForecast(
                        geohash = Geohash(value = "u6sce"),
                        hours = listOf(
                            AuroraHour(
                                time = Instant.parse(input = "2026-11-08T18:00:00Z"),
                                score = 42
                            ),
                            AuroraHour(
                                time = Instant.parse(input = "2026-11-08T19:00:00Z"),
                                score = 55
                            )
                        )
                    )
                ).asRight(),
                actual = auroraForecast
            )
            assertEquals(
                expected = listOf("https://api.vadretapp.se/api/v1/aurora?geohashes=u3chr%2Cu6sce" to "Bearer $INSTALLATION_TOKEN"),
                actual = recordedRequests.map { request: HttpRequestData -> request.url.toString() to request.headers[HttpHeaders.Authorization] }
            )
        }

    @Test
    fun `hours without a time or a score from 0 to 100 and unknown cells are logged and left out`() =
        runTest {
            answer = """{"cells":[{"geohash":"u6sce","hours":[{"time":"2026-11-08T18:00:00Z","score":42},{"time":"later","score":10},{"time":"2026-11-08T20:00:00Z","score":101},{"time":"2026-11-08T21:00:00Z"}]},{"geohash":"abc","hours":[]},{"hours":[]}]}"""

            withRecordedLogs { recordingLogSink ->
                assertEquals(
                    expected = listOf(
                        AuroraCellForecast(
                            geohash = Geohash(value = "u6sce"),
                            hours = listOf(
                                AuroraHour(
                                    time = Instant.parse(input = "2026-11-08T18:00:00Z"),
                                    score = 42
                                )
                            )
                        )
                    ).asRight(),
                    actual = getAuroraForecast()
                )
                assertEquals(
                    expected = listOf(
                        "Skipping 3 aurora hours in u6sce without a readable time or a score from 0 to 100",
                        "Skipping an aurora forecast for the unknown cell abc",
                        "Skipping an aurora forecast for the unknown cell null"
                    ),
                    actual = recordingLogSink.lines.map { line: LogLine -> line.message }
                )
            }
        }

    @Test
    fun `an answer without cells is an empty forecast`() =
        runTest {
            answer = "{}"

            assertEquals(
                expected = emptyList<AuroraCellForecast>().asRight(),
                actual = getAuroraForecast()
            )
        }

    @Test
    fun `a refused or broken answer is a failure`() =
        runTest {
            status = HttpStatusCode.BadRequest
            answer = """{"code":"AURORA_INVALID"}"""
            val refused = getAuroraForecast()
            status = HttpStatusCode.OK
            answer = "not json"
            val broken = getAuroraForecast()

            assertEquals(
                expected = listOf(
                    Failure(errorCode = ErrorCode.HTTP_BAD_REQUEST).asLeft(),
                    Failure(errorCode = ErrorCode.INVALID_RESPONSE).asLeft()
                ),
                actual = listOf(refused, broken)
            )
        }
}

private const val INSTALLATION_TOKEN = "vadret_token"
private const val AURORA_FORECAST = """{"cells":[{"geohash":"u6sce","hours":[{"time":"2026-11-08T19:00:00Z","score":55},{"time":"2026-11-08T18:00:00Z","score":42}]}]}"""