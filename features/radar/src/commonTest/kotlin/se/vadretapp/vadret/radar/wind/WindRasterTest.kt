package se.vadretapp.vadret.radar.wind

import se.vadretapp.vadret.radar.projection.RADAR_EAST_LONGITUDE
import se.vadretapp.vadret.radar.projection.RADAR_NORTH_LATITUDE
import se.vadretapp.vadret.radar.projection.RADAR_SOUTH_LATITUDE
import se.vadretapp.vadret.radar.projection.RADAR_WEST_LONGITUDE
import se.vadretapp.vadret.wind.model.WindFrame
import se.vadretapp.vadret.wind.model.WindGrid
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

private const val RASTER_WIDTH = 40
private const val TOLERANCE = 0.01f
private const val TRANSPARENT = 0

private val coveringGrid = WindGrid(
    columnCount = 2,
    rowCount = 2,
    longitudes = listOf(RADAR_WEST_LONGITUDE - 1, RADAR_EAST_LONGITUDE + 1, RADAR_WEST_LONGITUDE - 1, RADAR_EAST_LONGITUDE + 1),
    latitudes = listOf(RADAR_SOUTH_LATITUDE - 1, RADAR_SOUTH_LATITUDE - 1, RADAR_NORTH_LATITUDE + 1, RADAR_NORTH_LATITUDE + 1)
)

private fun frame(
    windSpeeds: List<Float>,
    windFromDirections: List<Float> = List(size = windSpeeds.size) { 0f }
): WindFrame =
    WindFrame(
        validAt = Instant.parse(input = "2026-10-24T22:00:00Z"),
        windSpeeds = windSpeeds,
        windFromDirections = windFromDirections
    )

class WindRasterTest {

    private val windRaster: WindRaster = coveringGrid.toWindRaster(width = RASTER_WIDTH)

    @Test
    fun `the raster keeps the mercator aspect of the radar area`() {
        assertEquals(expected = RASTER_WIDTH, actual = windRaster.width)
        assertEquals(expected = 58, actual = windRaster.height)
    }

    @Test
    fun `an even field is even everywhere`() {
        val windFieldValues: WindFieldValues = windRaster.render(
            windFrame = frame(windSpeeds = List(size = 4) { 7f }),
            windPalette = WindPalette.DAY
        )

        assertTrue(actual = windFieldValues.speeds.all { speed: Float -> abs(x = speed - 7f) < TOLERANCE })
    }

    @Test
    fun `speeds blend between the west and east edges`() {
        val windFieldValues: WindFieldValues = windRaster.render(
            windFrame = frame(windSpeeds = listOf(0f, 10f, 0f, 10f)),
            windPalette = WindPalette.DAY
        )
        val row: Int = windRaster.height / 2

        val west: Float = windFieldValues.speeds[row * RASTER_WIDTH]
        val east: Float = windFieldValues.speeds[row * RASTER_WIDTH + RASTER_WIDTH - 1]

        assertTrue(actual = west < east)
        assertTrue(actual = west > 0f && east < 10f)
    }

    @Test
    fun `wind from the north blows south`() {
        val windFieldValues: WindFieldValues = windRaster.render(
            windFrame = frame(windSpeeds = List(size = 4) { 5f }),
            windPalette = WindPalette.DAY
        )
        val pixel: Int = windRaster.height / 2 * RASTER_WIDTH + RASTER_WIDTH / 2

        assertEquals(expected = 0f, actual = windFieldValues.eastward[pixel], absoluteTolerance = TOLERANCE)
        assertEquals(expected = -5f, actual = windFieldValues.northward[pixel], absoluteTolerance = TOLERANCE)
    }

    @Test
    fun `wind from the west blows east`() {
        val windFieldValues: WindFieldValues = windRaster.render(
            windFrame = frame(
                windSpeeds = List(size = 4) { 5f },
                windFromDirections = List(size = 4) { 270f }
            ),
            windPalette = WindPalette.DAY
        )
        val pixel: Int = windRaster.height / 2 * RASTER_WIDTH + RASTER_WIDTH / 2

        assertEquals(expected = 5f, actual = windFieldValues.eastward[pixel], absoluteTolerance = TOLERANCE)
        assertEquals(expected = 0f, actual = windFieldValues.northward[pixel], absoluteTolerance = TOLERANCE)
    }

