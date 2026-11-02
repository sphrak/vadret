package se.vadretapp.vadret.radar.ui.overlay

import androidx.compose.ui.geometry.Offset
import se.vadretapp.vadret.location.model.Location
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val TOLERANCE = 0.01f
private const val COORDINATE_TOLERANCE = 0.0001
private const val VIEWPORT_WIDTH = 1080f
private const val VIEWPORT_HEIGHT = 2340f

class CameraScreenProjectionTest {

    private fun projection(
        zoom: Double,
        density: Float = 1f
    ): CameraScreenProjection =
        CameraScreenProjection(
            centerLatitude = 59.3293,
            centerLongitude = 18.0686,
            zoom = zoom,
            viewportWidth = VIEWPORT_WIDTH,
            viewportHeight = VIEWPORT_HEIGHT,
            density = density
        )

    private fun assertNear(
        expected: Offset,
        actual: Offset
    ) {
        assertTrue(
            actual = abs(expected.x - actual.x) < TOLERANCE && abs(expected.y - actual.y) < TOLERANCE,
            message = "expected $expected but was $actual"
        )
    }

    @Test
    fun `the camera center lands in the middle of the viewport`() {
        assertNear(
            expected = Offset(x = VIEWPORT_WIDTH / 2, y = VIEWPORT_HEIGHT / 2),
            actual = projection(zoom = 5.0).screenOffset(latitude = 59.3293, longitude = 18.0686)
        )
    }

    @Test
    fun `a whole world is one tile wide at zoom zero`() {
        val center: Offset = projection(zoom = 0.0).screenOffset(latitude = 59.3293, longitude = 18.0686)
        val halfTurnEast: Offset = projection(zoom = 0.0).screenOffset(latitude = 59.3293, longitude = 18.0686 + 180.0)

        assertEquals(expected = 256f, actual = halfTurnEast.x - center.x, absoluteTolerance = TOLERANCE)
    }

    @Test
    fun `each zoom level doubles the distances and density scales them`() {
        val near: Float = projection(zoom = 4.0).screenOffset(latitude = 59.3293, longitude = 19.0686).x - VIEWPORT_WIDTH / 2
        val far: Float = projection(zoom = 5.0).screenOffset(latitude = 59.3293, longitude = 19.0686).x - VIEWPORT_WIDTH / 2
        val dense: Float = projection(zoom = 4.0, density = 3f).screenOffset(latitude = 59.3293, longitude = 19.0686).x - VIEWPORT_WIDTH / 2

        assertEquals(expected = near * 2, actual = far, absoluteTolerance = TOLERANCE)
        assertEquals(expected = near * 3, actual = dense, absoluteTolerance = TOLERANCE)
    }

    @Test
    fun `north is up and east is right`() {
        val center: Offset = projection(zoom = 6.0).screenOffset(latitude = 59.3293, longitude = 18.0686)
        val northEast: Offset = projection(zoom = 6.0).screenOffset(latitude = 60.3293, longitude = 19.0686)

        assertTrue(actual = northEast.x > center.x && northEast.y < center.y)
    }

    @Test
    fun `a screen point turns back into the place that was projected onto it`() {
        listOf(
            Location(latitude = 55.6050, longitude = 13.0038),
            Location(latitude = 59.3293, longitude = 18.0686),
            Location(latitude = 67.8558, longitude = 20.2253)
        ).forEach { location: Location ->
            val cameraScreenProjection: CameraScreenProjection = projection(zoom = 7.0, density = 2.75f)
            val screenOffset: Offset = cameraScreenProjection.screenOffset(latitude = location.latitude, longitude = location.longitude)

            val projectedBack: Location = cameraScreenProjection.location(screenOffset = screenOffset)

            assertEquals(expected = location.latitude, actual = projectedBack.latitude, absoluteTolerance = COORDINATE_TOLERANCE)
            assertEquals(expected = location.longitude, actual = projectedBack.longitude, absoluteTolerance = COORDINATE_TOLERANCE)
        }
    }

    @Test
    fun `the middle of the viewport is the camera center`() {
        val center: Location = projection(zoom = 5.0).location(screenOffset = Offset(x = VIEWPORT_WIDTH / 2, y = VIEWPORT_HEIGHT / 2))

        assertEquals(expected = 59.3293, actual = center.latitude, absoluteTolerance = COORDINATE_TOLERANCE)
        assertEquals(expected = 18.0686, actual = center.longitude, absoluteTolerance = COORDINATE_TOLERANCE)
    }
}