package se.vadretapp.vadret.radar.ui.map

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Projection
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.radar.wind.WindAtLocation

private const val DOT_RADIUS_DP = 6f
private const val DOT_BORDER_DP = 2f
private const val SHADOW_RADIUS_DP = 3f
private const val SHADOW_OFFSET_DP = 1f
private const val SHADOW_COLOR = 0x40000000
private const val LABEL_HEIGHT_DP = 32f
private const val LABEL_GAP_DP = 14f
private const val LABEL_PADDING_DP = 12f
private const val LABEL_ICON_DP = 16f
private const val LABEL_ICON_GAP_DP = 4f
private const val LABEL_TEXT_SP = 14f
private const val TABULAR_NUMBERS = "tnum"
private const val MEDIUM_FONT_FAMILY = "sans-serif-medium"
private const val HALF = 2f

internal class LocationMarkerDrawer(
    private val density: Float,
    private val scaledDensity: Float,
    private val arrow: Drawable
) {

    var location: Location? = null
    var windAtLocation: WindAtLocation? = null
    var windSpeedText: String? = null

    private val labelBounds: RectF = RectF()
    private val borderPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        setShadowLayer(SHADOW_RADIUS_DP * density, 0f, SHADOW_OFFSET_DP * density, SHADOW_COLOR)
    }
    private val dotPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = LABEL_TEXT_SP * scaledDensity
        typeface = Typeface.create(MEDIUM_FONT_FAMILY, Typeface.NORMAL)
        fontFeatureSettings = TABULAR_NUMBERS
    }

    fun setColors(
        markerColor: Int,
        surfaceColor: Int,
        labelTextColor: Int
    ) {
        borderPaint.color = surfaceColor
        dotPaint.color = markerColor
        textPaint.color = labelTextColor
        arrow.setTint(markerColor)
    }

    fun draw(
        canvas: Canvas,
        projection: Projection
    ) {
        val markerLocation: Location = location ?: return
        val point: PointF = projection.toScreenLocation(LatLng(markerLocation.latitude, markerLocation.longitude))
        canvas.drawCircle(point.x, point.y, (DOT_RADIUS_DP + DOT_BORDER_DP) * density, borderPaint)
        canvas.drawCircle(point.x, point.y, DOT_RADIUS_DP * density, dotPaint)
        val wind: WindAtLocation? = windAtLocation
        val text: String? = windSpeedText
        if (wind != null && text != null) {
            drawWindLabel(
                canvas = canvas,
                centerX = point.x,
                centerY = point.y,
                windAtLocation = wind,
                text = text
            )
        }
    }

    private fun drawWindLabel(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        windAtLocation: WindAtLocation,
        text: String
    ) {
        val iconSize: Float = LABEL_ICON_DP * density
        val padding: Float = LABEL_PADDING_DP * density
        val labelWidth: Float = padding + iconSize + LABEL_ICON_GAP_DP * density + textPaint.measureText(text) + padding
        val labelHeight: Float = LABEL_HEIGHT_DP * density
        val bottom: Float = centerY - LABEL_GAP_DP * density
        labelBounds.set(centerX - labelWidth / HALF, bottom - labelHeight, centerX + labelWidth / HALF, bottom)
        canvas.drawRoundRect(labelBounds, labelHeight / HALF, labelHeight / HALF, borderPaint)
        val iconLeft: Float = labelBounds.left + padding
        val iconTop: Float = labelBounds.centerY() - iconSize / HALF
        canvas.save()
        canvas.rotate(windAtLocation.towardDegrees, iconLeft + iconSize / HALF, iconTop + iconSize / HALF)
        arrow.setBounds(iconLeft.toInt(), iconTop.toInt(), (iconLeft + iconSize).toInt(), (iconTop + iconSize).toInt())
        arrow.draw(canvas)
        canvas.restore()
        val textBaseline: Float = labelBounds.centerY() - (textPaint.descent() + textPaint.ascent()) / HALF
        canvas.drawText(text, iconLeft + iconSize + LABEL_ICON_GAP_DP * density, textBaseline, textPaint)
    }
}