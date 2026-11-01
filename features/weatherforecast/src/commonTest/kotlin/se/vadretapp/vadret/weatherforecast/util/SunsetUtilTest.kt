package se.vadretapp.vadret.weatherforecast.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class SunsetUtilTest {

    private val stockholmLatitude = 59.33
    private val stockholmLongitude = 18.07
    private val kirunaLatitude = 67.85
    private val kirunaLongitude = 20.23
    private val tolerance: Duration = 2.minutes

    @Test
    fun `stockholm sunrise and sunset on 24 october 2026`() {
        val sunriseSunset: SunriseSunset? = SunsetUtil.getSunriseSunset(
            day = Instant.parse(input = "2026-10-24T10:00:00Z"),
            latitude = stockholmLatitude,
            longitude = stockholmLongitude
        )

        assertNotNull(actual = sunriseSunset)
        assertWithin(expected = Instant.parse(input = "2026-10-24T05:47:00Z"), actual = sunriseSunset.sunrise)
        assertWithin(expected = Instant.parse(input = "2026-10-24T15:18:00Z"), actual = sunriseSunset.sunset)
    }

    @Test
    fun `no sunrise during polar night in kiruna`() {
        val sunriseSunset: SunriseSunset? = SunsetUtil.getSunriseSunset(
            day = Instant.parse(input = "2026-12-21T12:00:00Z"),
            latitude = kirunaLatitude,
            longitude = kirunaLongitude
        )

        assertNull(actual = sunriseSunset)
    }

    @Test
    fun `no sunset during midnight sun in kiruna`() {
        val sunriseSunset: SunriseSunset? = SunsetUtil.getSunriseSunset(
            day = Instant.parse(input = "2026-06-21T12:00:00Z"),
            latitude = kirunaLatitude,
            longitude = kirunaLongitude
        )

        assertNull(actual = sunriseSunset)
    }

    @Test
    fun `the sun is up between sunrise and sunset in stockholm`() {
        assertFalse(actual = isSunUpInStockholm(at = "2026-10-24T05:40:00Z"))
        assertTrue(actual = isSunUpInStockholm(at = "2026-10-24T05:55:00Z"))
        assertTrue(actual = isSunUpInStockholm(at = "2026-10-24T15:10:00Z"))
        assertFalse(actual = isSunUpInStockholm(at = "2026-10-24T15:25:00Z"))
    }

    @Test
    fun `the sun is down at midnight in stockholm`() {
        assertFalse(actual = isSunUpInStockholm(at = "2026-10-24T22:00:00Z"))
        assertFalse(actual = isSunUpInStockholm(at = "2026-10-24T23:30:00Z"))
    }

    @Test
    fun `the sun stays down at noon during polar night in kiruna`() {
        assertFalse(actual = isSunUpInKiruna(at = "2026-12-21T11:00:00Z"))
    }

    @Test
    fun `the sun stays up at midnight during midnight sun in kiruna`() {
        assertTrue(actual = isSunUpInKiruna(at = "2026-06-21T22:00:00Z"))
    }

    private fun isSunUpInStockholm(at: String): Boolean =
        SunsetUtil.isSunUp(
            at = Instant.parse(input = at),
            latitude = stockholmLatitude,
            longitude = stockholmLongitude
        )

    private fun isSunUpInKiruna(at: String): Boolean =
        SunsetUtil.isSunUp(
            at = Instant.parse(input = at),
            latitude = kirunaLatitude,
            longitude = kirunaLongitude
        )

    private fun assertWithin(
        expected: Instant,
        actual: Instant
    ) {
        val difference: Duration = (actual - expected).absoluteValue
        assertTrue(
            actual = difference <= tolerance,
            message = "expected $expected but was $actual"
        )
    }
}