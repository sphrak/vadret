package se.vadretapp.vadret.pollen

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.extension.successOrNull
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.pollen.local.DefaultPollenLocalDataSource
import se.vadretapp.vadret.pollen.model.DailyPollenLevel
import se.vadretapp.vadret.pollen.model.PollenForecast
import se.vadretapp.vadret.pollen.model.PollenLevel
import se.vadretapp.vadret.pollen.model.PollenRegion
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.pollen.remote.DefaultPollenRemoteDataSource
import se.vadretapp.vadret.testing.FakeDiskCache
import se.vadretapp.vadret.testing.LogLine
import se.vadretapp.vadret.testing.mockHttpClient
import se.vadretapp.vadret.testing.networkUnavailableException
import se.vadretapp.vadret.testing.withRecordedLogs
import kotlin.test.Test
import kotlin.test.assertEquals

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

private const val EMPTY_PAGE = """{"items":[]}"""

private const val POLLEN_TYPES_CACHE_KEY = "pollen_types.json"

private const val BIRCH_PAGE = """{"items":[{"id":"birch","name":"Björk","hasForecasts":true}]}"""

private val repositoryCalls: List<suspend (PollenRepository) -> Either<Failure, Any>> = listOf(
    { pollenRepository: PollenRepository -> pollenRepository.getRegions() },
    { pollenRepository: PollenRepository -> pollenRepository.getPollenTypes() },
    { pollenRepository: PollenRepository ->
        pollenRepository.getForecasts(
            startDate = LocalDate.parse(input = "2026-07-10"),
            endDate = LocalDate.parse(input = "2026-07-16")
        )
    },
    { pollenRepository: PollenRepository -> pollenRepository.getCurrentForecasts() }
)

private const val SAVED_HAZEL = """[{"pollen_id":"hazel","name":"Hassel","has_forecasts":true}]"""

private val birch = PollenType(
    pollenId = "birch",
    name = "Björk",
    hasForecasts = true
)

private val hazel = PollenType(
    pollenId = "hazel",
    name = "Hassel",
    hasForecasts = true
)

class DefaultPollenRepositoryTest {

    private val testScope = TestScope(StandardTestDispatcher())
    private val requests = mutableListOf<HttpRequestData>()
    private val diskCache = FakeDiskCache()

    private fun repository(handler: MockRequestHandler): PollenRepository =
        DefaultPollenRepository(
            pollenRemoteDataSource = DefaultPollenRemoteDataSource(
                httpClient = lazy {
                    mockHttpClient { request: HttpRequestData ->
                        requests.add(request)
                        handler(request)
                    }
                }
            ),
            pollenLocalDataSource = DefaultPollenLocalDataSource(
                diskCache = diskCache,
                coroutineDispatcher = StandardTestDispatcher(scheduler = testScope.testScheduler)
            )
        )

    private fun unreachableRepository(): PollenRepository = repository { throw networkUnavailableException(host = "api.pollenrapporten.se") }

