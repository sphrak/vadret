package se.vadretapp.vadret.aggregatedfeed

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import se.vadretapp.vadret.aggregatedfeed.local.DefaultAggregatedFeedLocalDataSource
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.model.FeedArea
import se.vadretapp.vadret.aggregatedfeed.model.FeedDetail
import se.vadretapp.vadret.aggregatedfeed.model.FeedDetailKind
import se.vadretapp.vadret.aggregatedfeed.model.FeedRegion
import se.vadretapp.vadret.aggregatedfeed.model.FeedRegionKind
import se.vadretapp.vadret.aggregatedfeed.model.FeedSource
import se.vadretapp.vadret.aggregatedfeed.model.GeoLine
import se.vadretapp.vadret.aggregatedfeed.model.GeoPolygon
import se.vadretapp.vadret.aggregatedfeed.model.GeoPosition
import se.vadretapp.vadret.aggregatedfeed.model.WarningLevel
import se.vadretapp.vadret.aggregatedfeed.remote.GeoJsonDto
import se.vadretapp.vadret.aggregatedfeed.remote.GeoJsonGeometryDto
import se.vadretapp.vadret.aggregatedfeed.remote.KrisinformationAreaDto
import se.vadretapp.vadret.aggregatedfeed.remote.KrisinformationFeedDto
import se.vadretapp.vadret.aggregatedfeed.remote.SmhiAffectedAreaDto
import se.vadretapp.vadret.aggregatedfeed.remote.SmhiDescriptionDto
import se.vadretapp.vadret.aggregatedfeed.remote.SmhiTextDto
import se.vadretapp.vadret.aggregatedfeed.remote.SmhiWarningAreaDto
import se.vadretapp.vadret.aggregatedfeed.remote.SmhiWarningDto
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.testing.FakeDiskCache
import se.vadretapp.vadret.testing.FixedClock
import se.vadretapp.vadret.testing.LogLine
import se.vadretapp.vadret.testing.networkUnavailableException
import se.vadretapp.vadret.testing.withRecordedLogs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private val now: Instant = Instant.parse(input = "2026-10-24T14:00:00Z")

private fun text(
    sv: String,
    code: String? = null
): SmhiTextDto =
    SmhiTextDto(
        sv = sv,
        code = code
    )

private fun warningArea(
    id: Int,
    published: String,
    districtIds: List<Int> = listOf(14),
    warningLevel: SmhiTextDto = text(
        sv = "Gul",
        code = "YELLOW"
    ),
    eventDescription: String = "Mycket regn",
    area: GeoJsonDto? = null
): SmhiWarningAreaDto =
    SmhiWarningAreaDto(
        id = id,
        published = published,
        areaName = text(sv = "Västra Götaland"),
        warningLevel = warningLevel,
        eventDescription = text(sv = eventDescription),
        affectedAreas = districtIds.map { districtId: Int ->
            SmhiAffectedAreaDto(
                id = districtId,
                sv = "Område $districtId"
            )
        },
        area = area,
        descriptions = listOf(
            SmhiDescriptionDto(
                title = text(
                    sv = "Var?",
                    code = "WHERE"
                ),
                text = text(sv = "Kusten")
            )
        )
    )

private fun news(
    identifier: String,
    published: String,
    headline: String = "Nyhet $identifier",
    preamble: String? = "Ingress",
    bodyText: String? = null,
    senderName: String? = null,
    area: List<KrisinformationAreaDto>? = null
): KrisinformationFeedDto =
    KrisinformationFeedDto(
        identifier = identifier,
        published = published,
        headline = headline,
        preamble = preamble,
        bodyText = bodyText,
        senderName = senderName,
        area = area
    )

private fun geometry(
    type: String,
    coordinates: String
): GeoJsonDto =
    GeoJsonDto(
        type = "Feature",
        geometry = GeoJsonGeometryDto(
            type = type,
            coordinates = Json.parseToJsonElement(string = coordinates)
        )
    )

private val noArea = FeedArea(
    polygons = emptyList(),
    lines = emptyList(),
    regions = emptyList()
)

private fun smhiRegions(vararg districtIds: Int): List<FeedRegion> =
    districtIds.map { districtId: Int ->
        FeedRegion(
            kind = FeedRegionKind.COUNTY,
            name = "Område $districtId"
        )
    }

