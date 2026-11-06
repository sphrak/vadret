package se.vadretapp.vadret.radar

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.map
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import okio.Path
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.defaultJson
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.radar.local.DefaultRadarImageLocalDataSource
import se.vadretapp.vadret.radar.local.DefaultRadarLocalDataSource
import se.vadretapp.vadret.radar.local.RadarCacheEntity
import se.vadretapp.vadret.radar.local.RadarImageEntity
import se.vadretapp.vadret.radar.model.Radar
import se.vadretapp.vadret.radar.model.RadarImage
import se.vadretapp.vadret.radar.model.SavedRadar
import se.vadretapp.vadret.radar.remote.DefaultRadarRemoteDataSource
import se.vadretapp.vadret.radar.remote.RadarDto
import se.vadretapp.vadret.radar.remote.RadarFileDto
import se.vadretapp.vadret.radar.remote.RadarFormatDto
import se.vadretapp.vadret.testing.FakeDiskCache
import se.vadretapp.vadret.testing.FixedClock
import se.vadretapp.vadret.testing.LogLine
import se.vadretapp.vadret.testing.deleteRecursively
import se.vadretapp.vadret.testing.mockHttpClient
import se.vadretapp.vadret.testing.networkUnavailableException
import se.vadretapp.vadret.testing.temporaryDirectory
import se.vadretapp.vadret.testing.withRecordedLogs
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private const val CACHE_KEY = "radar_geotiff.json"

private val now: Instant = Instant.parse(input = "2026-10-24T14:00:00Z")

private fun file(
    valid: String,
    link: String?
): RadarFileDto =
    RadarFileDto(
        key = "radar_$valid",
        valid = valid,
        formats = if (link != null) {
            listOf(RadarFormatDto(link = link))
        } else {
            emptyList()
        }
    )

class DefaultRadarRepositoryTest {

    private val testScope = TestScope(StandardTestDispatcher())
    private val imageDirectory: Path = temporaryDirectory(prefix = "radar-images")
    private val diskCache = FakeDiskCache()
    private val clock = FixedClock(now = now)
    private val radarRemoteDataSource = FakeRadarRemoteDataSource(
        radarDto = RadarDto(
            files = listOf(
                file(valid = "2026-10-24 13:55", link = "https://smhi.se/1355.tif"),
                file(valid = "2026-10-24 13:50", link = "https://smhi.se/1350.tif"),
                file(valid = "soon", link = "https://smhi.se/soon.tif"),
                file(valid = "2026-10-24 13:45", link = null)
            )
        )
    )
    private val radarRepository: RadarRepository = DefaultRadarRepository(
        radarRemoteDataSource = radarRemoteDataSource,
        radarLocalDataSource = DefaultRadarLocalDataSource(
            diskCache = diskCache,
            coroutineDispatcher = StandardTestDispatcher(testScope.testScheduler)
        ),
        radarImageLocalDataSource = DefaultRadarImageLocalDataSource(
            directoryPath = imageDirectory.toString(),
            coroutineDispatcher = StandardTestDispatcher(testScope.testScheduler)
        ),
        clock = clock
    )

    @AfterTest
    fun tearDown() {
        imageDirectory.deleteRecursively()
    }

    private val fetchedRadar = Radar(
        images = listOf(
            RadarImage(
                imageUrl = "https://smhi.se/1350.tif",
                validAt = Instant.parse(input = "2026-10-24T13:50:00Z")
            ),
            RadarImage(
                imageUrl = "https://smhi.se/1355.tif",
                validAt = Instant.parse(input = "2026-10-24T13:55:00Z")
            )
        )
    )

    private fun cache(
        fetchedAt: Instant,
        date: String = "2026-10-24",
        images: List<RadarImageEntity> = listOf(
            RadarImageEntity(
                imageUrl = "https://smhi.se/cached.tif",
                validAtEpochMillis = now.toEpochMilliseconds()
            )
        )
    ) {
        diskCache.write(
            key = CACHE_KEY,
            value = defaultJson.encodeToString(
                value = RadarCacheEntity(
                    fetchedAtEpochMillis = fetchedAt.toEpochMilliseconds(),
                    date = date,
                    images = images
                )
            )
        )
    }

