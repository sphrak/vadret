package se.vadretapp.vadret.radar.ui.map

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Projection
import se.vadretapp.vadret.radar.model.PollenMarker
import se.vadretapp.vadret.radar.pollen.PollenColorScale

private const val DOT_RADIUS_DP = 8f
private const val DOT_BORDER_DP = 2f

internal class PollenMarkerDrawer(
    private val density: Float
) {

    var pollenMarkers: ImmutableList<PollenMarker> = persistentListOf()

    private val borderPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setBorderColor(borderColor: Int) {
        borderPaint.color = borderColor
    }

    fun draw(
        canvas: Canvas,
        projection: Projection
    ) {
        pollenMarkers.forEach { pollenMarker: PollenMarker ->
            val point: PointF = projection.toScreenLocation(LatLng(pollenMarker.latitude, pollenMarker.longitude))
            dotPaint.color = PollenColorScale.argb(pollenLevel = pollenMarker.pollenLevel)
            canvas.drawCircle(point.x, point.y, (DOT_RADIUS_DP + DOT_BORDER_DP) * density, borderPaint)
            canvas.drawCircle(point.x, point.y, DOT_RADIUS_DP * density, dotPaint)
        }
    }
}