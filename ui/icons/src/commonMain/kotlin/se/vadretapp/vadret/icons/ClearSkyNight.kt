package se.vadretapp.vadret.icons

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathNode

val VadretIcons.Weather.ClearSkyNight: VadretIconGraphic
    get() = clearSkyNight

private val clearSkyNightOutline1: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 74.49f, y = 58.74f),
        PathNode.ArcTo(horizontalEllipseRadius = 26f, verticalEllipseRadius = 26f, theta = 0f, isMoreThanHalf = true, isPositiveArc = true, arcStartX = 49.78f, arcStartY = 24f),
        PathNode.ArcTo(horizontalEllipseRadius = 21.32f, verticalEllipseRadius = 21.32f, theta = 0f, isMoreThanHalf = true, isPositiveArc = false, arcStartX = 74.49f, arcStartY = 58.74f),
        PathNode.Close
    )
)

private val clearSkyNight: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "ClearSkyNight",
        layers = listOf(
            IconLayer.Plain(
                elements = listOf(
                    IconElement(shape = clearSkyNightOutline1, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Butt, join = StrokeJoin.Round))
                )
            )
        )
    )
}