private val smhiDetails: List<FeedDetail> = listOf(
    FeedDetail(
        kind = FeedDetailKind.WHERE,
        title = "Var?",
        text = "Kusten"
    ),
    FeedDetail(
        kind = FeedDetailKind.OTHER,
        title = "Vad",
        text = "Skyfall"
    )
)

class DefaultFeedRepositoryTest {

    private val testScope = TestScope(StandardTestDispatcher())
    private val clock = FixedClock(now = now)
    private val smhiWarningRemoteDataSource = FakeSmhiWarningRemoteDataSource()
    private val krisinformationRemoteDataSource = FakeKrisinformationRemoteDataSource()
    private val diskCache = FakeDiskCache()
    private val feedRepository: FeedRepository = DefaultFeedRepository(
        smhiWarningRemoteDataSource = smhiWarningRemoteDataSource,
        krisinformationRemoteDataSource = krisinformationRemoteDataSource,
        aggregatedFeedLocalDataSource = DefaultAggregatedFeedLocalDataSource(
            diskCache = diskCache,
            coroutineDispatcher = StandardTestDispatcher(scheduler = testScope.testScheduler)
        ),
        seenWarningLocalDataSource = EmptySeenWarningLocalDataSource(),
        clock = clock
    )

    private suspend fun getFeed(isRefresh: Boolean = false): Either<Failure, List<AggregatedFeed>> = feedRepository.getAggregatedFeed(isRefresh = isRefresh)

    private fun Either<Failure, List<AggregatedFeed>>.identifiers(): List<String>? = (this as? Either.Right)?.b?.map { aggregatedFeed: AggregatedFeed -> aggregatedFeed.identifier }

    @Test
    fun `every smhi warning area becomes its own item wherever it is`() =
        testScope.runTest {
            smhiWarningRemoteDataSource.smhiWarningDtos = listOf(
                SmhiWarningDto(
                    id = 7,
                    event = text(sv = "Regn"),
                    descriptions = listOf(
                        SmhiDescriptionDto(
                            title = text(sv = "Vad"),
                            text = text(sv = "Skyfall")
                        )
                    ),
                    warningAreas = listOf(
                        warningArea(
                            id = 1,
                            published = "2026-10-24T09:00:00Z",
                            districtIds = listOf(14)
                        ).copy(
                            approximateStart = "2026-10-24T12:00:00.000Z",
                            approximateEnd = "2026-10-25T06:00:00.000Z"
                        ),
                        warningArea(
                            id = 2,
                            published = "2026-10-24T10:00:00Z",
                            districtIds = listOf(1, 14),
                            eventDescription = "Regn"
                        ),
                        warningArea(
                            id = 3,
                            published = "2026-10-24T11:00:00Z",
                            districtIds = listOf(41),
                            warningLevel = text(
                                sv = "Meddelande",
                                code = "MESSAGE"
                            )
                        )
                    )
                )
            )

            assertEquals<Either<Failure, List<AggregatedFeed>>>(
                expected = listOf(
                    AggregatedFeed(
                        identifier = "smhi-7-3",
                        headline = "Mycket regn",
                        areaName = "Västra Götaland",
                        warningLevel = WarningLevel.MESSAGE,
                        details = smhiDetails,
                        source = FeedSource.SMHI,
                        isPublicAlert = false,
                        senderName = null,
                        publishedAt = Instant.parse(input = "2026-10-24T11:00:00Z"),
                        startsAt = null,
                        endsAt = null,
                        area = noArea.copy(
                            regions = listOf(
                                FeedRegion(
                                    kind = FeedRegionKind.SEA,
                                    name = "Område 41"
                                )
                            )
                        )
                    ),
                    AggregatedFeed(
                        identifier = "smhi-7-2",
                        headline = "Regn",
                        areaName = "Västra Götaland",
                        warningLevel = WarningLevel.YELLOW,
                        details = smhiDetails,
                        source = FeedSource.SMHI,
                        isPublicAlert = false,
                        senderName = null,
                        publishedAt = Instant.parse(input = "2026-10-24T10:00:00Z"),
                        startsAt = null,
                        endsAt = null,
                        area = noArea.copy(regions = smhiRegions(1, 14))
                    ),
                    AggregatedFeed(
                        identifier = "smhi-7-1",
                        headline = "Mycket regn",
                        areaName = "Västra Götaland",
                        warningLevel = WarningLevel.YELLOW,
                        details = smhiDetails,
                        source = FeedSource.SMHI,
                        isPublicAlert = false,
                        senderName = null,
                        publishedAt = Instant.parse(input = "2026-10-24T09:00:00Z"),
                        startsAt = Instant.parse(input = "2026-10-24T12:00:00Z"),
                        endsAt = Instant.parse(input = "2026-10-25T06:00:00Z"),
                        area = noArea.copy(regions = smhiRegions(14))
                    )
                ).asRight(),
                actual = getFeed()
            )
        }

