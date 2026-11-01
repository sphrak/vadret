package se.vadretapp.vadret.radar.ui.map

import se.vadretapp.vadret.radar.projection.RADAR_EAST_LONGITUDE
import se.vadretapp.vadret.radar.projection.RADAR_NORTH_LATITUDE
import se.vadretapp.vadret.radar.projection.RADAR_SOUTH_LATITUDE
import se.vadretapp.vadret.radar.projection.RADAR_WEST_LONGITUDE
import se.vadretapp.vadret.radar.projection.WebMercator
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

private const val PHONE_WIDTH = 411.0
private const val PHONE_HEIGHT = 891.0
private const val TABLET_WIDTH = 1280.0
private const val TABLET_HEIGHT = 800.0
private const val TOLERANCE = 1e-6
private const val PANEL_HEIGHT = 200.0

class RadarCameraLimitsTest {

    private fun worldSize(zoom: Double): Double = 512.0 * 2.0.pow(zoom)

    private fun visibleLongitudes(
        viewportWidth: Double,
        zoom: Double
    ): Double = viewportWidth / worldSize(zoom = zoom) * 360.0

    private fun visibleMercator(
        viewportHeight: Double,
        zoom: Double
    ): Double = viewportHeight / worldSize(zoom = zoom) * 2 * PI

    private fun assertScreenInsideRadar(
        viewportWidth: Double,
        viewportHeight: Double,
        obscuredBottom: Double,
        zoom: Double
    ) {
        val radarCameraLimits: RadarCameraLimits = radarCameraLimits(
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            obscuredBottom = obscuredBottom,
            zoom = zoom
        )
        val shownZoom: Double = maxOf(zoom, radarCameraLimits.minimumZoom)
        val halfLongitudes: Double = visibleLongitudes(viewportWidth = viewportWidth, zoom = shownZoom) / 2.0
        val halfMercator: Double = visibleMercator(viewportHeight = viewportHeight, zoom = shownZoom) / 2.0
        assertTrue(actual = radarCameraLimits.westLongitude - halfLongitudes >= RADAR_WEST_LONGITUDE - TOLERANCE)
        assertTrue(actual = radarCameraLimits.eastLongitude + halfLongitudes <= RADAR_EAST_LONGITUDE + TOLERANCE)
        assertTrue(actual = WebMercator.y(latitude = radarCameraLimits.northLatitude) + halfMercator <= WebMercator.y(latitude = RADAR_NORTH_LATITUDE) + TOLERANCE)
        val obscuredMercator: Double = visibleMercator(viewportHeight = obscuredBottom, zoom = shownZoom)
        assertTrue(actual = WebMercator.y(latitude = radarCameraLimits.southLatitude) - halfMercator + obscuredMercator >= WebMercator.y(latitude = RADAR_SOUTH_LATITUDE) - TOLERANCE)
    }

    @Test
    fun `at the lowest zoom a phone screen is exactly as tall as the radar image`() {
        val radarCameraLimits: RadarCameraLimits = radarCameraLimits(
            viewportWidth = PHONE_WIDTH,
            viewportHeight = PHONE_HEIGHT,
            obscuredBottom = 0.0,
            zoom = 0.0
        )
        val radarMercatorHeight: Double = WebMercator.y(latitude = RADAR_NORTH_LATITUDE) - WebMercator.y(latitude = RADAR_SOUTH_LATITUDE)

        assertTrue(actual = abs(visibleMercator(viewportHeight = PHONE_HEIGHT, zoom = radarCameraLimits.minimumZoom) - radarMercatorHeight) < TOLERANCE)
        assertTrue(actual = abs(radarCameraLimits.northLatitude - radarCameraLimits.southLatitude) < TOLERANCE)
    }

    @Test
    fun `at the lowest zoom a wide screen is exactly as wide as the radar image`() {
        val radarCameraLimits: RadarCameraLimits = radarCameraLimits(
            viewportWidth = TABLET_WIDTH,
            viewportHeight = TABLET_HEIGHT,
            obscuredBottom = 0.0,
            zoom = 0.0
        )

        assertTrue(actual = abs(visibleLongitudes(viewportWidth = TABLET_WIDTH, zoom = radarCameraLimits.minimumZoom) - (RADAR_EAST_LONGITUDE - RADAR_WEST_LONGITUDE)) < TOLERANCE)
        assertTrue(actual = abs(radarCameraLimits.eastLongitude - radarCameraLimits.westLongitude) < TOLERANCE)
    }

    @Test
    fun `the screen never reaches outside the radar image at any zoom`() {
        listOf(0.0, 4.0, 5.5, 8.0, 12.0).forEach { zoom: Double ->
            assertScreenInsideRadar(viewportWidth = PHONE_WIDTH, viewportHeight = PHONE_HEIGHT, obscuredBottom = 0.0, zoom = zoom)
            assertScreenInsideRadar(viewportWidth = PHONE_WIDTH, viewportHeight = PHONE_HEIGHT, obscuredBottom = PANEL_HEIGHT, zoom = zoom)
            assertScreenInsideRadar(viewportWidth = TABLET_WIDTH, viewportHeight = TABLET_HEIGHT, obscuredBottom = 0.0, zoom = zoom)
        }
    }

    @Test
    fun `the south edge of the image can reach the top of the playback card`() {
        val radarCameraLimits: RadarCameraLimits = radarCameraLimits(
            viewportWidth = PHONE_WIDTH,
            viewportHeight = PHONE_HEIGHT,
            obscuredBottom = PANEL_HEIGHT,
            zoom = 8.0
        )
        val belowCenter: Double = visibleMercator(viewportHeight = PHONE_HEIGHT / 2.0 - PANEL_HEIGHT, zoom = 8.0)

        assertTrue(actual = abs(WebMercator.y(latitude = radarCameraLimits.southLatitude) - belowCenter - WebMercator.y(latitude = RADAR_SOUTH_LATITUDE)) < TOLERANCE)
    }

    @Test
    fun `at the lowest zoom the uncovered part of a phone is as tall as the radar image`() {
        val radarCameraLimits: RadarCameraLimits = radarCameraLimits(
            viewportWidth = PHONE_WIDTH,
            viewportHeight = PHONE_HEIGHT,
            obscuredBottom = PANEL_HEIGHT,
            zoom = 0.0
        )
        val radarMercatorHeight: Double = WebMercator.y(latitude = RADAR_NORTH_LATITUDE) - WebMercator.y(latitude = RADAR_SOUTH_LATITUDE)

        assertTrue(actual = abs(visibleMercator(viewportHeight = PHONE_HEIGHT - PANEL_HEIGHT, zoom = radarCameraLimits.minimumZoom) - radarMercatorHeight) < TOLERANCE)
    }

    @Test
    fun `zoomed in the centre can reach close to the edges`() {
        val radarCameraLimits: RadarCameraLimits = radarCameraLimits(
            viewportWidth = PHONE_WIDTH,
            viewportHeight = PHONE_HEIGHT,
            obscuredBottom = 0.0,
            zoom = 12.0
        )

        assertTrue(actual = RADAR_EAST_LONGITUDE - radarCameraLimits.eastLongitude < 0.05)
        assertTrue(actual = radarCameraLimits.westLongitude - RADAR_WEST_LONGITUDE < 0.05)
        assertTrue(actual = RADAR_NORTH_LATITUDE - radarCameraLimits.northLatitude < 0.05)
        assertTrue(actual = radarCameraLimits.southLatitude - RADAR_SOUTH_LATITUDE < 0.05)
    }
}