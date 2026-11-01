package se.vadretapp.vadret.radar.ui.overlay

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val TOLERANCE = 0.01f
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
}