    @Test
    fun `smhi polygons keep their holes and lines and points become lines`() =
        testScope.runTest {
            smhiWarningRemoteDataSource.smhiWarningDtos = listOf(
                SmhiWarningDto(
                    id = 7,
                    event = text(sv = "Regn"),
                    warningAreas = listOf(
                        warningArea(
                            id = 1,
                            published = "2026-10-24T09:00:00Z",
                            area = GeoJsonDto(
                                type = "FeatureCollection",
                                features = listOf(
                                    geometry(
                                        type = "Polygon",
                                        coordinates = "[[[10,60],[12,60],[12,62],[10,60]],[[10.5,60.5],[11,60.5],[11,61],[10.5,60.5]]]"
                                    ),
                                    geometry(
                                        type = "MultiPolygon",
                                        coordinates = "[[[[20,65],[21,65],[21,66],[20,65]]],[[[1,2],[3,4]]]]"
                                    ),
                                    geometry(
                                        type = "LineString",
                                        coordinates = "[[14.1,60.8,0],[14.2,60.9,0]]"
                                    ),
                                    geometry(
                                        type = "Point",
                                        coordinates = "[15.5,61.5]"
                                    ),
                                    geometry(
                                        type = "MultiLineString",
                                        coordinates = "[[[\"north\",\"east\"]]]"
                                    )
                                )
                            )
                        )
                    )
                )
            )

            assertEquals(
                expected = FeedArea(
                    polygons = listOf(
                        GeoPolygon(
                            outline = listOf(
                                GeoPosition(
                                    latitude = 60.0,
                                    longitude = 10.0
                                ),
                                GeoPosition(
                                    latitude = 60.0,
                                    longitude = 12.0
                                ),
                                GeoPosition(
                                    latitude = 62.0,
                                    longitude = 12.0
                                ),
                                GeoPosition(
                                    latitude = 60.0,
                                    longitude = 10.0
                                )
                            ),
                            holes = listOf(
                                listOf(
                                    GeoPosition(
                                        latitude = 60.5,
                                        longitude = 10.5
                                    ),
                                    GeoPosition(
                                        latitude = 60.5,
                                        longitude = 11.0
                                    ),
                                    GeoPosition(
                                        latitude = 61.0,
                                        longitude = 11.0
                                    ),
                                    GeoPosition(
                                        latitude = 60.5,
                                        longitude = 10.5
                                    )
                                )
                            )
                        ),
                        GeoPolygon(
                            outline = listOf(
                                GeoPosition(
                                    latitude = 65.0,
                                    longitude = 20.0
                                ),
                                GeoPosition(
                                    latitude = 65.0,
                                    longitude = 21.0
                                ),
                                GeoPosition(
                                    latitude = 66.0,
                                    longitude = 21.0
                                ),
                                GeoPosition(
                                    latitude = 65.0,
                                    longitude = 20.0
                                )
                            ),
                            holes = emptyList()
                        )
                    ),
                    lines = listOf(
                        GeoLine(
                            positions = listOf(
                                GeoPosition(
                                    latitude = 60.8,
                                    longitude = 14.1
                                ),
                                GeoPosition(
                                    latitude = 60.9,
                                    longitude = 14.2
                                )
                            )
                        ),
                        GeoLine(
                            positions = listOf(
                                GeoPosition(
                                    latitude = 61.5,
                                    longitude = 15.5
                                )
                            )
                        )
                    ),
                    regions = smhiRegions(14)
                ),
                actual = (getFeed() as Either.Right).b.single().area
            )
        }

