package se.vadretapp.vadret.forecastwarning.extension

import kotlinx.datetime.TimeZone
import se.vadretapp.vadret.aggregatedfeed.model.FeedDetail
import se.vadretapp.vadret.aggregatedfeed.model.FeedDetailKind
import se.vadretapp.vadret.aggregatedfeed.model.FeedRegion
import se.vadretapp.vadret.aggregatedfeed.model.FeedRegionKind
import se.vadretapp.vadret.aggregatedfeed.model.FeedSource
import se.vadretapp.vadret.aggregatedfeed.model.GeoLine
import se.vadretapp.vadret.aggregatedfeed.model.GeoPolygon
import se.vadretapp.vadret.aggregatedfeed.model.GeoPosition
import se.vadretapp.vadret.forecastwarning.aggregatedFeed
import se.vadretapp.vadret.forecastwarning.model.MapBounds
import se.vadretapp.vadret.forecastwarning.model.WarningDetail
import se.vadretapp.vadret.forecastwarning.model.WarningItem
import se.vadretapp.vadret.forecastwarning.model.WarningMapArea
import se.vadretapp.vadret.ui.model.ElapsedTime
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.hours_ago
import se.vadretapp.vadret.ui.resources.warning_detail_advice
import se.vadretapp.vadret.ui.resources.warning_detail_comment
import se.vadretapp.vadret.ui.resources.warning_detail_description
import se.vadretapp.vadret.ui.resources.warning_detail_what_happens
import se.vadretapp.vadret.ui.resources.warning_detail_where
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private const val ENGLISH = "en"

private val now: Instant = Instant.parse(input = "2026-10-26T09:00:00Z")
private val stockholm: TimeZone = TimeZone.of(zoneId = "Europe/Stockholm")

private val gotland = GeoPolygon(
    outline = listOf(
        GeoPosition(
            latitude = 57.0,
            longitude = 18.0
        ),
        GeoPosition(
            latitude = 57.0,
            longitude = 19.0
        ),
        GeoPosition(
            latitude = 58.0,
            longitude = 19.0
        ),
        GeoPosition(
            latitude = 57.0,
            longitude = 18.0
        )
    ),
    holes = listOf(
        listOf(
            GeoPosition(
                latitude = 57.2,
                longitude = 18.5
            ),
            GeoPosition(
                latitude = 57.2,
                longitude = 18.6
            ),
            GeoPosition(
                latitude = 57.3,
                longitude = 18.6
            ),
            GeoPosition(
                latitude = 57.2,
                longitude = 18.5
            )
        )
    )
)

class AggregatedFeedTest {

    private fun item(
        identifier: String = "smhi-1-1",
        source: FeedSource = FeedSource.SMHI,
        areaName: String? = "Gotlands län,Fårö",
        details: List<FeedDetail> = emptyList(),
        senderName: String? = null,
        startsAt: Instant? = null,
        endsAt: Instant? = null,
        regions: List<FeedRegion> = emptyList()
    ): WarningItem =
        aggregatedFeed(
            identifier = identifier,
            publishedAt = Instant.parse(input = "2026-10-26T06:00:00Z"),
            source = source,
            areaName = areaName,
            details = details,
            senderName = senderName,
            startsAt = startsAt,
            endsAt = endsAt,
            regions = regions
        ).toWarningItem(
            now = now,
            timeZone = stockholm,
            languageTag = ENGLISH
        )

    @Test
    fun `a feed item becomes a warning with spaced areas its age and the times it starts and ends`() {
        val warningItem: WarningItem = item(
            startsAt = Instant.parse(input = "2026-10-26T12:00:00Z"),
            endsAt = Instant.parse(input = "2026-10-27T05:00:00Z")
        )

        assertEquals(expected = "Gotlands län, Fårö", actual = warningItem.areaName)
        assertEquals(expected = "Gotlands län, Fårö", actual = warningItem.byline)
        assertEquals(expected = "SMHI", actual = warningItem.publisher)
        assertEquals(
            expected = ElapsedTime(
                resource = Res.string.hours_ago,
                count = 3
            ),
            actual = warningItem.elapsedTime
        )
        assertEquals(expected = "13:00", actual = warningItem.startsAtText)
        assertEquals(expected = "Tue 06:00", actual = warningItem.endsAtText)
    }

