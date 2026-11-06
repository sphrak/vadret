package se.vadretapp.vadret.icons

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathNode

val VadretIcons.Alert.Warning: VadretIconGraphic
    get() = warning

private val warningOutline1: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 26f, y = 42f),
        PathNode.HorizontalTo(x = 38f),
        PathNode.LineTo(x = 62f, y = 28f),
        PathNode.VerticalTo(y = 72f),
        PathNode.LineTo(x = 38f, y = 58f),
        PathNode.HorizontalTo(x = 26f),
        PathNode.ArcTo(horizontalEllipseRadius = 4f, verticalEllipseRadius = 4f, theta = 0f, isMoreThanHalf = false, isPositiveArc = true, arcStartX = 22f, arcStartY = 54f),
        PathNode.VerticalTo(y = 46f),
        PathNode.ArcTo(horizontalEllipseRadius = 4f, verticalEllipseRadius = 4f, theta = 0f, isMoreThanHalf = false, isPositiveArc = true, arcStartX = 26f, arcStartY = 42f),
        PathNode.Close
    )
)

private val warningOutline2: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 31f, y = 58f),
        PathNode.LineTo(x = 35f, y = 74f)
    )
)

private val warningOutline3: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 71f, y = 40f),
        PathNode.LineTo(x = 78f, y = 36f),
        PathNode.MoveTo(x = 72f, y = 50f),
        PathNode.HorizontalTo(x = 80f),
        PathNode.MoveTo(x = 71f, y = 60f),
        PathNode.LineTo(x = 78f, y = 64f)
    )
)

private val warning: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "Warning",
        layers = listOf(
            IconLayer.Plain(
                elements = listOf(
                    IconElement(shape = warningOutline1, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round), placement = IconPlacement(translateX = -10f, translateY = -10f, scale = 1.2f)),
                    IconElement(shape = warningOutline2, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round), placement = IconPlacement(translateX = -10f, translateY = -10f, scale = 1.2f)),
                    IconElement(shape = warningOutline3, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round), placement = IconPlacement(translateX = -10f, translateY = -10f, scale = 1.2f))
                )
            )
        )
    )
}