    @Test
    fun `an unsupported smhi geometry is logged and left out`() =
        testScope.runTest {
            smhiWarningRemoteDataSource.smhiWarningDtos = listOf(
                SmhiWarningDto(
                    id = 7,
                    event = text(sv = "Regn"),
                    warningAreas = listOf(
                        warningArea(
                            id = 1,
                            published = "2026-10-24T09:00:00Z",
                            area = GeoJsonDto(
                                type = "Feature",
                                geometry = GeoJsonGeometryDto(type = "GeometryCollection")
                            )
                        )
                    )
                )
            )

            withRecordedLogs { recordingLogSink ->
                assertEquals(
                    expected = noArea.copy(regions = smhiRegions(14)),
                    actual = (getFeed() as Either.Right).b.single().area
                )
                assertEquals(
                    expected = listOf("Ignoring [GeometryCollection] geometry on SMHI warning area 1"),
                    actual = recordingLogSink.lines.map { line: LogLine -> line.message }
                )
            }
        }

    @Test
    fun `a cached feed reads back the same items`() =
        testScope.runTest {
            smhiWarningRemoteDataSource.smhiWarningDtos = listOf(
                SmhiWarningDto(
                    id = 7,
                    event = text(sv = "Regn"),
                    warningAreas = listOf(
                        warningArea(
                            id = 1,
                            published = "2026-10-24T09:00:00Z",
                            area = geometry(
                                type = "Polygon",
                                coordinates = "[[[10,60],[12,60],[12,62],[10,60]]]"
                            )
                        ).copy(approximateEnd = "2026-10-25T06:00:00Z")
                    )
                )
            )
            krisinformationRemoteDataSource.news = {
                listOf(
                    news(
                        identifier = "a",
                        published = "2026-10-24T08:00:00Z",
                        senderName = "Länsstyrelsen",
                        area = listOf(
                            KrisinformationAreaDto(
                                type = "County",
                                description = "Jämtlands län"
                            )
                        )
                    )
                )
            }

            val fetchedFeed: Either<Failure, List<AggregatedFeed>> = getFeed()

            assertEquals(expected = fetchedFeed, actual = getFeed())
            assertEquals(expected = 1, actual = smhiWarningRemoteDataSource.requestCount)
        }

    @Test
    fun `news and public alerts are merged newest first and a public alert listed as news shows once as a public alert`() =
        testScope.runTest {
            smhiWarningRemoteDataSource.smhiWarningDtos = listOf(
                SmhiWarningDto(
                    id = 7,
                    event = text(sv = "Regn"),
                    warningAreas = listOf(
                        warningArea(
                            id = 1,
                            published = "2026-10-24T10:00:00Z",
                            districtIds = listOf(14)
                        )
                    )
                )
            )
            krisinformationRemoteDataSource.news = {
                listOf(
                    news(
                        identifier = "a",
                        published = "2026-10-24T08:00:00+02:00"
                    ),
                    news(
                        identifier = "vma",
                        published = "2026-10-24T13:30:00+02:00"
                    )
                )
            }
            krisinformationRemoteDataSource.publicAlerts = {
                listOf(
                    news(
                        identifier = "vma",
                        published = "2026-10-24T13:30:00+02:00"
                    )
                )
            }

            val feed: List<AggregatedFeed> = (getFeed() as Either.Right).b

            assertEquals(
                expected = listOf(
                    "krisinformation-vma" to true,
                    "smhi-7-1" to false,
                    "krisinformation-a" to false
                ),
                actual = feed.map { aggregatedFeed: AggregatedFeed -> aggregatedFeed.identifier to aggregatedFeed.isPublicAlert }
            )
        }

    @Test
    fun `every smhi warning level and description code is read`() =
        testScope.runTest {
            smhiWarningRemoteDataSource.smhiWarningDtos = listOf(
                SmhiWarningDto(
                    id = 8,
                    event = text(sv = "Vind"),
                    warningAreas = listOf("MESSAGE", "YELLOW", "ORANGE", "RED").mapIndexed { index: Int, code: String ->
                        warningArea(
                            id = index,
                            published = "2026-10-24T0$index:00:00Z",
                            districtIds = listOf(14),
                            warningLevel = text(
                                sv = code,
                                code = code
                            )
                        ).copy(
                            descriptions = listOf("HAPPENS", "INCIDENT", "WHERE", "AFFECT", "COMMENTS", "NEW").map { descriptionCode: String ->
                                SmhiDescriptionDto(
                                    title = text(
                                        sv = descriptionCode,
                                        code = descriptionCode
                                    ),
                                    text = text(sv = "Text")
                                )
                            }
                        )
                    }
                )
            )

            val feed: List<AggregatedFeed> = (getFeed() as Either.Right).b

            assertEquals(
                expected = listOf(WarningLevel.RED, WarningLevel.ORANGE, WarningLevel.YELLOW, WarningLevel.MESSAGE),
                actual = feed.map { aggregatedFeed: AggregatedFeed -> aggregatedFeed.warningLevel }
            )
            assertEquals(
                expected = listOf(
                    FeedDetailKind.WHAT_HAPPENS,
                    FeedDetailKind.DESCRIPTION,
                    FeedDetailKind.WHERE,
                    FeedDetailKind.ADVICE,
                    FeedDetailKind.COMMENT,
                    FeedDetailKind.OTHER
                ),
                actual = feed.first().details.map { feedDetail: FeedDetail -> feedDetail.kind }
            )
        }

