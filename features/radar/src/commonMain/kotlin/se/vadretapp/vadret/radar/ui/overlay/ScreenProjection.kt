package se.vadretapp.vadret.radar.ui.overlay

import androidx.compose.ui.geometry.Offset
import se.vadretapp.vadret.radar.projection.RADAR_EAST_LONGITUDE
import se.vadretapp.vadret.radar.projection.RADAR_NORTH_LATITUDE
import se.vadretapp.vadret.radar.projection.RADAR_SOUTH_LATITUDE
import se.vadretapp.vadret.radar.projection.RADAR_WEST_LONGITUDE
import se.vadretapp.vadret.radar.ui.map.RasterProjection

internal fun interface ScreenProjection {
    fun screenOffset(
        latitude: Double,
        longitude: Double
    ): Offset?
}

internal fun ScreenProjection.toRasterProjection(
    rasterWidth: Int,
    rasterHeight: Int
): RasterProjection? {
    val northWest: Offset = screenOffset(
        latitude = RADAR_NORTH_LATITUDE,
        longitude = RADAR_WEST_LONGITUDE
    ) ?: return null
    val southEast: Offset = screenOffset(
        latitude = RADAR_SOUTH_LATITUDE,
        longitude = RADAR_EAST_LONGITUDE
    ) ?: return null
    return RasterProjection(
        originX = northWest.x,
        originY = northWest.y,
        scaleX = (southEast.x - northWest.x) / rasterWidth,
        scaleY = (southEast.y - northWest.y) / rasterHeight
    )
}