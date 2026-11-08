package se.vadretapp.vadret.icons

import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathNode

val VadretIcons.Alert.Slipperiness: VadretIconGraphic
    get() = slipperiness

private val slipperinessOutline1: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 43.95f, y = 23.39f),
        PathNode.ArcTo(horizontalEllipseRadius = 7f, verticalEllipseRadius = 7f, theta = 0f, isMoreThanHalf = false, isPositiveArc = true, arcStartX = 56.05f, arcStartY = 23.39f),
        PathNode.LineTo(x = 82.88f, y = 69.48f),
        PathNode.ArcTo(horizontalEllipseRadius = 7f, verticalEllipseRadius = 7f, theta = 0f, isMoreThanHalf = false, isPositiveArc = true, arcStartX = 76.83f, arcStartY = 80f),
        PathNode.LineTo(x = 23.17f, y = 80f),
        PathNode.ArcTo(horizontalEllipseRadius = 7f, verticalEllipseRadius = 7f, theta = 0f, isMoreThanHalf = false, isPositiveArc = true, arcStartX = 17.12f, arcStartY = 69.48f),
        PathNode.Close
    )
)

private val slipperinessOutline2: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 46.94f, y = 42.33f),
        PathNode.LineTo(x = 47.79f, y = 42.14f),
        PathNode.LineTo(x = 49.9f, y = 42.28f),
        PathNode.LineTo(x = 54.14f, y = 43.24f),
        PathNode.LineTo(x = 58.13f, y = 45.03f),
        PathNode.LineTo(x = 59f, y = 45.99f),
        PathNode.LineTo(x = 60.08f, y = 50.99f),
        PathNode.LineTo(x = 60.78f, y = 52.53f),
        PathNode.LineTo(x = 60.92f, y = 54.46f),
        PathNode.LineTo(x = 60.54f, y = 55.81f),
        PathNode.LineTo(x = 58.95f, y = 58.5f),
        PathNode.LineTo(x = 58.21f, y = 61f),
        PathNode.LineTo(x = 57.8f, y = 61.62f),
        PathNode.LineTo(x = 57.22f, y = 62f),
        PathNode.LineTo(x = 56.26f, y = 62.13f),
        PathNode.LineTo(x = 55.68f, y = 62.01f),
        PathNode.LineTo(x = 55.02f, y = 61.58f),
        PathNode.LineTo(x = 54.47f, y = 60.43f),
        PathNode.LineTo(x = 54.81f, y = 58.5f),
        PathNode.LineTo(x = 54.14f, y = 58.21f),
        PathNode.LineTo(x = 43.74f, y = 55.28f),
        PathNode.LineTo(x = 43.53f, y = 55.42f),
        PathNode.LineTo(x = 43.02f, y = 56.96f),
        PathNode.LineTo(x = 41.96f, y = 57.73f),
        PathNode.LineTo(x = 40.86f, y = 57.77f),
        PathNode.LineTo(x = 40.3f, y = 57.54f),
        PathNode.LineTo(x = 39.7f, y = 56.93f),
        PathNode.LineTo(x = 39.43f, y = 55.61f),
        PathNode.LineTo(x = 40.17f, y = 52.92f),
        PathNode.LineTo(x = 40.28f, y = 49.65f),
        PathNode.LineTo(x = 40.78f, y = 48.3f),
        PathNode.LineTo(x = 41.67f, y = 47.14f),
        PathNode.LineTo(x = 43.07f, y = 46.18f),
        PathNode.LineTo(x = 46.18f, y = 42.91f),
        PathNode.Close,
        PathNode.MoveTo(x = 47.7f, y = 44.26f),
        PathNode.LineTo(x = 49.13f, y = 44.19f),
        PathNode.LineTo(x = 52.41f, y = 44.8f),
        PathNode.LineTo(x = 55.1f, y = 45.76f),
        PathNode.LineTo(x = 57.03f, y = 46.74f),
        PathNode.LineTo(x = 57.47f, y = 48.11f),
        PathNode.LineTo(x = 57.81f, y = 50.42f),
        PathNode.LineTo(x = 45.48f, y = 46.87f),
        PathNode.LineTo(x = 45.35f, y = 46.76f),
        PathNode.LineTo(x = 45.46f, y = 46.57f),
        PathNode.Close,
        PathNode.MoveTo(x = 40.05f, y = 62.54f),
        PathNode.LineTo(x = 41.05f, y = 62.45f),
        PathNode.LineTo(x = 41.21f, y = 62.74f),
        PathNode.LineTo(x = 41.05f, y = 63.01f),
        PathNode.LineTo(x = 35.08f, y = 65.18f),
        PathNode.LineTo(x = 34.61f, y = 65.82f),
        PathNode.LineTo(x = 34.82f, y = 66.59f),
        PathNode.LineTo(x = 35.66f, y = 67.01f),
        PathNode.LineTo(x = 46.99f, y = 70.63f),
        PathNode.LineTo(x = 47.51f, y = 71.21f),
        PathNode.LineTo(x = 47.4f, y = 72.07f),
        PathNode.LineTo(x = 47.02f, y = 72.44f),
        PathNode.LineTo(x = 40.47f, y = 75.88f),
        PathNode.LineTo(x = 29.23f, y = 75.83f),
        PathNode.LineTo(x = 40.09f, y = 72.01f),
        PathNode.LineTo(x = 40.47f, y = 71.85f),
        PathNode.LineTo(x = 40.61f, y = 71.59f),
        PathNode.LineTo(x = 40.28f, y = 71.16f),
        PathNode.LineTo(x = 29.11f, y = 67.45f),
        PathNode.LineTo(x = 28.28f, y = 66.78f),
        PathNode.LineTo(x = 28.08f, y = 65.82f),
        PathNode.LineTo(x = 28.34f, y = 65.12f),
        PathNode.LineTo(x = 29.31f, y = 64.4f),
        PathNode.Close,
        PathNode.MoveTo(x = 54.47f, y = 62.93f),
        PathNode.LineTo(x = 55.49f, y = 62.82f),
        PathNode.LineTo(x = 55.63f, y = 63.12f),
        PathNode.LineTo(x = 55.49f, y = 63.36f),
        PathNode.LineTo(x = 49.33f, y = 65.34f),
        PathNode.LineTo(x = 49.33f, y = 65.68f),
        PathNode.LineTo(x = 73.77f, y = 68.46f),
        PathNode.LineTo(x = 74.92f, y = 68.9f),
        PathNode.LineTo(x = 75.78f, y = 69.86f),
        PathNode.LineTo(x = 76.01f, y = 71.21f),
        PathNode.LineTo(x = 75.72f, y = 72.36f),
        PathNode.LineTo(x = 72.62f, y = 75.85f),
        PathNode.LineTo(x = 60.11f, y = 75.85f),
        PathNode.LineTo(x = 68.96f, y = 72.36f),
        PathNode.LineTo(x = 69.35f, y = 71.59f),
        PathNode.LineTo(x = 68.91f, y = 70.82f),
        PathNode.LineTo(x = 42.78f, y = 66.27f),
        PathNode.LineTo(x = 42.12f, y = 65.82f),
        PathNode.LineTo(x = 42.4f, y = 65.18f),
        PathNode.Close
    ),
    fillType = PathFillType.EvenOdd
)

private val slipperiness: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "Slipperiness",
        layers = listOf(
            IconLayer.Plain(
                elements = listOf(
                    IconElement(shape = slipperinessOutline1, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round), placement = IconPlacement(translateX = -10f, translateY = -10f, scale = 1.2f)),
                    IconElement(shape = slipperinessOutline2, paint = IconPaint.Fill, placement = IconPlacement(translateX = -10f, translateY = -10f, scale = 1.2f))
                )
            )
        )
    )
}