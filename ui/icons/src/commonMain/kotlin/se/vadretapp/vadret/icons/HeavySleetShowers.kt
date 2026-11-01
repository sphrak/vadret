package se.vadretapp.vadret.icons

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathNode

val VadretIcons.Weather.HeavySleetShowers: VadretIconGraphic
    get() = heavySleetShowers

private val heavySleetShowers: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "HeavySleetShowers",
        layers = listOf(
            IconLayer.Masked(
                cutouts = listOf(
                    IconCutout(shape = cloudSilhouette, placement = IconPlacement(translateX = 4f, translateY = 74f, scale = 0.083333f), marginWidth = 43.2f)
                ),
                elements = listOf(
                    IconElement(shape = cloudOutline, paint = IconPaint.Fill, placement = IconPlacement(translateX = 42f, translateY = 44f, scale = 0.054167f), motion = IconMotion(kind = IconMotionKind.DRIFT, durationMilliseconds = 6000, delayMilliseconds = 0, easing = IconEasing.EASE_IN_OUT)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 23f, y = 66f), PathNode.LineTo(x = 17.54f, y = 79f))), paint = IconPaint.Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Miter), motion = IconMotion(kind = IconMotionKind.SLANTED_FALL, durationMilliseconds = 900, delayMilliseconds = 0, easing = IconEasing.LINEAR)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = -4.5f, y = 0f), PathNode.LineTo(x = 4.5f, y = 0f))), paint = IconPaint.Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Miter), placement = IconPlacement(translateX = 33.5f, translateY = 74f, scale = 1f), motion = IconMotion(kind = IconMotionKind.SLANTED_FLAKE, durationMilliseconds = 2600, delayMilliseconds = -520, easing = IconEasing.EASE_IN_OUT)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = -2.25f, y = -3.9f), PathNode.LineTo(x = 2.25f, y = 3.9f))), paint = IconPaint.Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Miter), placement = IconPlacement(translateX = 33.5f, translateY = 74f, scale = 1f), motion = IconMotion(kind = IconMotionKind.SLANTED_FLAKE, durationMilliseconds = 2600, delayMilliseconds = -520, easing = IconEasing.EASE_IN_OUT)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 2.25f, y = -3.9f), PathNode.LineTo(x = -2.25f, y = 3.9f))), paint = IconPaint.Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Miter), placement = IconPlacement(translateX = 33.5f, translateY = 74f, scale = 1f), motion = IconMotion(kind = IconMotionKind.SLANTED_FLAKE, durationMilliseconds = 2600, delayMilliseconds = -520, easing = IconEasing.EASE_IN_OUT)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 44f, y = 66f), PathNode.LineTo(x = 38.54f, y = 79f))), paint = IconPaint.Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Miter), motion = IconMotion(kind = IconMotionKind.SLANTED_FALL, durationMilliseconds = 900, delayMilliseconds = -360, easing = IconEasing.LINEAR)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = -4.5f, y = 0f), PathNode.LineTo(x = 4.5f, y = 0f))), paint = IconPaint.Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Miter), placement = IconPlacement(translateX = 54.5f, translateY = 74f, scale = 1f), motion = IconMotion(kind = IconMotionKind.SLANTED_FLAKE, durationMilliseconds = 2600, delayMilliseconds = -1560, easing = IconEasing.EASE_IN_OUT)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = -2.25f, y = -3.9f), PathNode.LineTo(x = 2.25f, y = 3.9f))), paint = IconPaint.Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Miter), placement = IconPlacement(translateX = 54.5f, translateY = 74f, scale = 1f), motion = IconMotion(kind = IconMotionKind.SLANTED_FLAKE, durationMilliseconds = 2600, delayMilliseconds = -1560, easing = IconEasing.EASE_IN_OUT)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 2.25f, y = -3.9f), PathNode.LineTo(x = -2.25f, y = 3.9f))), paint = IconPaint.Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Miter), placement = IconPlacement(translateX = 54.5f, translateY = 74f, scale = 1f), motion = IconMotion(kind = IconMotionKind.SLANTED_FLAKE, durationMilliseconds = 2600, delayMilliseconds = -1560, easing = IconEasing.EASE_IN_OUT)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 65f, y = 66f), PathNode.LineTo(x = 59.54f, y = 79f))), paint = IconPaint.Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Miter), motion = IconMotion(kind = IconMotionKind.SLANTED_FALL, durationMilliseconds = 900, delayMilliseconds = -720, easing = IconEasing.LINEAR))
                )
            ),
            IconLayer.Plain(
                elements = listOf(
                    IconElement(shape = cloudOutline, paint = IconPaint.Fill, placement = IconPlacement(translateX = 4f, translateY = 74f, scale = 0.083333f))
                )
            )
        )
    )
}