package se.vadretapp.vadret.radar.ui.overlay

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.collections.immutable.ImmutableList
import se.vadretapp.vadret.radar.model.PollenMarker
import se.vadretapp.vadret.radar.pollen.PollenColorScale

private const val DOT_RADIUS_DP = 8f
private const val DOT_BORDER_DP = 2f

internal class PollenMarkerLayer(
    private val borderColor: Color
) {

    fun DrawScope.drawPollenMarkers(
        screenProjection: ScreenProjection,
        pollenMarkers: ImmutableList<PollenMarker>
    ) {
        pollenMarkers.forEach { pollenMarker: PollenMarker ->
            val point: Offset? = screenProjection.screenOffset(
                latitude = pollenMarker.latitude,
                longitude = pollenMarker.longitude
            )
            if (point != null) {
                drawCircle(
                    color = borderColor,
                    radius = (DOT_RADIUS_DP + DOT_BORDER_DP) * density,
                    center = point
                )
                drawCircle(
                    color = Color(color = PollenColorScale.argb(pollenLevel = pollenMarker.pollenLevel)),
                    radius = DOT_RADIUS_DP * density,
                    center = point
                )
            }
        }
    }
}