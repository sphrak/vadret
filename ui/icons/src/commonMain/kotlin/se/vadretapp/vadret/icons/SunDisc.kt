package se.vadretapp.vadret.icons

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin

val VadretIcons.Symbol.SunDisc: VadretIconGraphic
    get() = sunDisc

private val sunDisc: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "SunDisc",
        layers = listOf(
            IconLayer.Plain(
                elements = listOf(
                    IconElement(shape = IconShape.Circle(centerX = 50f, centerY = 50f, radius = 26f), paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Butt, join = StrokeJoin.Miter))
                )
            )
        )
    )
}