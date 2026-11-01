package se.vadretapp.vadret.radar.ui.map.extension

import org.maplibre.android.camera.CameraPosition
import se.vadretapp.vadret.radar.ui.map.RadarCamera
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RadarCameraTest {

    @Test
    fun `a camera survives the round trip through maplibre`() {
        val radarCamera = RadarCamera(
            latitude = 59.33,
            longitude = 18.07,
            zoom = 7.5
        )

        assertEquals(
            expected = radarCamera,
            actual = radarCamera.toCameraPosition().toRadarCamera()
        )
    }

    @Test
    fun `a camera without target is ignored`() {
        val cameraPosition: CameraPosition = CameraPosition.Builder()
            .zoom(7.5)
            .build()

        assertNull(actual = cameraPosition.toRadarCamera())
    }
}