package se.vadretapp.vadret.radar.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import se.vadretapp.vadret.ui.VadretTheme

// Part sizes come from the design file, where the figure is 34 units high.
private const val DESIGN_HEIGHT = 34f
private const val DESIGN_WIDTH = 21.64f

private val head = FigurePart(
    left = 6.18f,
    top = 0f,
    width = 9.27f,
    height = 9.27f,
    topCorner = 4.64f,
    bottomCorner = 4.64f
)
private val torso = FigurePart(
    left = 5.15f,
    top = 9.79f,
    width = 11.33f,
    height = 13.39f,
    topCorner = 5.67f,
    bottomCorner = 3.09f
)
private val leftArm = FigurePart(
    left = 2.27f,
    top = 10.82f,
    width = 3.92f,
    height = 11.33f,
    topCorner = 1.96f,
    bottomCorner = 1.96f
)
private val rightArm = leftArm.copy(left = 15.45f)
private val leftLeg = FigurePart(
    left = 6.08f,
    top = 20.61f,
    width = 4.33f,
    height = 13.39f,
    topCorner = 2.16f,
    bottomCorner = 2.16f
)
private val rightLeg = leftLeg.copy(left = 11.23f)

private val outlineWidth: Dp = 1.dp

internal val headCenterFraction: Float = (head.top + head.height / HALF) / DESIGN_HEIGHT

private const val ARM_DEGREES_STANDING = 8f
private const val ARM_DEGREES_CARRIED = 145f
private const val LEG_DEGREES_CARRIED = 14f
private const val HALF = 2f
internal const val STANDING = 0f
internal const val CARRIED = 1f

private data class FigurePart(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val topCorner: Float,
    val bottomCorner: Float
)

@Composable
internal fun PegmanFigure(
    height: Dp,
    isSilhouette: Boolean,
    modifier: Modifier = Modifier,
    spread: () -> Float = { STANDING }
) {
    val bodyColor: Color = if (isSilhouette) {
        VadretTheme.colorScheme.outline
    } else {
        VadretTheme.colorScheme.primary
    }
    val outlineColor: Color = VadretTheme.colorScheme.background
    Canvas(
        modifier = modifier.size(
            width = height * DESIGN_WIDTH / DESIGN_HEIGHT,
            height = height
        )
    ) {
        val unit: Float = size.height / DESIGN_HEIGHT
        val currentSpread: Float = spread()
        val armDegrees: Float = ARM_DEGREES_STANDING + (ARM_DEGREES_CARRIED - ARM_DEGREES_STANDING) * currentSpread
        val legDegrees: Float = LEG_DEGREES_CARRIED * currentSpread
        if (!isSilhouette) {
            drawFigure(
                unit = unit,
                armDegrees = armDegrees,
                legDegrees = legDegrees,
                inflate = outlineWidth.toPx(),
                color = outlineColor
            )
        }
        drawFigure(
            unit = unit,
            armDegrees = armDegrees,
            legDegrees = legDegrees,
            inflate = 0f,
            color = bodyColor
        )
    }
}

// Two passes, so the outline runs around the whole figure and not between its parts.
private fun DrawScope.drawFigure(
    unit: Float,
    armDegrees: Float,
    legDegrees: Float,
    inflate: Float,
    color: Color
) {
    drawPart(
        part = leftLeg,
        unit = unit,
        degrees = legDegrees,
        inflate = inflate,
        color = color
    )
    drawPart(
        part = rightLeg,
        unit = unit,
        degrees = -legDegrees,
        inflate = inflate,
        color = color
    )
    drawPart(
        part = torso,
        unit = unit,
        degrees = 0f,
        inflate = inflate,
        color = color
    )
    drawPart(
        part = leftArm,
        unit = unit,
        degrees = armDegrees,
        inflate = inflate,
        color = color
    )
    drawPart(
        part = rightArm,
        unit = unit,
        degrees = -armDegrees,
        inflate = inflate,
        color = color
    )
    drawPart(
        part = head,
        unit = unit,
        degrees = 0f,
        inflate = inflate,
        color = color
    )
}

private fun DrawScope.drawPart(
    part: FigurePart,
    unit: Float,
    degrees: Float,
    inflate: Float,
    color: Color
) {
    val joint = Offset(
        x = (part.left + part.width / HALF) * unit,
        y = (part.top + part.width / HALF) * unit
    )
    val topCorner = CornerRadius(x = part.topCorner * unit + inflate)
    val bottomCorner = CornerRadius(x = part.bottomCorner * unit + inflate)
    val outline: Path = Path().apply {
        addRoundRect(
            roundRect = RoundRect(
                left = part.left * unit - inflate,
                top = part.top * unit - inflate,
                right = (part.left + part.width) * unit + inflate,
                bottom = (part.top + part.height) * unit + inflate,
                topLeftCornerRadius = topCorner,
                topRightCornerRadius = topCorner,
                bottomRightCornerRadius = bottomCorner,
                bottomLeftCornerRadius = bottomCorner
            )
        )
    }
    rotate(
        degrees = degrees,
        pivot = joint
    ) {
        drawPath(
            path = outline,
            color = color
        )
    }
}