    private fun saveHazel() {
        diskCache.write(
            key = POLLEN_TYPES_CACHE_KEY,
            value = SAVED_HAZEL
        )
    }

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
        testScope.runTest {
            val pollenRepository: PollenRepository = respondingWith(body = EMPTY_PAGE)

            repositoryCalls.forEach { repositoryCall: suspend (PollenRepository) -> Either<Failure, Any> -> repositoryCall(pollenRepository) }

            assertEquals(
                expected = listOf(
                    "https://api.pollenrapporten.se/v1/regions" to mapOf("limit" to "100"),
                    "https://api.pollenrapporten.se/v1/pollen-types" to mapOf("limit" to "100"),
                    "https://api.pollenrapporten.se/v1/forecasts" to mapOf(
                        "start_date" to "2026-07-10",
                        "end_date" to "2026-07-16",
                        "limit" to "100",
                        "offset" to "0"
                    ),
                    "https://api.pollenrapporten.se/v1/forecasts" to mapOf(
                        "current" to "true",
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
        testScope.runTest {
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
    fun `pollen types keep their names and whether they have forecasts`() =
        testScope.runTest {
            assertEquals(
                expected = listOf(
                    PollenType(
                        pollenId = "hazel",
                        name = "Hassel",
                        hasForecasts = true
                    ),
                    PollenType(
                        pollenId = "birch",
                        name = "Björk",
                        hasForecasts = true
                    ),
                    PollenType(
                        pollenId = "alternaria",
                        name = "Alternaria",
                        hasForecasts = false
                    )
                ).asRight(),
                actual = respondingWith(
                    body = """{"items":[{"id":"hazel","name":"Hassel","hasForecasts":true,"thresholdLow":10},{"id":"birch","name":"Björk"},{"id":"alternaria","name":"Alternaria","hasForecasts":false}]}"""
                ).getPollenTypes()
            )
        }

    @Test
    fun `a forecast keeps its days and every pollen level`() =
        testScope.runTest {
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

    private fun forecastPage(
        offset: Int,
        size: Int,
        totalRecords: Int
    ): String {
        val items: String = List(size = size) { index: Int ->
            """{"id":"forecast-${offset + index}","regionId":"stockholm","startDate":"2026-05-01","endDate":"2026-05-07"}"""
        }.joinToString(separator = ",")
        return """{"_meta":{"totalRecords":$totalRecords,"offset":$offset,"limit":100,"count":$size},"items":[$items]}"""
    }

    private fun pagedRepository(totalRecords: Int): PollenRepository =
        repository { request: HttpRequestData ->
            val offset: Int = request.url.parameters["offset"].orEmpty().toInt()
            respond(
                content = forecastPage(
                    offset = offset,
                    size = minOf(100, totalRecords - offset),
                    totalRecords = totalRecords
                ),
                headers = jsonHeaders
            )
        }

    @Test
    fun `forecasts are asked for page by page until every record is read`() =
        testScope.runTest {
            val forecastIds: List<String> = pagedRepository(totalRecords = 159)
                .getForecasts(
                    startDate = LocalDate.parse(input = "2026-05-01"),
                    endDate = LocalDate.parse(input = "2026-05-14")
                ).successOrNull
                .orEmpty()
                .map { pollenForecast: PollenForecast -> pollenForecast.forecastId }

            assertEquals(
                expected = List(size = 159) { index: Int -> "forecast-$index" } to listOf("0", "100"),
                actual = forecastIds to requests.map { request: HttpRequestData -> request.url.parameters["offset"] }
            )
        }

    @Test
    fun `forecasts stop after ten pages however many records are claimed`() =
        testScope.runTest {
            val forecastCount: Int = pagedRepository(totalRecords = 1_000_000)
                .getForecasts(
                    startDate = LocalDate.parse(input = "2026-05-01"),
                    endDate = LocalDate.parse(input = "2026-05-14")
                ).successOrNull
                .orEmpty()
                .size

            assertEquals(
                expected = 1_000 to 10,
                actual = forecastCount to requests.size
            )
        }

    @Test
    fun `current forecasts keep the end of season flag`() =
        testScope.runTest {
            assertEquals(
                expected = listOf(
                    PollenForecast(
                        forecastId = "autumn",
                        regionId = "stockholm",
                        startDate = LocalDate.parse(input = "2026-09-18"),
                        endDate = LocalDate.parse(input = "2026-09-18"),
                        text = "Säsongen är slut.",
                        isEndOfSeason = true,
                        dailyLevels = emptyList()
                    )
                ).asRight(),
                actual = respondingWith(
                    body = """{"items":[{"id":"autumn","regionId":"stockholm","startDate":"2026-09-18","endDate":"2026-09-18","text":"Säsongen är slut.","isEndOfSeason":true,"levelSeries":[]}]}"""
                ).getCurrentForecasts()
            )
        }

    @Test
    fun `forecast parts that cannot be read are skipped`() =
        testScope.runTest {
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
        testScope.runTest {
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

    @Test
    fun `saved pollen types are got without asking`() =
        testScope.runTest {
            saveHazel()

            assertEquals(
                expected = listOf(hazel).asRight() to 0,
                actual = respondingWith(body = BIRCH_PAGE).getPollenTypes() to requests.size
            )
        }

    @Test
    fun `pollen types are fetched and saved when none are saved`() =
        testScope.runTest {
            assertEquals(
                expected = listOf(birch).asRight() to """[{"pollen_id":"birch","name":"Björk","has_forecasts":true}]""",
                actual = respondingWith(body = BIRCH_PAGE).getPollenTypes() to diskCache.entries[POLLEN_TYPES_CACHE_KEY]
            )
        }

    @Test
    fun `observed pollen types are the saved ones and nothing saved is none`() =
        testScope.runTest {
            val pollenRepository: PollenRepository = unreachableRepository()
            val nothingSaved: Either<Failure, List<PollenType>> = pollenRepository.observePollenTypes().first()
            saveHazel()

            assertEquals(
                expected = emptyList<PollenType>().asRight<List<PollenType>>() to listOf(hazel).asRight(),
                actual = nothingSaved to pollenRepository.observePollenTypes().first()
            )
        }

    @Test
    fun `a refresh saves what is fetched and the observed pollen types show it`() =
        testScope.runTest {
            saveHazel()
            val pollenRepository: PollenRepository = respondingWith(body = BIRCH_PAGE)
            val seen = mutableListOf<Either<Failure, List<PollenType>>>()
            val observing: Job = launch(context = UnconfinedTestDispatcher(scheduler = testScheduler)) {
                pollenRepository.observePollenTypes().collect { pollenTypes: Either<Failure, List<PollenType>> -> seen += pollenTypes }
            }
            advanceUntilIdle()

            val refreshed: Either<Failure, List<PollenType>> = pollenRepository.refreshPollenTypes()
            advanceUntilIdle()
            observing.cancel()

            assertEquals<Pair<Either<Failure, List<PollenType>>, List<Either<Failure, List<PollenType>>>>>(
                expected = listOf(birch).asRight() to listOf(listOf(hazel).asRight(), listOf(birch).asRight()),
                actual = refreshed to seen
            )
        }

    @Test
    fun `a refresh that fails keeps the saved pollen types`() =
        testScope.runTest {
            saveHazel()

            withRecordedLogs {
                assertEquals(
                    expected = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft() to SAVED_HAZEL,
                    actual = unreachableRepository().refreshPollenTypes() to diskCache.entries[POLLEN_TYPES_CACHE_KEY]
                )
            }
        }

    @Test
    fun `unreadable saved pollen types are dropped and observed as none`() =
        testScope.runTest {
            diskCache.write(
                key = POLLEN_TYPES_CACHE_KEY,
                value = "not json"
            )

            withRecordedLogs {
                assertEquals(
                    expected = emptyList<PollenType>().asRight<List<PollenType>>() to emptyMap(),
                    actual = unreachableRepository().observePollenTypes().first() to diskCache.entries
                )
            }
        }

    @Test
    fun `pollen types that are neither saved nor fetched are a failure`() =
        testScope.runTest {
            withRecordedLogs {
                assertEquals(
                    expected = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft() to emptyMap(),
                    actual = unreachableRepository().getPollenTypes() to diskCache.entries
                )
            }
        }
}