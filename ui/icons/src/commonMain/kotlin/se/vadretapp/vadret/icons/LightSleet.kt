package se.vadretapp.vadret.icons

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathNode

val VadretIcons.Weather.LightSleet: VadretIconGraphic
    get() = lightSleet

private val lightSleet: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "LightSleet",
        layers = listOf(
            IconLayer.Masked(
                cutouts = listOf(
                    IconCutout(shape = cloudSilhouette, placement = IconPlacement(translateX = 7f, translateY = 73f, scale = 0.089583f), marginWidth = null)
                ),
                elements = listOf(
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 29f, y = 62f), PathNode.LineTo(x = 29f, y = 68f))), paint = IconPaint.Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Miter), motion = IconMotion(kind = IconMotionKind.FALL, durationMilliseconds = 900, delayMilliseconds = 0, easing = IconEasing.LINEAR)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = -4.5f, y = 0f), PathNode.LineTo(x = 4.5f, y = 0f))), paint = IconPaint.Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Miter), placement = IconPlacement(translateX = 50f, translateY = 70f, scale = 1f), motion = IconMotion(kind = IconMotionKind.FLAKE, durationMilliseconds = 2600, delayMilliseconds = -870, easing = IconEasing.EASE_IN_OUT)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = -2.25f, y = -3.9f), PathNode.LineTo(x = 2.25f, y = 3.9f))), paint = IconPaint.Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Miter), placement = IconPlacement(translateX = 50f, translateY = 70f, scale = 1f), motion = IconMotion(kind = IconMotionKind.FLAKE, durationMilliseconds = 2600, delayMilliseconds = -870, easing = IconEasing.EASE_IN_OUT)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 2.25f, y = -3.9f), PathNode.LineTo(x = -2.25f, y = 3.9f))), paint = IconPaint.Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Miter), placement = IconPlacement(translateX = 50f, translateY = 70f, scale = 1f), motion = IconMotion(kind = IconMotionKind.FLAKE, durationMilliseconds = 2600, delayMilliseconds = -870, easing = IconEasing.EASE_IN_OUT)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 71f, y = 62f), PathNode.LineTo(x = 71f, y = 68f))), paint = IconPaint.Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Miter), motion = IconMotion(kind = IconMotionKind.FALL, durationMilliseconds = 900, delayMilliseconds = -600, easing = IconEasing.LINEAR))
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