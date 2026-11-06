package se.vadretapp.vadret.icons

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathNode

val VadretIcons.Alert.Warmer: VadretIconGraphic
    get() = warmer

private val warmerOutline1: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 31f, y = 58f),
        PathNode.VerticalTo(y = 29f),
        PathNode.ArcTo(horizontalEllipseRadius = 7f, verticalEllipseRadius = 7f, theta = 0f, isMoreThanHalf = false, isPositiveArc = true, arcStartX = 45f, arcStartY = 29f),
        PathNode.VerticalTo(y = 58f),
        PathNode.ArcTo(horizontalEllipseRadius = 12f, verticalEllipseRadius = 12f, theta = 0f, isMoreThanHalf = true, isPositiveArc = true, arcStartX = 31f, arcStartY = 58f),
        PathNode.Close
    )
)

private val warmerOutline2: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 38f, y = 67.75f),
        PathNode.VerticalTo(y = 32f)
    )
)

private val warmerOutline3: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 66f, y = 58f),
        PathNode.VerticalTo(y = 24f),
        PathNode.MoveTo(x = 57f, y = 33f),
        PathNode.LineTo(x = 66f, y = 24f),
        PathNode.LineTo(x = 75f, y = 33f)
    )
)

private val warmerOutline4: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 32.5f, y = 67.75f),
        PathNode.ArcTo(horizontalEllipseRadius = 5.5f, verticalEllipseRadius = 5.5f, theta = 0f, isMoreThanHalf = true, isPositiveArc = false, arcStartX = 43.5f, arcStartY = 67.75f),
        PathNode.ArcTo(horizontalEllipseRadius = 5.5f, verticalEllipseRadius = 5.5f, theta = 0f, isMoreThanHalf = true, isPositiveArc = false, arcStartX = 32.5f, arcStartY = 67.75f),
        PathNode.Close
    )
)

private val warmer: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "Warmer",
        layers = listOf(
            IconLayer.Plain(
                elements = listOf(
                    IconElement(shape = warmerOutline1, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round), placement = IconPlacement(translateX = -10f, translateY = -10f, scale = 1.2f)),
                    IconElement(shape = warmerOutline2, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round), placement = IconPlacement(translateX = -10f, translateY = -10f, scale = 1.2f)),
                    IconElement(shape = warmerOutline3, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round), placement = IconPlacement(translateX = -10f, translateY = -10f, scale = 1.2f)),
                    IconElement(shape = warmerOutline4, paint = IconPaint.Fill, placement = IconPlacement(translateX = -10f, translateY = -10f, scale = 1.2f))
                )
            )
        )
    )
}