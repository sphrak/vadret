package se.vadretapp.vadret.geohash

import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.geohash.model.geohashOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GeohashTest {
    @Test
    fun aPlaceInSwedenBecomesTheCellTheServerExpects() {
        assertEquals(
            expected = listOf("u6sce", "u3chr", "ukn6h", "u7q7n"),
            actual = listOf(
                geohashOf(latitude = 59.3293, longitude = 18.0686),
                geohashOf(latitude = 55.6050, longitude = 13.0038),
                geohashOf(latitude = 67.8558, longitude = 20.2253),
                geohashOf(latitude = 63.82189, longitude = 20.30458)
            ).map { geohash -> geohash?.value }
        )
    }

    @Test
    fun theEdgesOfTheWorldAreCellsToo() {
        assertEquals(
            expected = listOf("zzzzz", "00000", "s0000", "gcpvj"),
            actual = listOf(
                geohashOf(latitude = 90.0, longitude = 180.0),
                geohashOf(latitude = -90.0, longitude = -180.0),
                geohashOf(latitude = 0.0, longitude = 0.0),
                geohashOf(latitude = 51.5074, longitude = -0.1278)
            ).map { geohash -> geohash?.value }
        )
    }

    @Test
    fun aPositionOffTheGlobeOrNotANumberHasNoCell() {
        assertEquals(
            expected = listOf(null, null, null, null),
            actual = listOf(
                geohashOf(latitude = 90.5, longitude = 18.0),
                geohashOf(latitude = 59.0, longitude = -180.5),
                geohashOf(latitude = Double.NaN, longitude = 18.0),
                geohashOf(latitude = 59.0, longitude = Double.NaN)
            )
        )
    }

    @Test
    fun everyCornerOfSwedenIsInSwedishWaters() {
        assertEquals(
            expected = listOf(true, true, true, true, true),
            actual = listOf(CENTRAL_STOCKHOLM, SMYGEHUK, MALMO, KIRUNA, TRERIKSROSET).map { geohash -> Geohash(value = geohash).isInSwedishWaters() }
        )
    }

    @Test
    fun aCellOutsideTheBoxTheServerAcceptsIsNotInSwedishWaters() {
        assertEquals(
            expected = listOf(false, false, false, false, false, false),
            actual = listOf(LONDON, SOUTH_OF_THE_BALTIC_COAST, BERLIN, HELSINKI, "00000", "zzzzz").map { geohash -> Geohash(value = geohash).isInSwedishWaters() }
        )
    }

    @Test
    fun aCellThatReachesIntoTheBoxCountsLikeOnTheServer() {
        assertEquals(
            expected = listOf("u39np" to true, "u39jz" to false),
            actual = listOf(
                geohashOf(latitude = 54.495, longitude = 13.0),
                geohashOf(latitude = 54.49, longitude = 13.0)
            ).map { geohash -> geohash?.value to geohash?.isInSwedishWaters() }
        )
    }

    @Test
    fun aGeohashThatIsNotFiveCharactersOfTheAlphabetIsRefused() {
        listOf("u6sc", "u6scee", "u6sca", "U6SCE").forEach { value ->
            assertFailsWith<IllegalArgumentException> { Geohash(value = value) }
        }
    }
}

private const val CENTRAL_STOCKHOLM = "u6sce"
private const val SMYGEHUK = "u3c6z"
private const val MALMO = "u3chr"
private const val KIRUNA = "ukn6h"
private const val TRERIKSROSET = "ukq8g"
private const val LONDON = "gcpvj"
private const val SOUTH_OF_THE_BALTIC_COAST = "u39hz"
private const val BERLIN = "u33dc"
private const val HELSINKI = "ud9wr"