    @Test
    fun `krisinformation is bylined by its sender and placed by its regions`() {
        val warningItem: WarningItem = item(
            source = FeedSource.KRISINFORMATION,
            areaName = null,
            senderName = "Länsstyrelsen Jämtlands län",
            regions = listOf(
                FeedRegion(
                    kind = FeedRegionKind.COUNTY,
                    name = "Jämtlands län"
                ),
                FeedRegion(
                    kind = FeedRegionKind.MUNICIPALITY,
                    name = "Härjedalen"
                ),
                FeedRegion(
                    kind = FeedRegionKind.COUNTY,
                    name = "Jämtlands län"
                )
            )
        )

        assertEquals(expected = "Jämtlands län, Härjedalen", actual = warningItem.areaName)
        assertEquals(expected = "Länsstyrelsen Jämtlands län", actual = warningItem.byline)
        assertEquals(expected = "Länsstyrelsen Jämtlands län", actual = warningItem.publisher)
    }

    @Test
    fun `without a sender or area the source names the warning`() {
        val warningItem: WarningItem = item(
            source = FeedSource.KRISINFORMATION,
            areaName = null
        )

        assertEquals(expected = null, actual = warningItem.areaName)
        assertEquals(expected = "Krisinformation", actual = warningItem.byline)
        assertEquals(expected = null, actual = warningItem.startsAtText)
        assertEquals(expected = null, actual = warningItem.endsAtText)
    }

    @Test
    fun `known details get a short label and unknown ones keep their title`() {
        val warningItem: WarningItem = item(
            details = listOf(
                FeedDetail(
                    kind = FeedDetailKind.WHAT_HAPPENS,
                    title = "Vad händer",
                    text = "a"
                ),
                FeedDetail(
                    kind = FeedDetailKind.DESCRIPTION,
                    title = "Händelsebeskrivning",
                    text = "b"
                ),
                FeedDetail(
                    kind = FeedDetailKind.WHERE,
                    title = "Var?",
                    text = "c"
                ),
                FeedDetail(
                    kind = FeedDetailKind.ADVICE,
                    title = "Vad ska jag tänka på?",
                    text = "d"
                ),
                FeedDetail(
                    kind = FeedDetailKind.COMMENT,
                    title = "Kommentar",
                    text = "e"
                ),
                FeedDetail(
                    kind = FeedDetailKind.OTHER,
                    title = "Övrigt",
                    text = "f"
                )
            )
        )

        assertEquals<List<WarningDetail>>(
            expected = listOf(
                WarningDetail.Labelled(
                    labelResource = Res.string.warning_detail_what_happens,
                    text = "a"
                ),
                WarningDetail.Labelled(
                    labelResource = Res.string.warning_detail_description,
                    text = "b"
                ),
                WarningDetail.Labelled(
                    labelResource = Res.string.warning_detail_where,
                    text = "c"
                ),
                WarningDetail.Labelled(
                    labelResource = Res.string.warning_detail_advice,
                    text = "d"
                ),
                WarningDetail.Labelled(
                    labelResource = Res.string.warning_detail_comment,
                    text = "e"
                ),
                WarningDetail.Titled(
                    title = "Övrigt",
                    text = "f"
                )
            ),
            actual = warningItem.details
        )
    }

