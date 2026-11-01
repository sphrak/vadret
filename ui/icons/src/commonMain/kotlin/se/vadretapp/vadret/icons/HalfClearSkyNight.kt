package se.vadretapp.vadret.icons

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathNode

val VadretIcons.Weather.HalfClearSkyNight: VadretIconGraphic
    get() = halfClearSkyNight

private val halfClearSkyNightOutline1: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 40.13f, y = 31.04f),
        PathNode.ArcTo(horizontalEllipseRadius = 15f, verticalEllipseRadius = 15f, theta = 0f, isMoreThanHalf = true, isPositiveArc = true, arcStartX = 25.88f, arcStartY = 11f),
        PathNode.ArcTo(horizontalEllipseRadius = 12.3f, verticalEllipseRadius = 12.3f, theta = 0f, isMoreThanHalf = true, isPositiveArc = false, arcStartX = 40.13f, arcStartY = 31.04f),
        PathNode.Close
    )
)

private val halfClearSkyNight: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "HalfClearSkyNight",
        layers = listOf(
            IconLayer.Masked(
                cutouts = listOf(
                    IconCutout(shape = cloudSilhouette, placement = IconPlacement(translateX = 6f, translateY = 100f, scale = 0.083333f), marginWidth = 43.2f)
                ),
                elements = listOf(
                    IconElement(shape = halfClearSkyNightOutline1, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Butt, join = StrokeJoin.Round)),
                    IconElement(shape = cloudOutline, paint = IconPaint.Fill, placement = IconPlacement(translateX = 38f, translateY = 60f, scale = 0.0625f), motion = IconMotion(kind = IconMotionKind.DRIFT, durationMilliseconds = 6000, delayMilliseconds = 0, easing = IconEasing.EASE_IN_OUT))
                )
            ),
            IconLayer.Plain(
                elements = listOf(
                    IconElement(shape = cloudOutline, paint = IconPaint.Fill, placement = IconPlacement(translateX = 6f, translateY = 100f, scale = 0.083333f))
                )
            )
        )
    )
}