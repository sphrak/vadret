package se.vadretapp.vadret.warning.extension

import se.vadretapp.vadret.aggregatedfeed.model.FeedRegion
import se.vadretapp.vadret.aggregatedfeed.model.FeedRegionKind
import se.vadretapp.vadret.aggregatedfeed.model.FeedSource
import se.vadretapp.vadret.aggregatedfeed.model.GeoLine
import se.vadretapp.vadret.aggregatedfeed.model.GeoPolygon
import se.vadretapp.vadret.aggregatedfeed.model.GeoPosition
import se.vadretapp.vadret.warning.aggregatedFeed
import se.vadretapp.vadret.warning.model.WarningPlace
import se.vadretapp.vadret.warning.square
import kotlin.test.Test
import kotlin.test.assertEquals

private const val DEGREES_LONGITUDE_PER_KILOMETER_AT_62_5 = 0.01946
private const val DEGREES_LATITUDE_PER_KILOMETER = 0.008993

private val herjedalen = GeoPolygon(
    outline = square(
        south = 62.4,
        west = 12.3,
        north = 62.7,
        east = 12.9
    ),
    holes = listOf(
        square(
            south = 62.5,
            west = 12.5,
            north = 62.6,
            east = 12.6
        )
    )
)

private val vanan = GeoLine(
    positions = listOf(
        GeoPosition(
            latitude = 60.85,
            longitude = 14.0
        ),
        GeoPosition(
            latitude = 60.85,
            longitude = 14.3
        )
    )
)

private val jamtland = FeedRegion(
    kind = FeedRegionKind.COUNTY,
    name = "Jämtlands län"
)

private val bottenviken = FeedRegion(
    kind = FeedRegionKind.SEA,
    name = "Bottenviken"
)

private fun place(
    latitude: Double,
    longitude: Double,
    county: String? = null,
    municipality: String? = null
): WarningPlace =
    WarningPlace(
        latitude = latitude,
        longitude = longitude,
        county = county,
        municipality = municipality
    )

class AggregatedFeedTest {

    private val polygonWarning = aggregatedFeed(
        identifier = "smhi-1-1",
        polygons = listOf(herjedalen),
        regions = listOf(jamtland)
    )

    @Test
    fun `a place inside the polygon is affected`() {
        assertEquals(
            expected = true,
            actual = polygonWarning.affects(
                warningPlace = place(
                    latitude = 62.45,
                    longitude = 12.4
                )
            )
        )
    }

    @Test
    fun `a place on land within two kilometers of the polygon is affected`() {
        assertEquals(
            expected = listOf(true, false),
            actual = listOf(1, 3).map { kilometers: Int ->
                polygonWarning.affects(
                    warningPlace = place(
                        latitude = 62.5,
                        longitude = 12.9 + kilometers * DEGREES_LONGITUDE_PER_KILOMETER_AT_62_5
                    )
                )
            }
        )
    }

    @Test
    fun `a place deep inside a hole is not affected but one near the hole edge is`() {
        assertEquals(
            expected = listOf(false, true),
            actual = listOf(
                place(
                    latitude = 62.55,
                    longitude = 12.55
                ),
                place(
                    latitude = 62.55,
                    longitude = 12.6 - DEGREES_LONGITUDE_PER_KILOMETER_AT_62_5
                )
            ).map { warningPlace: WarningPlace -> polygonWarning.affects(warningPlace = warningPlace) }
        )
    }

    @Test
    fun `a sea warning only affects places inside the sea area`() {
        val seaWarning = aggregatedFeed(
            identifier = "smhi-2-1",
            polygons = listOf(herjedalen.copy(holes = emptyList())),
            regions = listOf(bottenviken)
        )

        assertEquals(
            expected = listOf(true, false),
            actual = listOf(
                place(
                    latitude = 62.45,
                    longitude = 12.4
                ),
                place(
                    latitude = 62.5,
                    longitude = 12.9 + DEGREES_LONGITUDE_PER_KILOMETER_AT_62_5
                )
            ).map { warningPlace: WarningPlace -> seaWarning.affects(warningPlace = warningPlace) }
        )
    }

