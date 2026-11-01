package se.vadretapp.vadret.radar.ui.map

private const val INITIAL_LATITUDE = 58.62
private const val INITIAL_LONGITUDE = 15.57
private const val INITIAL_ZOOM_LEVEL = 5.0

internal data class RadarCamera(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double
)

internal val initialRadarCamera: RadarCamera = RadarCamera(
    latitude = INITIAL_LATITUDE,
    longitude = INITIAL_LONGITUDE,
    zoom = INITIAL_ZOOM_LEVEL
)