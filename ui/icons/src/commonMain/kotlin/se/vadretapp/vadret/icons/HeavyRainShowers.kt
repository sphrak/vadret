package se.vadretapp.vadret.icons

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathNode

val VadretIcons.Weather.HeavyRainShowers: VadretIconGraphic
    get() = heavyRainShowers

private val heavyRainShowers: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "HeavyRainShowers",
        layers = listOf(
            IconLayer.Masked(
                cutouts = listOf(
                    IconCutout(shape = cloudSilhouette, placement = IconPlacement(translateX = 7f, translateY = 73f, scale = 0.089583f), marginWidth = null)
                ),
                elements = listOf(
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 29f, y = 62f), PathNode.LineTo(x = 23.54f, y = 75f))), paint = IconPaint.Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Miter), motion = IconMotion(kind = IconMotionKind.SLANTED_FALL, durationMilliseconds = 900, delayMilliseconds = 0, easing = IconEasing.LINEAR)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 39.5f, y = 67f), PathNode.LineTo(x = 34.04f, y = 80f))), paint = IconPaint.Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Miter), motion = IconMotion(kind = IconMotionKind.SLANTED_FALL, durationMilliseconds = 900, delayMilliseconds = -180, easing = IconEasing.LINEAR)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 50f, y = 62f), PathNode.LineTo(x = 44.54f, y = 75f))), paint = IconPaint.Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Miter), motion = IconMotion(kind = IconMotionKind.SLANTED_FALL, durationMilliseconds = 900, delayMilliseconds = -360, easing = IconEasing.LINEAR)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 60.5f, y = 67f), PathNode.LineTo(x = 55.04f, y = 80f))), paint = IconPaint.Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Miter), motion = IconMotion(kind = IconMotionKind.SLANTED_FALL, durationMilliseconds = 900, delayMilliseconds = -540, easing = IconEasing.LINEAR)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 71f, y = 62f), PathNode.LineTo(x = 65.54f, y = 75f))), paint = IconPaint.Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Miter), motion = IconMotion(kind = IconMotionKind.SLANTED_FALL, durationMilliseconds = 900, delayMilliseconds = -720, easing = IconEasing.LINEAR))
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