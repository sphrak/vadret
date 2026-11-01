package se.vadretapp.vadret.icons

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathNode

val VadretIcons.Weather.VariableCloudinessNight: VadretIconGraphic
    get() = variableCloudinessNight

private val variableCloudinessNightOutline1: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 59.57f, y = 15.59f),
        PathNode.ArcTo(horizontalEllipseRadius = 17f, verticalEllipseRadius = 17f, theta = 0f, isMoreThanHalf = true, isPositiveArc = true, arcStartX = 51.27f, arcStartY = 43.27f),
        PathNode.ArcTo(horizontalEllipseRadius = 14.45f, verticalEllipseRadius = 14.45f, theta = 0f, isMoreThanHalf = true, isPositiveArc = false, arcStartX = 59.57f, arcStartY = 15.59f),
        PathNode.Close
    )
)

private val variableCloudinessNight: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "VariableCloudinessNight",
        layers = listOf(
            IconLayer.Masked(
                cutouts = listOf(
                    IconCutout(shape = cloudSilhouette, placement = IconPlacement(translateX = 2f, translateY = 100f, scale = 0.0875f), marginWidth = 41.14f)
                ),
                elements = listOf(
                    IconElement(shape = variableCloudinessNightOutline1, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Butt, join = StrokeJoin.Round))
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