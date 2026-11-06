package se.vadretapp.vadret.icons

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathNode

val VadretIcons.Alert.Storm: VadretIconGraphic
    get() = storm

private val stormOutline1: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 20f, y = 38f),
        PathNode.HorizontalTo(x = 60f),
        PathNode.ArcTo(horizontalEllipseRadius = 9f, verticalEllipseRadius = 9f, theta = 0f, isMoreThanHalf = true, isPositiveArc = false, arcStartX = 51f, arcStartY = 29f)
    )
)

private val stormOutline2: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 14f, y = 50f),
        PathNode.HorizontalTo(x = 78f)
    )
)

private val stormOutline3: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 20f, y = 62f),
        PathNode.HorizontalTo(x = 66f),
        PathNode.ArcTo(horizontalEllipseRadius = 9f, verticalEllipseRadius = 9f, theta = 0f, isMoreThanHalf = true, isPositiveArc = true, arcStartX = 57f, arcStartY = 71f)
    )
)

private val storm: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "Storm",
        layers = listOf(
            IconLayer.Plain(
                elements = listOf(
                    IconElement(shape = stormOutline1, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round), placement = IconPlacement(translateX = -10f, translateY = -10f, scale = 1.2f)),
                    IconElement(shape = stormOutline2, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round), placement = IconPlacement(translateX = -10f, translateY = -10f, scale = 1.2f)),
                    IconElement(shape = stormOutline3, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round), placement = IconPlacement(translateX = -10f, translateY = -10f, scale = 1.2f))
                )
            )
        )
    )
}