    @Test
    fun `without a cache today's radar is fetched with unreadable files skipped`() =
        testScope.runTest {
            withRecordedLogs { recordingLogSink ->
                assertEquals<Either<Failure, Radar>>(
                    expected = fetchedRadar.asRight(),
                    actual = radarRepository.refreshRadar().map { savedRadar: SavedRadar -> savedRadar.radar }
                )
                assertEquals(
                    expected = listOf(
                        LocalDate(
                            year = 2026,
                            month = 10,
                            day = 24
                        )
                    ),
                    actual = radarRemoteDataSource.requestedDates
                )
                assertEquals(
                    expected = listOf(LogLevel.WARN, LogLevel.WARN),
                    actual = recordingLogSink.lines.map { line: LogLine -> line.level }
                )
            }
        }

    @Test
    fun `the radar of the UTC day is asked for as GeoTIFF in GMT`() =
        testScope.runTest {
            val requests = mutableListOf<HttpRequestData>()
            clock.now = Instant.parse(input = "2026-01-05T23:30:00Z")
            val remoteRepository: RadarRepository = DefaultRadarRepository(
                radarRemoteDataSource = DefaultRadarRemoteDataSource(
                    httpClient = lazy {
                        mockHttpClient { request: HttpRequestData ->
                            requests.add(request)
                            respond(
                                content = """{"files":[]}""",
                                headers = headersOf(HttpHeaders.ContentType, "application/json")
                            )
                        }
                    }
                ),
                radarLocalDataSource = DefaultRadarLocalDataSource(
                    diskCache = FakeDiskCache(),
                    coroutineDispatcher = StandardTestDispatcher(testScheduler)
                ),
                radarImageLocalDataSource = DefaultRadarImageLocalDataSource(
                    directoryPath = imageDirectory.toString(),
                    coroutineDispatcher = StandardTestDispatcher(testScope.testScheduler)
                ),
                clock = clock
            )

            assertEquals<Either<Failure, Radar>>(
                expected = Radar(images = emptyList()).asRight(),
                actual = remoteRepository.refreshRadar().map { savedRadar: SavedRadar -> savedRadar.radar }
            )
            assertEquals(
                expected = "https://opendata-download-radar.smhi.se/api/version/latest/area/sweden/product/comp/2026/1/5?format=tif&timeZone=GMT",
                actual = requests.single().url.toString()
            )
        }

    @Test
    fun `a day without radar is told apart from other failures`() =
        testScope.runTest {
            val statuses: Map<HttpStatusCode, ErrorCode> = mapOf(
                HttpStatusCode.NotFound to ErrorCode.RADAR_NOT_AVAILABLE,
                HttpStatusCode.InternalServerError to ErrorCode.HTTP_INTERNAL_SERVER_ERROR
            )

            withRecordedLogs {
                val actual: Map<HttpStatusCode, Either<Failure, Radar>> = statuses.keys.associateWith { status: HttpStatusCode ->
                    DefaultRadarRepository(
                        radarRemoteDataSource = DefaultRadarRemoteDataSource(
                            httpClient = lazy {
                                mockHttpClient {
                                    respond(
                                        content = "",
                                        status = status
                                    )
                                }
                            }
                        ),
                        radarLocalDataSource = DefaultRadarLocalDataSource(
                            diskCache = FakeDiskCache(),
                            coroutineDispatcher = StandardTestDispatcher(testScheduler)
                        ),
                        radarImageLocalDataSource = DefaultRadarImageLocalDataSource(
                            directoryPath = imageDirectory.toString(),
                            coroutineDispatcher = StandardTestDispatcher(testScope.testScheduler)
                        ),
                        clock = clock
                    ).refreshRadar().map { savedRadar: SavedRadar -> savedRadar.radar }
                }

                assertEquals(
                    expected = statuses.mapValues { (_, errorCode: ErrorCode) -> Failure(errorCode = errorCode).asLeft() },
                    actual = actual
                )
            }
        }

    @Test
    fun `the radar containing an image is the radar of the day in its url`() =
        testScope.runTest {
            assertEquals<Either<Failure, Radar>>(
                expected = fetchedRadar.asRight(),
                actual = radarRepository.getRadarContaining(imageUrl = "https://opendata-download-radar.smhi.se/api/version/latest/area/sweden/product/comp/2026/10/25/radar_2610251300.tif")
            )
            assertEquals(
                expected = listOf(LocalDate.parse(input = "2026-10-25")),
                actual = radarRemoteDataSource.requestedDates
            )
        }

