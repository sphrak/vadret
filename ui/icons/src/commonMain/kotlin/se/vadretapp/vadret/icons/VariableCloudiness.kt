package se.vadretapp.vadret.icons

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin

val VadretIcons.Weather.VariableCloudiness: VadretIconGraphic
    get() = variableCloudiness

private val variableCloudiness: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "VariableCloudiness",
        layers = listOf(
            IconLayer.Masked(
                cutouts = listOf(
                    IconCutout(shape = cloudSilhouette, placement = IconPlacement(translateX = 2f, translateY = 100f, scale = 0.0875f), marginWidth = 41.14f)
                ),
                elements = listOf(
                    IconElement(shape = IconShape.Circle(centerX = 64f, centerY = 34f, radius = 18f), paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Butt, join = StrokeJoin.Miter))
                )
            ),
            IconLayer.Plain(
                elements = listOf(
                    IconElement(shape = cloudOutline, paint = IconPaint.Fill, placement = IconPlacement(translateX = 2f, translateY = 100f, scale = 0.0875f))
                )
            )
        )
    )
}