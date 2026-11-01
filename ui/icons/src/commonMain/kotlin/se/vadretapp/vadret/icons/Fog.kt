package se.vadretapp.vadret.icons

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathNode

val VadretIcons.Weather.Fog: VadretIconGraphic
    get() = fog

private val fog: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "Fog",
        layers = listOf(
            IconLayer.Plain(
                elements = listOf(
                    IconElement(shape = cloudOutline, paint = IconPaint.Fill, placement = IconPlacement(translateX = 7f, translateY = 75f, scale = 0.089583f)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 24f, y = 70f), PathNode.LineTo(x = 76f, y = 70f))), paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Miter), motion = IconMotion(kind = IconMotionKind.FOG, durationMilliseconds = 4000, delayMilliseconds = 0, easing = IconEasing.EASE_IN_OUT)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 24f, y = 80f), PathNode.LineTo(x = 76f, y = 80f))), paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Miter), motion = IconMotion(kind = IconMotionKind.FOG, durationMilliseconds = 4000, delayMilliseconds = -2000, easing = IconEasing.EASE_IN_OUT))
                )
            )
        )
    )
}