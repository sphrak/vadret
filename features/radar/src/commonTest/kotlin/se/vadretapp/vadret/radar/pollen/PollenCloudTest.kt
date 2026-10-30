package se.vadretapp.vadret.radar.pollen

import se.vadretapp.vadret.pollen.model.PollenLevel
import se.vadretapp.vadret.radar.model.PollenMarker
import se.vadretapp.vadret.radar.wind.radarRasterHeight
import se.vadretapp.vadret.radar.wind.rasterX
import se.vadretapp.vadret.radar.wind.rasterY
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val WIDTH = 120
private const val ALPHA_SHIFT = 24
private const val BYTE_MASK = 0xFF
private const val RGB_MASK = 0x00FFFFFF
private const val TRANSPARENT = 0

private val height: Int = radarRasterHeight(width = WIDTH)

private fun marker(
    latitude: Double,
    longitude: Double,
    pollenLevel: PollenLevel
): PollenMarker =
    PollenMarker(
        latitude = latitude,
        longitude = longitude,
        pollenLevel = pollenLevel
    )

private fun IntArray.pixelAt(
    latitude: Double,
    longitude: Double
): Int {
    val y: Int = rasterY(
        latitude = latitude,
        height = height
    ).toInt()
    val x: Int = rasterX(
        longitude = longitude,
        width = WIDTH
    ).toInt()
    return this[y * WIDTH + x]
}

private fun Int.alpha(): Int = (this ushr ALPHA_SHIFT) and BYTE_MASK

class PollenCloudTest {

    private val stockholm = marker(
        latitude = 59.37,
        longitude = 18.06,
        pollenLevel = PollenLevel.VERY_HIGH
    )

    @Test
    fun `a station is wrapped in a cloud of its own color`() {
        val pixels: IntArray = pollenCloudPixels(
            pollenMarkers = listOf(stockholm),
            width = WIDTH,
            height = height
        )

        val pixel: Int = pixels.pixelAt(
            latitude = stockholm.latitude,
            longitude = stockholm.longitude
        )
        assertEquals(
            expected = PollenColorScale.argb(pollenLevel = PollenLevel.VERY_HIGH) and RGB_MASK,
            actual = pixel and RGB_MASK
        )
        assertTrue(actual = pixel.alpha() > 0)
    }

    @Test
    fun `the cloud fades with distance and is gone far away`() {
        val pixels: IntArray = pollenCloudPixels(
            pollenMarkers = listOf(stockholm),
            width = WIDTH,
            height = height
        )

        val atStation: Int = pixels.pixelAt(
            latitude = 59.37,
            longitude = 18.06
        ).alpha()
        val nearby: Int = pixels.pixelAt(
            latitude = 59.37,
            longitude = 19.6
        ).alpha()
        assertTrue(actual = atStation > nearby)
        assertTrue(actual = nearby > 0)
        assertEquals(expected = TRANSPARENT, actual = pixels.pixelAt(latitude = 67.85, longitude = 20.3))
    }

    @Test
    fun `a station without pollen keeps its air clear`() {
        val pixels: IntArray = pollenCloudPixels(
            pollenMarkers = listOf(
                marker(
                    latitude = 59.37,
                    longitude = 18.06,
                    pollenLevel = PollenLevel.NONE
                )
            ),
            width = WIDTH,
            height = height
        )

        assertTrue(actual = pixels.all { pixel: Int -> pixel == TRANSPARENT })
    }

    @Test
    fun `no stations means no clouds`() {
        assertTrue(
            actual = pollenCloudPixels(
                pollenMarkers = emptyList(),
                width = WIDTH,
                height = height
            ).all { pixel: Int -> pixel == TRANSPARENT }
        )
    }
}