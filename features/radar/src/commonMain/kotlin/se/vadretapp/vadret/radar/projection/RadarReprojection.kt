package se.vadretapp.vadret.radar.projection

internal interface RadarReprojection {
    fun reproject(radarRaster: RadarRaster): ReprojectedImage
}