    @Test
    fun `a place within two kilometers of a river or point is affected`() {
        val riverWarning = aggregatedFeed(
            identifier = "smhi-3-1",
            lines = listOf(vanan)
        )
        val pointWarning = aggregatedFeed(
            identifier = "smhi-4-1",
            lines = listOf(
                GeoLine(
                    positions = listOf(
                        GeoPosition(
                            latitude = 60.85,
                            longitude = 14.15
                        )
                    )
                )
            )
        )

        assertEquals(
            expected = listOf(true, false, true, false),
            actual = listOf(
                riverWarning.affects(
                    warningPlace = place(
                        latitude = 60.85 + DEGREES_LATITUDE_PER_KILOMETER,
                        longitude = 14.15
                    )
                ),
                riverWarning.affects(
                    warningPlace = place(
                        latitude = 60.85 + 3 * DEGREES_LATITUDE_PER_KILOMETER,
                        longitude = 14.15
                    )
                ),
                pointWarning.affects(
                    warningPlace = place(
                        latitude = 60.85 + DEGREES_LATITUDE_PER_KILOMETER,
                        longitude = 14.15
                    )
                ),
                pointWarning.affects(
                    warningPlace = place(
                        latitude = 60.85 + 3 * DEGREES_LATITUDE_PER_KILOMETER,
                        longitude = 14.15
                    )
                )
            )
        )
    }

    @Test
    fun `a drawn area is trusted over the county it names`() {
        assertEquals(
            expected = false,
            actual = polygonWarning.affects(
                warningPlace = place(
                    latitude = 63.18,
                    longitude = 14.64,
                    county = "Jämtland"
                )
            )
        )
    }

    @Test
    fun `without a drawn area the county or municipality is matched by name`() {
        val countyWarning = aggregatedFeed(
            identifier = "krisinformation-1",
            source = FeedSource.KRISINFORMATION,
            regions = listOf(jamtland)
        )
        val municipalityWarning = aggregatedFeed(
            identifier = "krisinformation-2",
            source = FeedSource.KRISINFORMATION,
            regions = listOf(
                FeedRegion(
                    kind = FeedRegionKind.MUNICIPALITY,
                    name = "Härjedalen"
                )
            )
        )
        val strangnasWarning = aggregatedFeed(
            identifier = "krisinformation-3",
            source = FeedSource.KRISINFORMATION,
            regions = listOf(
                FeedRegion(
                    kind = FeedRegionKind.MUNICIPALITY,
                    name = "Strängnäs"
                )
            )
        )

        assertEquals(
            expected = listOf(true, true, false, false, true, true),
            actual = listOf(
                countyWarning.affects(
                    warningPlace = place(
                        latitude = 0.0,
                        longitude = 0.0,
                        county = "Jämtland"
                    )
                ),
                countyWarning.affects(
                    warningPlace = place(
                        latitude = 0.0,
                        longitude = 0.0,
                        county = "JÄMTLANDS LÄN"
                    )
                ),
                countyWarning.affects(
                    warningPlace = place(
                        latitude = 0.0,
                        longitude = 0.0,
                        county = "Stockholm"
                    )
                ),
                countyWarning.affects(
                    warningPlace = place(
                        latitude = 0.0,
                        longitude = 0.0
                    )
                ),
                municipalityWarning.affects(
                    warningPlace = place(
                        latitude = 0.0,
                        longitude = 0.0,
                        municipality = "Härjedalens kommun"
                    )
                ),
                strangnasWarning.affects(
                    warningPlace = place(
                        latitude = 0.0,
                        longitude = 0.0,
                        municipality = "Strängnäs kommun"
                    )
                )
            )
        )
    }

    @Test
    fun `a country wide or unknown region affects no one`() {
        val countryWarning = aggregatedFeed(
            identifier = "krisinformation-1",
            source = FeedSource.KRISINFORMATION,
            regions = listOf(
                FeedRegion(
                    kind = FeedRegionKind.COUNTRY,
                    name = "Sverige"
                ),
                FeedRegion(
                    kind = FeedRegionKind.OTHER,
                    name = "Jämtland"
                )
            )
        )

        assertEquals(
            expected = false,
            actual = countryWarning.affects(
                warningPlace = place(
                    latitude = 62.5,
                    longitude = 12.5,
                    county = "Jämtland",
                    municipality = "Sverige"
                )
            )
        )
    }
}