    @Test
    fun `a missing value leaves its triangles empty and see-through`() {
        val middleLongitude: Double = (RADAR_WEST_LONGITUDE + RADAR_EAST_LONGITUDE) / 2
        val threeColumnGrid = WindGrid(
            columnCount = 3,
            rowCount = 2,
            longitudes = listOf(RADAR_WEST_LONGITUDE - 1, middleLongitude, RADAR_EAST_LONGITUDE + 1, RADAR_WEST_LONGITUDE - 1, middleLongitude, RADAR_EAST_LONGITUDE + 1),
            latitudes = listOf(RADAR_SOUTH_LATITUDE - 1, RADAR_SOUTH_LATITUDE - 1, RADAR_SOUTH_LATITUDE - 1, RADAR_NORTH_LATITUDE + 1, RADAR_NORTH_LATITUDE + 1, RADAR_NORTH_LATITUDE + 1)
        )

        val windFieldValues: WindFieldValues = threeColumnGrid
            .toWindRaster(width = RASTER_WIDTH)
            .render(
                windFrame = frame(windSpeeds = listOf(Float.NaN, 5f, 5f, 5f, 5f, 5f)),
                windPalette = WindPalette.DAY
            )

        assertTrue(actual = windFieldValues.speeds.any { speed: Float -> speed.isNaN() })
        assertTrue(actual = windFieldValues.speeds.any { speed: Float -> !speed.isNaN() })
        windFieldValues.speeds.forEachIndexed { pixel: Int, speed: Float ->
            assertEquals(expected = speed.isNaN(), actual = windFieldValues.colors[pixel] == TRANSPARENT)
        }
    }

    @Test
    fun `pixels outside the grid stay empty`() {
        val smallGrid = WindGrid(
            columnCount = 2,
            rowCount = 2,
            longitudes = listOf(15.0, 16.0, 15.0, 16.0),
            latitudes = listOf(60.0, 60.0, 61.0, 61.0)
        )

        val windFieldValues: WindFieldValues = smallGrid
            .toWindRaster(width = RASTER_WIDTH)
            .render(
                windFrame = frame(windSpeeds = List(size = 4) { 3f }),
                windPalette = WindPalette.DAY
            )

        assertTrue(actual = windFieldValues.speeds.first().isNaN())
        assertTrue(actual = windFieldValues.speeds.any { speed: Float -> speed == 3f })
    }

    @Test
    fun `wind at a location points where it blows`() {
        val windFieldValues: WindFieldValues = windRaster.render(
            windFrame = frame(
                windSpeeds = List(size = 4) { 4f },
                windFromDirections = List(size = 4) { 270f }
            ),
            windPalette = WindPalette.DAY
        )

        val windAtLocation: WindAtLocation? = windFieldValues.windAt(latitude = 59.33, longitude = 18.07)

        assertNotNull(actual = windAtLocation)
        assertEquals(expected = 4f, actual = windAtLocation.windSpeed, absoluteTolerance = TOLERANCE)
        assertEquals(expected = 90f, actual = windAtLocation.towardDegrees, absoluteTolerance = TOLERANCE)
    }

    @Test
    fun `wind outside the radar area is unknown`() {
        val windFieldValues: WindFieldValues = windRaster.render(
            windFrame = frame(windSpeeds = List(size = 4) { 4f }),
            windPalette = WindPalette.DAY
        )

        assertNull(actual = windFieldValues.windAt(latitude = 45.0, longitude = 18.0))
    }

    @Test
    fun `raster coordinates round trip to geographic coordinates`() {
        val x: Float = rasterX(longitude = 18.07, width = RASTER_WIDTH)
        val y: Float = rasterY(latitude = 59.33, height = windRaster.height)

        assertEquals(expected = 18.07, actual = rasterLongitude(x = x, width = RASTER_WIDTH), absoluteTolerance = 1e-4)
        assertEquals(expected = 59.33, actual = rasterLatitude(y = y, height = windRaster.height), absoluteTolerance = 1e-4)
    }
}