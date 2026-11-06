package se.vadretapp.vadret.icons

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathNode

val VadretIcons.Alert.Pollen: VadretIconGraphic
    get() = pollen

private val pollenOutline1: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 41.09f, y = 25.74f),
        PathNode.ArcTo(horizontalEllipseRadius = 9f, verticalEllipseRadius = 9f, theta = 0f, isMoreThanHalf = true, isPositiveArc = true, arcStartX = 58.91f, arcStartY = 25.74f),
        PathNode.ArcTo(horizontalEllipseRadius = 9f, verticalEllipseRadius = 9f, theta = 0f, isMoreThanHalf = true, isPositiveArc = true, arcStartX = 64.42f, arcStartY = 42.68f),
        PathNode.ArcTo(horizontalEllipseRadius = 9f, verticalEllipseRadius = 9f, theta = 0f, isMoreThanHalf = true, isPositiveArc = true, arcStartX = 50f, arcStartY = 53.16f),
        PathNode.ArcTo(horizontalEllipseRadius = 9f, verticalEllipseRadius = 9f, theta = 0f, isMoreThanHalf = true, isPositiveArc = true, arcStartX = 35.58f, arcStartY = 42.68f),
        PathNode.ArcTo(horizontalEllipseRadius = 9f, verticalEllipseRadius = 9f, theta = 0f, isMoreThanHalf = true, isPositiveArc = true, arcStartX = 41.09f, arcStartY = 25.74f),
        PathNode.Close
    )
)

private val pollenOutline2: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 50f, y = 60f),
        PathNode.VerticalTo(y = 82f)
    )
)

private val pollenOutline3: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 50f, y = 76f),
        PathNode.CurveTo(x1 = 50f, y1 = 68f, x2 = 58f, y2 = 64f, x3 = 66f, y3 = 64f),
        PathNode.CurveTo(x1 = 66f, y1 = 72f, x2 = 58f, y2 = 76f, x3 = 50f, y3 = 76f),
        PathNode.Close
    )
)

private val pollenOutline4: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 45f, y = 38f),
        PathNode.ArcTo(horizontalEllipseRadius = 5f, verticalEllipseRadius = 5f, theta = 0f, isMoreThanHalf = true, isPositiveArc = false, arcStartX = 55f, arcStartY = 38f),
        PathNode.ArcTo(horizontalEllipseRadius = 5f, verticalEllipseRadius = 5f, theta = 0f, isMoreThanHalf = true, isPositiveArc = false, arcStartX = 45f, arcStartY = 38f),
        PathNode.Close
    )
)

private val pollen: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "Pollen",
        layers = listOf(
            IconLayer.Plain(
                elements = listOf(
                    IconElement(shape = pollenOutline1, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round), placement = IconPlacement(translateX = -10f, translateY = -10f, scale = 1.2f)),
                    IconElement(shape = pollenOutline2, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round), placement = IconPlacement(translateX = -10f, translateY = -10f, scale = 1.2f)),
                    IconElement(shape = pollenOutline3, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round), placement = IconPlacement(translateX = -10f, translateY = -10f, scale = 1.2f)),
                    IconElement(shape = pollenOutline4, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round), placement = IconPlacement(translateX = -10f, translateY = -10f, scale = 1.2f))
                )
            )
        )
    )
}