    @Test
    fun `an image url without a radar day is a radar that is not available`() =
        testScope.runTest {
            val imageUrls: List<String> = listOf(
                "https://smhi.se/1355.tif",
                "https://opendata-download-radar.smhi.se/api/version/latest/area/sweden/product/comp/2026/13/40/radar_2613400000.tif",
                "https://opendata-download-radar.smhi.se/api/version/latest/area/sweden/product/comp/2026/10/25/radar_2610251300.png"
            )

            withRecordedLogs { recordingLogSink ->
                assertEquals(
                    expected = imageUrls.map { Failure(errorCode = ErrorCode.RADAR_NOT_AVAILABLE).asLeft() },
                    actual = imageUrls.map { imageUrl: String -> radarRepository.getRadarContaining(imageUrl = imageUrl) }
                )
                assertEquals(
                    expected = imageUrls.map { "se.vadretapp.vadret.radar" to LogLevel.WARN },
                    actual = recordingLogSink.lines.map { line: LogLine -> line.tag to line.level }
                )
            }
            assertEquals(
                expected = emptyList(),
                actual = radarRemoteDataSource.requestedDates
            )
        }

    @Test
    fun `a refreshed radar is observed as the radar of today`() =
        testScope.runTest {
            val nothingSaved: Either<Failure, SavedRadar> = radarRepository.observeRadar().first()

            radarRepository.refreshRadar()

            assertEquals<Pair<Either<Failure, SavedRadar>, Either<Failure, SavedRadar>>>(
                expected = SavedRadar(
                    radar = Radar(images = emptyList()),
                    fetchedAt = null
                ).asRight() to SavedRadar(
                    radar = fetchedRadar,
                    fetchedAt = now
                ).asRight(),
                actual = nothingSaved to radarRepository.observeRadar().first()
            )
        }

    @Test
    fun `a radar saved on another day or in an old format or unreadable is observed as nothing saved`() =
        testScope.runTest {
            val saves: List<() -> Unit> = listOf(
                { cache(fetchedAt = now - 1.minutes, date = "2026-10-23") },
                {
                    diskCache.write(
                        key = "radar.json",
                        value = defaultJson.encodeToString(
                            value = RadarCacheEntity(
                                fetchedAtEpochMillis = now.toEpochMilliseconds(),
                                date = "2026-10-24",
                                images = listOf(
                                    RadarImageEntity(
                                        imageUrl = "https://smhi.se/cached.png",
                                        validAtEpochMillis = now.toEpochMilliseconds()
                                    )
                                )
                            )
                        )
                    )
                },
                {
                    diskCache.write(
                        key = CACHE_KEY,
                        value = "serialized java object"
                    )
                }
            )

            withRecordedLogs {
                assertEquals(
                    expected = List(size = saves.size) { null },
                    actual = saves.map { save: () -> Unit ->
                        diskCache.remove(key = CACHE_KEY)
                        save()
                        (radarRepository.observeRadar().first() as Either.Right).b.fetchedAt
                    }
                )
            }
        }

    @Test
    fun `a refresh that fails keeps the saved radar of the day`() =
        testScope.runTest {
            cache(fetchedAt = now - 3.hours)
            radarRemoteDataSource.radarException = networkUnavailableException(host = "opendata-download-radar.smhi.se")

            withRecordedLogs {
                assertEquals<Pair<Either<Failure, SavedRadar>, Instant?>>(
                    expected = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft() to now - 3.hours,
                    actual = radarRepository.refreshRadar() to (radarRepository.observeRadar().first() as Either.Right).b.fetchedAt
                )
            }
        }

    @Test
    fun `the radar containing an image is the saved radar of its day without asking`() =
        testScope.runTest {
            cache(fetchedAt = now - 3.hours)

            assertEquals<Pair<Either<Failure, Radar>, Int>>(
                expected = Radar(
                    images = listOf(
                        RadarImage(
                            imageUrl = "https://smhi.se/cached.tif",
                            validAt = now
                        )
                    )
                ).asRight() to 0,
                actual = radarRepository.getRadarContaining(imageUrl = "https://opendata-download-radar.smhi.se/api/version/latest/area/sweden/product/comp/2026/10/24/radar_2610241300.tif") to
                    radarRemoteDataSource.requestedDates.size
            )
        }
}