package se.vadretapp.vadret.icons

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathNode

val VadretIcons.Alert.HardWind: VadretIconGraphic
    get() = hardWind

private val hardWindOutline1: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 20f, y = 42f),
        PathNode.HorizontalTo(x = 60f),
        PathNode.ArcTo(horizontalEllipseRadius = 9f, verticalEllipseRadius = 9f, theta = 0f, isMoreThanHalf = true, isPositiveArc = false, arcStartX = 51f, arcStartY = 33f)
    )
)

private val hardWindOutline2: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 20f, y = 58f),
        PathNode.HorizontalTo(x = 66f),
        PathNode.ArcTo(horizontalEllipseRadius = 9f, verticalEllipseRadius = 9f, theta = 0f, isMoreThanHalf = true, isPositiveArc = true, arcStartX = 57f, arcStartY = 67f)
    )
)

private val hardWind: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "HardWind",
        layers = listOf(
            IconLayer.Plain(
                elements = listOf(
                    IconElement(shape = hardWindOutline1, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round), placement = IconPlacement(translateX = -10f, translateY = -10f, scale = 1.2f)),
                    IconElement(shape = hardWindOutline2, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round), placement = IconPlacement(translateX = -10f, translateY = -10f, scale = 1.2f))
                )
            )
        )
    )
}