package se.vadretapp.vadret.icons

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathNode

val VadretIcons.Weather.HeavySnowShowers: VadretIconGraphic
    get() = heavySnowShowers

private val heavySnowShowers: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "HeavySnowShowers",
        layers = listOf(
            IconLayer.Masked(
                cutouts = listOf(
                    IconCutout(shape = cloudSilhouette, placement = IconPlacement(translateX = 4f, translateY = 74f, scale = 0.083333f), marginWidth = 43.2f)
                ),
                elements = listOf(
                    IconElement(shape = cloudOutline, paint = IconPaint.Fill, placement = IconPlacement(translateX = 42f, translateY = 44f, scale = 0.054167f), motion = IconMotion(kind = IconMotionKind.DRIFT, durationMilliseconds = 6000, delayMilliseconds = 0, easing = IconEasing.EASE_IN_OUT)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = -4.5f, y = 0f), PathNode.LineTo(x = 4.5f, y = 0f))), paint = IconPaint.Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Miter), placement = IconPlacement(translateX = 23f, translateY = 69f, scale = 1f), motion = IconMotion(kind = IconMotionKind.SLANTED_FLAKE, durationMilliseconds = 2600, delayMilliseconds = 0, easing = IconEasing.EASE_IN_OUT)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = -2.25f, y = -3.9f), PathNode.LineTo(x = 2.25f, y = 3.9f))), paint = IconPaint.Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Miter), placement = IconPlacement(translateX = 23f, translateY = 69f, scale = 1f), motion = IconMotion(kind = IconMotionKind.SLANTED_FLAKE, durationMilliseconds = 2600, delayMilliseconds = 0, easing = IconEasing.EASE_IN_OUT)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 2.25f, y = -3.9f), PathNode.LineTo(x = -2.25f, y = 3.9f))), paint = IconPaint.Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Miter), placement = IconPlacement(translateX = 23f, translateY = 69f, scale = 1f), motion = IconMotion(kind = IconMotionKind.SLANTED_FLAKE, durationMilliseconds = 2600, delayMilliseconds = 0, easing = IconEasing.EASE_IN_OUT)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = -4.5f, y = 0f), PathNode.LineTo(x = 4.5f, y = 0f))), paint = IconPaint.Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Miter), placement = IconPlacement(translateX = 33.5f, translateY = 74f, scale = 1f), motion = IconMotion(kind = IconMotionKind.SLANTED_FLAKE, durationMilliseconds = 2600, delayMilliseconds = -520, easing = IconEasing.EASE_IN_OUT)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = -2.25f, y = -3.9f), PathNode.LineTo(x = 2.25f, y = 3.9f))), paint = IconPaint.Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Miter), placement = IconPlacement(translateX = 33.5f, translateY = 74f, scale = 1f), motion = IconMotion(kind = IconMotionKind.SLANTED_FLAKE, durationMilliseconds = 2600, delayMilliseconds = -520, easing = IconEasing.EASE_IN_OUT)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 2.25f, y = -3.9f), PathNode.LineTo(x = -2.25f, y = 3.9f))), paint = IconPaint.Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Miter), placement = IconPlacement(translateX = 33.5f, translateY = 74f, scale = 1f), motion = IconMotion(kind = IconMotionKind.SLANTED_FLAKE, durationMilliseconds = 2600, delayMilliseconds = -520, easing = IconEasing.EASE_IN_OUT)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = -4.5f, y = 0f), PathNode.LineTo(x = 4.5f, y = 0f))), paint = IconPaint.Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Miter), placement = IconPlacement(translateX = 44f, translateY = 69f, scale = 1f), motion = IconMotion(kind = IconMotionKind.SLANTED_FLAKE, durationMilliseconds = 2600, delayMilliseconds = -1040, easing = IconEasing.EASE_IN_OUT)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = -2.25f, y = -3.9f), PathNode.LineTo(x = 2.25f, y = 3.9f))), paint = IconPaint.Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Miter), placement = IconPlacement(translateX = 44f, translateY = 69f, scale = 1f), motion = IconMotion(kind = IconMotionKind.SLANTED_FLAKE, durationMilliseconds = 2600, delayMilliseconds = -1040, easing = IconEasing.EASE_IN_OUT)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 2.25f, y = -3.9f), PathNode.LineTo(x = -2.25f, y = 3.9f))), paint = IconPaint.Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Miter), placement = IconPlacement(translateX = 44f, translateY = 69f, scale = 1f), motion = IconMotion(kind = IconMotionKind.SLANTED_FLAKE, durationMilliseconds = 2600, delayMilliseconds = -1040, easing = IconEasing.EASE_IN_OUT)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = -4.5f, y = 0f), PathNode.LineTo(x = 4.5f, y = 0f))), paint = IconPaint.Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Miter), placement = IconPlacement(translateX = 54.5f, translateY = 74f, scale = 1f), motion = IconMotion(kind = IconMotionKind.SLANTED_FLAKE, durationMilliseconds = 2600, delayMilliseconds = -1560, easing = IconEasing.EASE_IN_OUT)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = -2.25f, y = -3.9f), PathNode.LineTo(x = 2.25f, y = 3.9f))), paint = IconPaint.Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Miter), placement = IconPlacement(translateX = 54.5f, translateY = 74f, scale = 1f), motion = IconMotion(kind = IconMotionKind.SLANTED_FLAKE, durationMilliseconds = 2600, delayMilliseconds = -1560, easing = IconEasing.EASE_IN_OUT)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 2.25f, y = -3.9f), PathNode.LineTo(x = -2.25f, y = 3.9f))), paint = IconPaint.Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Miter), placement = IconPlacement(translateX = 54.5f, translateY = 74f, scale = 1f), motion = IconMotion(kind = IconMotionKind.SLANTED_FLAKE, durationMilliseconds = 2600, delayMilliseconds = -1560, easing = IconEasing.EASE_IN_OUT)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = -4.5f, y = 0f), PathNode.LineTo(x = 4.5f, y = 0f))), paint = IconPaint.Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Miter), placement = IconPlacement(translateX = 65f, translateY = 69f, scale = 1f), motion = IconMotion(kind = IconMotionKind.SLANTED_FLAKE, durationMilliseconds = 2600, delayMilliseconds = -2080, easing = IconEasing.EASE_IN_OUT)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = -2.25f, y = -3.9f), PathNode.LineTo(x = 2.25f, y = 3.9f))), paint = IconPaint.Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Miter), placement = IconPlacement(translateX = 65f, translateY = 69f, scale = 1f), motion = IconMotion(kind = IconMotionKind.SLANTED_FLAKE, durationMilliseconds = 2600, delayMilliseconds = -2080, easing = IconEasing.EASE_IN_OUT)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 2.25f, y = -3.9f), PathNode.LineTo(x = -2.25f, y = 3.9f))), paint = IconPaint.Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Miter), placement = IconPlacement(translateX = 65f, translateY = 69f, scale = 1f), motion = IconMotion(kind = IconMotionKind.SLANTED_FLAKE, durationMilliseconds = 2600, delayMilliseconds = -2080, easing = IconEasing.EASE_IN_OUT))
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