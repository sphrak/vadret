package se.vadretapp.vadret.icons

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathNode

val VadretIcons.Symbol.Moon: VadretIconGraphic
    get() = moon

private val moonOutline1: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 76.37f, y = 59.41f),
        PathNode.ArcTo(horizontalEllipseRadius = 28f, verticalEllipseRadius = 28f, theta = 0f, isMoreThanHalf = true, isPositiveArc = true, arcStartX = 49.77f, arcStartY = 22f),
        PathNode.ArcTo(horizontalEllipseRadius = 22.96f, verticalEllipseRadius = 22.96f, theta = 0f, isMoreThanHalf = true, isPositiveArc = false, arcStartX = 76.37f, arcStartY = 59.41f),
        PathNode.Close
    )
)

private val moon: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "Moon",
        layers = listOf(
            IconLayer.Plain(
                elements = listOf(
                    IconElement(shape = moonOutline1, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Butt, join = StrokeJoin.Round))
                )
            )
        )
    )
}