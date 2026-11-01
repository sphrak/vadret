package se.vadretapp.vadret.icons

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathNode

val VadretIcons.Weather.Thunderstorm: VadretIconGraphic
    get() = thunderstorm

private val thunderstorm: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "Thunderstorm",
        layers = listOf(
            IconLayer.Masked(
                cutouts = listOf(
                    IconCutout(shape = cloudSilhouette, placement = IconPlacement(translateX = 7f, translateY = 73f, scale = 0.089583f), marginWidth = 40.19f)
                ),
                elements = listOf(
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 22f, y = 62f), PathNode.LineTo(x = 19.48f, y = 68f))), paint = IconPaint.Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Miter), motion = IconMotion(kind = IconMotionKind.SLANTED_FALL, durationMilliseconds = 900, delayMilliseconds = 0, easing = IconEasing.LINEAR)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 30f, y = 67f), PathNode.LineTo(x = 27.48f, y = 73f))), paint = IconPaint.Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Miter), motion = IconMotion(kind = IconMotionKind.SLANTED_FALL, durationMilliseconds = 900, delayMilliseconds = -230, easing = IconEasing.LINEAR)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 72f, y = 62f), PathNode.LineTo(x = 69.48f, y = 68f))), paint = IconPaint.Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Miter), motion = IconMotion(kind = IconMotionKind.SLANTED_FALL, durationMilliseconds = 900, delayMilliseconds = -450, easing = IconEasing.LINEAR)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 80f, y = 67f), PathNode.LineTo(x = 77.48f, y = 73f))), paint = IconPaint.Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Miter), motion = IconMotion(kind = IconMotionKind.SLANTED_FALL, durationMilliseconds = 900, delayMilliseconds = -680, easing = IconEasing.LINEAR)),
                    IconElement(shape = bolt, paint = IconPaint.Fill, placement = IconPlacement(translateX = 23f, translateY = 103f, scale = 0.05625f), motion = IconMotion(kind = IconMotionKind.BOLT, durationMilliseconds = 3000, delayMilliseconds = 0, easing = IconEasing.LINEAR))
                )
            ),
            IconLayer.Plain(
                elements = listOf(
                    IconElement(shape = cloudOutline, paint = IconPaint.Fill, placement = IconPlacement(translateX = 7f, translateY = 73f, scale = 0.089583f))
                )
            )
        )
    )
}