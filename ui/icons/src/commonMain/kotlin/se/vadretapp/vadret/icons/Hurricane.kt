package se.vadretapp.vadret.icons

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathNode

val VadretIcons.Alert.Hurricane: VadretIconGraphic
    get() = hurricane

private val hurricaneOutline1: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 20f, y = 30f),
        PathNode.HorizontalTo(x = 58f),
        PathNode.ArcTo(horizontalEllipseRadius = 7f, verticalEllipseRadius = 7f, theta = 0f, isMoreThanHalf = true, isPositiveArc = false, arcStartX = 51f, arcStartY = 23f)
    )
)

private val hurricaneOutline2: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 14f, y = 43f),
        PathNode.HorizontalTo(x = 78f)
    )
)

private val hurricaneOutline3: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 24f, y = 57f),
        PathNode.HorizontalTo(x = 72f)
    )
)

private val hurricaneOutline4: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 20f, y = 70f),
        PathNode.HorizontalTo(x = 62f),
        PathNode.ArcTo(horizontalEllipseRadius = 7f, verticalEllipseRadius = 7f, theta = 0f, isMoreThanHalf = true, isPositiveArc = true, arcStartX = 55f, arcStartY = 77f)
    )
)

private val hurricane: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "Hurricane",
        layers = listOf(
            IconLayer.Plain(
                elements = listOf(
                    IconElement(shape = hurricaneOutline1, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round), placement = IconPlacement(translateX = -10f, translateY = -10f, scale = 1.2f)),
                    IconElement(shape = hurricaneOutline2, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round), placement = IconPlacement(translateX = -10f, translateY = -10f, scale = 1.2f)),
                    IconElement(shape = hurricaneOutline3, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round), placement = IconPlacement(translateX = -10f, translateY = -10f, scale = 1.2f)),
                    IconElement(shape = hurricaneOutline4, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round), placement = IconPlacement(translateX = -10f, translateY = -10f, scale = 1.2f))
                )
            )
        )
    )
}