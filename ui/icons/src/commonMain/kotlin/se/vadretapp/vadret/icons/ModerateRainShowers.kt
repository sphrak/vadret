package se.vadretapp.vadret.icons

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathNode

val VadretIcons.Weather.ModerateRainShowers: VadretIconGraphic
    get() = moderateRainShowers

private val moderateRainShowers: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "ModerateRainShowers",
        layers = listOf(
            IconLayer.Masked(
                cutouts = listOf(
                    IconCutout(shape = cloudSilhouette, placement = IconPlacement(translateX = 7f, translateY = 73f, scale = 0.089583f), marginWidth = null)
                ),
                elements = listOf(
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 29f, y = 62f), PathNode.LineTo(x = 25.22f, y = 71f))), paint = IconPaint.Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Miter), motion = IconMotion(kind = IconMotionKind.SLANTED_FALL, durationMilliseconds = 900, delayMilliseconds = 0, easing = IconEasing.LINEAR)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 43f, y = 67f), PathNode.LineTo(x = 39.22f, y = 76f))), paint = IconPaint.Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Miter), motion = IconMotion(kind = IconMotionKind.SLANTED_FALL, durationMilliseconds = 900, delayMilliseconds = -230, easing = IconEasing.LINEAR)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 57f, y = 62f), PathNode.LineTo(x = 53.22f, y = 71f))), paint = IconPaint.Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Miter), motion = IconMotion(kind = IconMotionKind.SLANTED_FALL, durationMilliseconds = 900, delayMilliseconds = -450, easing = IconEasing.LINEAR)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 71f, y = 67f), PathNode.LineTo(x = 67.22f, y = 76f))), paint = IconPaint.Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Miter), motion = IconMotion(kind = IconMotionKind.SLANTED_FALL, durationMilliseconds = 900, delayMilliseconds = -680, easing = IconEasing.LINEAR))
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