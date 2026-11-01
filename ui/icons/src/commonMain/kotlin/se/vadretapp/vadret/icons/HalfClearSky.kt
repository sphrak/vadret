package se.vadretapp.vadret.icons

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin

val VadretIcons.Weather.HalfClearSky: VadretIconGraphic
    get() = halfClearSky

private val halfClearSky: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "HalfClearSky",
        layers = listOf(
            IconLayer.Masked(
                cutouts = listOf(
                    IconCutout(shape = cloudSilhouette, placement = IconPlacement(translateX = 6f, translateY = 100f, scale = 0.083333f), marginWidth = 43.2f)
                ),
                elements = listOf(
                    IconElement(shape = IconShape.Circle(centerX = 26f, centerY = 26f, radius = 14f), paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Butt, join = StrokeJoin.Miter)),
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