    @Test
    fun `an unknown smhi warning level is logged and left out`() =
        testScope.runTest {
            smhiWarningRemoteDataSource.smhiWarningDtos = listOf(
                SmhiWarningDto(
                    id = 9,
                    event = text(sv = "Vind"),
                    warningAreas = listOf(
                        warningArea(
                            id = 1,
                            published = "2026-10-24T09:00:00Z",
                            districtIds = listOf(14),
                            warningLevel = text(
                                sv = "Lila",
                                code = "PURPLE"
                            )
                        )
                    )
                )
            )

            withRecordedLogs { recordingLogSink ->
                val feed: List<AggregatedFeed> = (getFeed() as Either.Right).b

                assertEquals(
                    expected = listOf(null),
                    actual = feed.map { aggregatedFeed: AggregatedFeed -> aggregatedFeed.warningLevel }
                )
                assertEquals(expected = LogLevel.WARN, actual = recordingLogSink.lines.single().level)
            }
        }

    @Test
    fun `krisinformation keeps its preamble and body as untitled details and its areas as regions`() =
        testScope.runTest {
            krisinformationRemoteDataSource.news = {
                listOf(
                    news(
                        identifier = "a",
                        published = "2026-10-24T08:00:00Z",
                        preamble = " Ingress ",
                        bodyText = "Brödtext",
                        senderName = " Länsstyrelsen Jämtlands län ",
                        area = listOf(
                            KrisinformationAreaDto(
                                type = "Country",
                                description = "Sverige"
                            ),
                            KrisinformationAreaDto(
                                type = "County",
                                description = "Jämtlands län"
                            ),
                            KrisinformationAreaDto(
                                type = "Municipality",
                                description = "Härjedalens kommun"
                            ),
                            KrisinformationAreaDto(
                                type = "Lake",
                                description = "Storsjön"
                            ),
                            KrisinformationAreaDto(
                                type = "County",
                                description = " "
                            )
                        )
                    ),
                    news(
                        identifier = "b",
                        published = "2026-10-24T07:00:00Z",
                        preamble = " ",
                        bodyText = null,
                        senderName = ""
                    )
                )
            }

            assertEquals<Either<Failure, List<AggregatedFeed>>>(
                expected = listOf(
                    AggregatedFeed(
                        identifier = "krisinformation-a",
                        headline = "Nyhet a",
                        areaName = null,
                        warningLevel = null,
                        details = listOf(
                            FeedDetail(
                                kind = FeedDetailKind.SUMMARY,
                                title = "",
                                text = "Ingress"
                            ),
                            FeedDetail(
                                kind = FeedDetailKind.BODY,
                                title = "",
                                text = "Brödtext"
                            )
                        ),
                        source = FeedSource.KRISINFORMATION,
                        isPublicAlert = false,
                        senderName = "Länsstyrelsen Jämtlands län",
                        publishedAt = Instant.parse(input = "2026-10-24T08:00:00Z"),
                        startsAt = null,
                        endsAt = null,
                        area = noArea.copy(
                            regions = listOf(
                                FeedRegion(
                                    kind = FeedRegionKind.COUNTRY,
                                    name = "Sverige"
                                ),
                                FeedRegion(
                                    kind = FeedRegionKind.COUNTY,
                                    name = "Jämtlands län"
                                ),
                                FeedRegion(
                                    kind = FeedRegionKind.MUNICIPALITY,
                                    name = "Härjedalens kommun"
                                ),
                                FeedRegion(
                                    kind = FeedRegionKind.OTHER,
                                    name = "Storsjön"
                                )
                            )
                        )
                    ),
                    AggregatedFeed(
                        identifier = "krisinformation-b",
                        headline = "Nyhet b",
                        areaName = null,
                        warningLevel = null,
                        details = emptyList(),
                        source = FeedSource.KRISINFORMATION,
                        isPublicAlert = false,
                        senderName = null,
                        publishedAt = Instant.parse(input = "2026-10-24T07:00:00Z"),
                        startsAt = null,
                        endsAt = null,
                        area = noArea
                    )
                ).asRight(),
                actual = getFeed()
            )
        }

