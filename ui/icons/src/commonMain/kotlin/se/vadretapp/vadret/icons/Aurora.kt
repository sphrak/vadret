package se.vadretapp.vadret.icons

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathNode

val VadretIcons.Alert.Aurora: VadretIconGraphic
    get() = aurora

private val auroraOutline1: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 20f, y = 40f),
        PathNode.CurveTo(x1 = 30f, y1 = 28f, x2 = 40f, y2 = 28f, x3 = 50f, y3 = 38f),
        PathNode.CurveTo(x1 = 60f, y1 = 48f, x2 = 70f, y2 = 48f, x3 = 80f, y3 = 36f)
    )
)

private val auroraOutline2: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 30f, y = 32f),
        PathNode.VerticalTo(y = 50f)
    )
)

private val auroraOutline3: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 50f, y = 38f),
        PathNode.VerticalTo(y = 52f)
    )
)

private val auroraOutline4: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 70f, y = 44f),
        PathNode.VerticalTo(y = 54f)
    )
)

private val auroraOutline5: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 16f, y = 76f),
        PathNode.LineTo(x = 32f, y = 64f),
        PathNode.LineTo(x = 42f, y = 70f),
        PathNode.LineTo(x = 56f, y = 60f),
        PathNode.LineTo(x = 84f, y = 76f)
    )
)

private val aurora: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "Aurora",
        layers = listOf(
            IconLayer.Plain(
                elements = listOf(
                    IconElement(shape = auroraOutline1, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round), placement = IconPlacement(translateX = -10f, translateY = -10f, scale = 1.2f)),
                    IconElement(shape = auroraOutline2, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round), placement = IconPlacement(translateX = -10f, translateY = -10f, scale = 1.2f)),
                    IconElement(shape = auroraOutline3, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round), placement = IconPlacement(translateX = -10f, translateY = -10f, scale = 1.2f)),
                    IconElement(shape = auroraOutline4, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round), placement = IconPlacement(translateX = -10f, translateY = -10f, scale = 1.2f)),
                    IconElement(shape = auroraOutline5, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round), placement = IconPlacement(translateX = -10f, translateY = -10f, scale = 1.2f))
                )
            )
        )
    )
}