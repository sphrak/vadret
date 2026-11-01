package se.vadretapp.vadret.radar.ui.map.extension

import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import se.vadretapp.vadret.radar.ui.map.RadarCamera

internal fun CameraPosition.toRadarCamera(): RadarCamera? {
    val cameraTarget: LatLng = target ?: return null
    return RadarCamera(
        latitude = cameraTarget.latitude,
        longitude = cameraTarget.longitude,
        zoom = zoom
    )
}