    @Test
    fun `a feed cached in the old format is dropped and fetched again`() =
        testScope.runTest {
            diskCache.write(
                key = "aggregatedfeed.json",
                value = """
                {"fetched_at_epoch_millis":${now.toEpochMilliseconds()},"feed_sources":["KRISINFORMATION","SMHI"],"district_ids":[14],
                "items":[{"identifier":"smhi-1-1","headline":"Gul varning: Regn","preamble":"Kusten","body_text":"Text","source":"SMHI","published_at_epoch_millis":0}]}
                """.trimIndent()
            )

            withRecordedLogs { recordingLogSink ->
                assertEquals(expected = emptyList(), actual = getFeed().identifiers())
                assertEquals(expected = 1, actual = smhiWarningRemoteDataSource.requestCount)
                assertEquals(
                    expected = listOf(LogLevel.WARN),
                    actual = recordingLogSink.lines.map { line: LogLine -> line.level }
                )
            }
        }

    @Test
    fun `items with an unreadable publish time are skipped`() =
        testScope.runTest {
            krisinformationRemoteDataSource.news = {
                listOf(
                    news(
                        identifier = "a",
                        published = "yesterday"
                    ),
                    news(
                        identifier = "b",
                        published = "2026-10-24T08:00:00Z"
                    )
                )
            }

            withRecordedLogs { recordingLogSink ->
                assertEquals(expected = listOf("krisinformation-b"), actual = getFeed().identifiers())
                assertEquals(expected = LogLevel.WARN, actual = recordingLogSink.lines.single().level)
            }
        }

    @Test
    fun `a failing public alert feed still shows news`() =
        testScope.runTest {
            krisinformationRemoteDataSource.news = {
                listOf(
                    news(
                        identifier = "a",
                        published = "2026-10-24T08:00:00Z"
                    )
                )
            }
            krisinformationRemoteDataSource.publicAlerts = { throw IOException("reset") }

            withRecordedLogs { recordingLogSink ->
                assertEquals(expected = listOf("krisinformation-a"), actual = getFeed().identifiers())
                assertEquals(
                    expected = listOf("Skipping public alerts: IOException: reset"),
                    actual = recordingLogSink.lines.map { line: LogLine -> line.message }
                )
            }
        }

    @Test
    fun `a failing news feed fails the whole feed`() =
        testScope.runTest {
            krisinformationRemoteDataSource.news = { throw networkUnavailableException(host = "api.krisinformation.se") }

            withRecordedLogs { recordingLogSink ->
                assertEquals(
                    expected = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft(),
                    actual = getFeed()
                )
                assertEquals(
                    expected = listOf(LogLevel.WARN),
                    actual = recordingLogSink.lines.map { line: LogLine -> line.level }
                )
            }
        }

    @Test
    fun `an unexpected failure is unknown and logged as an error`() =
        testScope.runTest {
            krisinformationRemoteDataSource.news = { throw IllegalStateException("unexpected") }

            withRecordedLogs { recordingLogSink ->
                assertEquals(
                    expected = Failure.unknown().asLeft(),
                    actual = getFeed()
                )
                assertEquals(
                    expected = listOf(LogLevel.ERROR),
                    actual = recordingLogSink.lines.map { line: LogLine -> line.level }
                )
            }
        }

    @Test
    fun `the feed is reused within five minutes`() =
        testScope.runTest {
            getFeed()
            clock.now = now + 4.minutes
            getFeed()

            assertEquals(expected = 1, actual = smhiWarningRemoteDataSource.requestCount)
        }

    @Test
    fun `a refresh or an old feed is fetched again`() =
        testScope.runTest {
            getFeed()
            getFeed(isRefresh = true)
            clock.now = now + 5.minutes
            getFeed()

            assertEquals(expected = 3, actual = smhiWarningRemoteDataSource.requestCount)
        }
}