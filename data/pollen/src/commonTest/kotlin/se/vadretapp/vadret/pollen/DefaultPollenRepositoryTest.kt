package se.vadretapp.vadret.pollen

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.pollen.model.DailyPollenLevel
import se.vadretapp.vadret.pollen.model.PollenForecast
import se.vadretapp.vadret.pollen.model.PollenLevel
import se.vadretapp.vadret.pollen.model.PollenRegion
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.pollen.remote.DefaultPollenRemoteDataSource
import se.vadretapp.vadret.testing.LogLine
import se.vadretapp.vadret.testing.mockHttpClient
import se.vadretapp.vadret.testing.networkUnavailableException
import se.vadretapp.vadret.testing.withRecordedLogs
import kotlin.test.Test
import kotlin.test.assertEquals

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

private const val EMPTY_PAGE = """{"items":[]}"""

private val repositoryCalls: List<suspend (PollenRepository) -> Either<Failure, Any>> = listOf(
    { pollenRepository: PollenRepository -> pollenRepository.getRegions() },
    { pollenRepository: PollenRepository -> pollenRepository.getPollenTypes() },
    { pollenRepository: PollenRepository ->
        pollenRepository.getForecasts(
            startDate = LocalDate.parse(input = "2026-07-10"),
            endDate = LocalDate.parse(input = "2026-07-16")
        )
    }
)

class DefaultPollenRepositoryTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun repository(handler: MockRequestHandler): PollenRepository =
        DefaultPollenRepository(
            pollenRemoteDataSource = DefaultPollenRemoteDataSource(
                httpClient = lazy {
                    mockHttpClient { request: HttpRequestData ->
                        requests.add(request)
                        handler(request)
                    }
                }
            )
        )

    private fun respondingWith(body: String): PollenRepository =
        repository {
            respond(
                content = body,
                headers = jsonHeaders
            )
        }

    private fun Url.parameterValues(): Map<String, String?> = parameters.names().associateWith { name: String -> parameters[name] }

    @Test
    fun `each list is asked for at its endpoint with room for every item`() =
        runTest {
            val pollenRepository: PollenRepository = respondingWith(body = EMPTY_PAGE)

            repositoryCalls.forEach { repositoryCall: suspend (PollenRepository) -> Either<Failure, Any> -> repositoryCall(pollenRepository) }

            assertEquals(
                expected = listOf(
                    "https://api.pollenrapporten.se/v1/regions" to mapOf("limit" to "100"),
                    "https://api.pollenrapporten.se/v1/pollen-types" to mapOf("limit" to "100"),
                    "https://api.pollenrapporten.se/v1/forecasts" to mapOf(
                        "start_date" to "2026-07-10",
                        "end_date" to "2026-07-16",
                        "limit" to "100"
                    )
                ),
                actual = requests.map { request: HttpRequestData ->
                    request.url.toString().substringBefore(delimiter = '?') to request.url.parameterValues()
                }
            )
        }

    @Test
    fun `regions keep their coordinates and regions without readable coordinates are skipped`() =
        runTest {
            withRecordedLogs { recordingLogSink ->
                assertEquals(
                    expected = listOf(
                        PollenRegion(
                            regionId = "stockholm",
                            name = "Stockholm",
                            latitude = 59.36561,
                            longitude = 18.05964
                        )
                    ).asRight(),
                    actual = respondingWith(
                        body = """
                        {"items":[
                            {"id":"stockholm","name":"Stockholm","longitude":"18.059640","latitude":"59.365610","forecasts":"https://api.pollenrapporten.se/v1/forecasts?region_id=stockholm&current=true"},
                            {"id":"kiruna","name":"Kiruna","longitude":"20.225300","latitude":"north"},
                            {"id":"visby","name":"Visby","latitude":"57.634800"}
                        ]}
                        """
                    ).getRegions()
                )
                assertEquals(
                    expected = List(size = 2) { "se.vadretapp.vadret.pollen" to LogLevel.WARN },
                    actual = recordingLogSink.lines.map { line: LogLine -> line.tag to line.level }
                )
            }
        }

    @Test
    fun `pollen types keep their names`() =
        runTest {
            assertEquals(
                expected = listOf(
                    PollenType(
                        pollenId = "hazel",
                        name = "Hassel"
                    ),
                    PollenType(
                        pollenId = "birch",
                        name = "Björk"
                    )
                ).asRight(),
                actual = respondingWith(
                    body = """{"items":[{"id":"hazel","name":"Hassel","hasForecasts":true,"thresholdLow":10},{"id":"birch","name":"Björk"}]}"""
                ).getPollenTypes()
            )
        }

    @Test
    fun `a forecast keeps its days and every pollen level`() =
        runTest {
            val levelSeries: String = PollenLevel.entries.joinToString(separator = ",") { pollenLevel: PollenLevel ->
                """{"pollenId":"birch","level":${pollenLevel.ordinal},"time":"2026-05-0${pollenLevel.ordinal + 1}T00:00:00"}"""
            }

            assertEquals(
                expected = listOf(
                    PollenForecast(
                        forecastId = "spring",
                        regionId = "stockholm",
                        startDate = LocalDate.parse(input = "2026-05-01"),
                        endDate = LocalDate.parse(input = "2026-05-08"),
                        text = "Björken blommar.\nGräs väntas.",
                        isEndOfSeason = false,
                        dailyLevels = PollenLevel.entries.map { pollenLevel: PollenLevel ->
                            DailyPollenLevel(
                                pollenId = "birch",
                                date = LocalDate.parse(input = "2026-05-0${pollenLevel.ordinal + 1}"),
                                pollenLevel = pollenLevel
                            )
                        }
                    )
                ).asRight(),
                actual = respondingWith(
                    body = """
                    {"items":[{"id":"spring","regionId":"stockholm","startDate":"2026-05-01","endDate":"2026-05-08","text":"Björken blommar.\r\nGräs väntas.\r","isEndOfSeason":false,"images":[],"levelSeries":[$levelSeries]}]}
                    """
                ).getForecasts(
                    startDate = LocalDate.parse(input = "2026-05-01"),
                    endDate = LocalDate.parse(input = "2026-09-30")
                )
            )
        }

    @Test
    fun `forecast parts that cannot be read are skipped`() =
        runTest {
            withRecordedLogs { recordingLogSink ->
                assertEquals(
                    expected = listOf(
                        PollenForecast(
                            forecastId = "autumn",
                            regionId = "stockholm",
                            startDate = LocalDate.parse(input = "2026-09-18"),
                            endDate = LocalDate.parse(input = "2026-09-18"),
                            text = "",
                            isEndOfSeason = false,
                            dailyLevels = emptyList()
                        ),
                        PollenForecast(
                            forecastId = "summer",
                            regionId = "stockholm",
                            startDate = LocalDate.parse(input = "2026-07-17"),
                            endDate = LocalDate.parse(input = "2026-07-20"),
                            text = "Gräs.",
                            isEndOfSeason = true,
                            dailyLevels = listOf(
                                DailyPollenLevel(
                                    pollenId = "grass",
                                    date = LocalDate.parse(input = "2026-07-17"),
                                    pollenLevel = PollenLevel.MODERATE
                                )
                            )
                        )
                    ).asRight(),
                    actual = respondingWith(
                        body = """
                        {"items":[
                            {"id":"soon","regionId":"stockholm","startDate":"soon","endDate":"2026-09-18"},
                            {"id":"autumn","regionId":"stockholm","startDate":"2026-09-18","endDate":"2026-09-18"},
                            {"id":"summer","regionId":"stockholm","startDate":"2026-07-17","endDate":"2026-07-20","text":"Gräs.","isEndOfSeason":true,"levelSeries":[
                                {"pollenId":"grass","level":3,"time":"2026-07-17T00:00:00"},
                                {"pollenId":"grass","level":8,"time":"2026-07-18T00:00:00"},
                                {"pollenId":"grass","level":2,"time":"tomorrow"}
                            ]}
                        ]}
                        """
                    ).getForecasts(
                        startDate = LocalDate.parse(input = "2026-05-01"),
                        endDate = LocalDate.parse(input = "2026-09-30")
                    )
                )
                assertEquals(
                    expected = List(size = 3) { "se.vadretapp.vadret.pollen" to LogLevel.WARN },
                    actual = recordingLogSink.lines.map { line: LogLine -> line.tag to line.level }
                )
            }
        }

    @Test
    fun `http and network and response failures are told apart`() =
        runTest {
            val failingRepositories: Map<ErrorCode, PollenRepository> = mapOf(
                ErrorCode.HTTP_INTERNAL_SERVER_ERROR to repository {
                    respond(
                        content = "",
                        status = HttpStatusCode.InternalServerError
                    )
                },
                ErrorCode.NETWORK_UNAVAILABLE to repository { throw networkUnavailableException(host = "api.pollenrapporten.se") },
                ErrorCode.INVALID_RESPONSE to respondingWith(body = "not json")
            )

            withRecordedLogs {
                assertEquals(
                    expected = failingRepositories.mapValues { (errorCode: ErrorCode, _) ->
                        repositoryCalls.map { Failure(errorCode = errorCode).asLeft() }
                    },
                    actual = failingRepositories.mapValues { (_, pollenRepository: PollenRepository) ->
                        repositoryCalls.map { repositoryCall: suspend (PollenRepository) -> Either<Failure, Any> -> repositoryCall(pollenRepository) }
                    }
                )
            }
        }
}