    @Test
    fun `untitled details become paragraphs and summaries get spaced commas`() {
        val warningItem: WarningItem = item(
            details = listOf(
                FeedDetail(
                    kind = FeedDetailKind.SUMMARY,
                    title = "",
                    text = "Stockholm,Uppsala"
                ),
                FeedDetail(
                    kind = FeedDetailKind.BODY,
                    title = "",
                    text = "Text,utan"
                )
            )
        )

        assertEquals<List<WarningDetail>>(
            expected = listOf(
                WarningDetail.Paragraph(text = "Stockholm, Uppsala"),
                WarningDetail.Paragraph(text = "Text,utan")
            ),
            actual = warningItem.details
        )
    }

    @Test
    fun `a where detail that repeats the area is left out`() {
        val warningItem: WarningItem = item(
            details = listOf(
                FeedDetail(
                    kind = FeedDetailKind.WHERE,
                    title = "Var?",
                    text = "Gotlands län,Fårö"
                ),
                FeedDetail(
                    kind = FeedDetailKind.WHERE,
                    title = "Var?",
                    text = "Norra delen"
                )
            )
        )

        assertEquals<List<WarningDetail>>(
            expected = listOf(
                WarningDetail.Labelled(
                    labelResource = Res.string.warning_detail_where,
                    text = "Norra delen"
                )
            ),
            actual = warningItem.details
        )
    }

    @Test
    fun `warnings from both sources are listed together newest first`() {
        val warningItems: List<WarningItem> = listOf(
            aggregatedFeed(
                identifier = "smhi-1",
                publishedAt = Instant.parse(input = "2026-10-26T05:00:00Z")
            ),
            aggregatedFeed(
                identifier = "krisinformation-1",
                publishedAt = Instant.parse(input = "2026-10-26T06:00:00Z"),
                source = FeedSource.KRISINFORMATION
            ),
            aggregatedFeed(
                identifier = "smhi-2",
                publishedAt = Instant.parse(input = "2026-10-26T07:00:00Z")
            ),
            aggregatedFeed(
                identifier = "krisinformation-2",
                publishedAt = Instant.parse(input = "2026-10-26T04:00:00Z"),
                source = FeedSource.KRISINFORMATION
            )
        ).toWarningItems(
            now = now,
            timeZone = stockholm,
            languageTag = ENGLISH
        )

        assertEquals(
            expected = listOf("smhi-2", "krisinformation-1", "smhi-1", "krisinformation-2"),
            actual = warningItems.map { warningItem: WarningItem -> warningItem.identifier }
        )
    }

    @Test
    fun `the map shows the polygons lines and points and frames every position`() {
        val mapArea: WarningMapArea = aggregatedFeed(
            identifier = "smhi-1",
            polygons = listOf(gotland),
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
                            longitude = 20.5
                        )
                    )
                )
            )
        ).toWarningItem(
            now = now,
            timeZone = stockholm,
            languageTag = ENGLISH
        ).mapArea

        assertEquals(
            expected = WarningMapArea(
                geoJson = """{"type":"FeatureCollection","features":[""" +
                    """{"type":"Feature","properties":{},"geometry":{"type":"Polygon","coordinates":[[[18.0,57.0],[19.0,57.0],[19.0,58.0],[18.0,57.0]],[[18.5,57.2],[18.6,57.2],[18.6,57.3],[18.5,57.2]]]}},""" +
                    """{"type":"Feature","properties":{},"geometry":{"type":"LineString","coordinates":[[14.1,60.8],[14.2,60.9]]}},""" +
                    """{"type":"Feature","properties":{},"geometry":{"type":"Point","coordinates":[20.5,61.5]}}]}""",
                bounds = MapBounds(
                    west = 14.1,
                    south = 57.0,
                    east = 20.5,
                    north = 61.5
                )
            ),
            actual = mapArea
        )
    }

    @Test
    fun `without a drawn area the map shows all of Sweden`() {
        assertEquals(
            expected = WarningMapArea(
                geoJson = null,
                bounds = MapBounds(
                    west = 10.9,
                    south = 55.3,
                    east = 24.2,
                    north = 69.1
                )
            ),
            actual = item().mapArea
        )
    }
}