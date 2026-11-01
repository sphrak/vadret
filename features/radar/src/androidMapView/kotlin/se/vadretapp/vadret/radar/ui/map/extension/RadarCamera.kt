package se.vadretapp.vadret.radar.ui.map.extension

import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import se.vadretapp.vadret.radar.ui.map.RadarCamera

internal fun RadarCamera.toCameraPosition(): CameraPosition =
    CameraPosition.Builder()
        .target(LatLng(latitude, longitude))
        .zoom(zoom)
        .build()