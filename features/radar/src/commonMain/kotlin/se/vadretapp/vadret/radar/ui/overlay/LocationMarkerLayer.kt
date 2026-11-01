package se.vadretapp.vadret.radar.ui.overlay

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.shadow.DropShadowPainter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.radar.wind.WindAtLocation

private const val DOT_RADIUS_DP = 6f
private const val DOT_BORDER_DP = 2f
private const val LABEL_HEIGHT_DP = 32f
private const val LABEL_GAP_DP = 14f
private const val LABEL_PADDING_DP = 12f
private const val LABEL_ICON_DP = 16f
private const val LABEL_ICON_GAP_DP = 4f
private const val SINGLE_LINE = 1
private const val HALF = 2f
private const val FULLY_ROUNDED_PERCENT = 50

private val markerShadow = Shadow(
    radius = 3.dp,
    color = Color(color = 0x40000000),
    offset = DpOffset(
        x = 0.dp,
        y = 1.dp
    )
)

internal class LocationMarkerLayer(
    private val markerColor: Color,
    private val surfaceColor: Color
) {

    private val dotShadow = DropShadowPainter(
        shape = CircleShape,
        shadow = markerShadow
    )
    private val labelShadow = DropShadowPainter(
        shape = RoundedCornerShape(percent = FULLY_ROUNDED_PERCENT),
        shadow = markerShadow
    )
    private val arrowColorFilter: ColorFilter = ColorFilter.tint(color = markerColor)

    fun DrawScope.drawMarker(
        screenProjection: ScreenProjection,
        location: Location,
        windAtLocation: WindAtLocation?,
        windSpeedText: String?,
        arrow: Painter,
        textMeasurer: TextMeasurer,
        labelStyle: TextStyle
    ) {
        val point: Offset = screenProjection.screenOffset(
            latitude = location.latitude,
            longitude = location.longitude
        ) ?: return
        val borderRadius: Float = (DOT_RADIUS_DP + DOT_BORDER_DP) * density
        drawShadow(
            painter = dotShadow,
            topLeft = Offset(
                x = point.x - borderRadius,
                y = point.y - borderRadius
            ),
            size = Size(
                width = borderRadius * HALF,
                height = borderRadius * HALF
            )
        )
        drawCircle(
            color = surfaceColor,
            radius = borderRadius,
            center = point
        )
        drawCircle(
            color = markerColor,
            radius = DOT_RADIUS_DP * density,
            center = point
        )
        if (windAtLocation != null && windSpeedText != null) {
            drawWindLabel(
                center = point,
                windAtLocation = windAtLocation,
                arrow = arrow,
                textLayoutResult = textMeasurer.measure(
                    text = windSpeedText,
                    style = labelStyle,
                    maxLines = SINGLE_LINE
                )
            )
        }
    }

    private fun DrawScope.drawWindLabel(
        center: Offset,
        windAtLocation: WindAtLocation,
        arrow: Painter,
        textLayoutResult: TextLayoutResult
    ) {
        val iconSize: Float = LABEL_ICON_DP * density
        val padding: Float = LABEL_PADDING_DP * density
        val iconGap: Float = LABEL_ICON_GAP_DP * density
        val labelWidth: Float = padding + iconSize + iconGap + textLayoutResult.size.width + padding
        val labelHeight: Float = LABEL_HEIGHT_DP * density
        val bottom: Float = center.y - LABEL_GAP_DP * density
        val left: Float = center.x - labelWidth / HALF
        val top: Float = bottom - labelHeight
        val labelCenterY: Float = top + labelHeight / HALF
        val labelTopLeft = Offset(
            x = left,
            y = top
        )
        val labelSize = Size(
            width = labelWidth,
            height = labelHeight
        )
        drawShadow(
            painter = labelShadow,
            topLeft = labelTopLeft,
            size = labelSize
        )
        drawRoundRect(
            color = surfaceColor,
            topLeft = labelTopLeft,
            size = labelSize,
            cornerRadius = CornerRadius(x = labelHeight / HALF)
        )
        val iconLeft: Float = left + padding
        val iconTop: Float = labelCenterY - iconSize / HALF
        rotate(
            degrees = windAtLocation.towardDegrees,
            pivot = Offset(
                x = iconLeft + iconSize / HALF,
                y = labelCenterY
            )
        ) {
            translate(
                left = iconLeft,
                top = iconTop
            ) {
                with(receiver = arrow) {
                    draw(
                        size = Size(
                            width = iconSize,
                            height = iconSize
                        ),
                        colorFilter = arrowColorFilter
                    )
                }
            }
        }
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(
                x = iconLeft + iconSize + iconGap,
                y = labelCenterY - textLayoutResult.size.height / HALF
            )
        )
    }

    private fun DrawScope.drawShadow(
        painter: DropShadowPainter,
        topLeft: Offset,
        size: Size
    ) {
        translate(
            left = topLeft.x,
            top = topLeft.y
        ) {
            with(receiver = painter) {
                draw(size = size)
            }
        }
    }
}