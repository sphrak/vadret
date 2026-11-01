package se.vadretapp.vadret.icons

import androidx.compose.ui.graphics.vector.PathNode

val VadretIcons.Symbol.Drop: VadretIconGraphic
    get() = drop

private val dropOutline1: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 265.73f, y = -188.25f),
        PathNode.QuadTo(x1 = 180f, y1 = -276.51f, x2 = 180f, y2 = -407.94f),
        PathNode.RelativeQuadTo(dx1 = 0f, dy1 = -59.44f, dx2 = 27.19f, dy2 = -118.85f),
        PathNode.RelativeQuadTo(dx1 = 27.2f, dy1 = -59.4f, dx2 = 67.89f, dy2 = -114.07f),
        PathNode.RelativeQuadTo(dx1 = 40.69f, dy1 = -54.68f, dx2 = 87.84f, dy2 = -102.46f),
        PathNode.RelativeQuadTo(dx1 = 47.16f, dy1 = -47.78f, dx2 = 87.85f, dy2 = -84.52f),
        PathNode.RelativeQuadTo(dx1 = 6.61f, dy1 = -5.62f, dx2 = 13.87f, dy2 = -8.5f),
        PathNode.RelativeQuadTo(dx1 = 7.26f, dy1 = -2.89f, dx2 = 15.36f, dy2 = -2.89f),
        PathNode.RelativeReflectiveQuadTo(dx = 15.36f, dy = 2.89f),
        PathNode.RelativeQuadTo(dx1 = 7.26f, dy1 = 2.88f, dx2 = 13.87f, dy2 = 8.5f),
        PathNode.RelativeQuadTo(dx1 = 40.69f, dy1 = 36.74f, dx2 = 87.85f, dy2 = 84.52f),
        PathNode.RelativeQuadTo(dx1 = 47.15f, dy1 = 47.78f, dx2 = 87.84f, dy2 = 102.46f),
        PathNode.RelativeQuadTo(dx1 = 40.69f, dy1 = 54.67f, dx2 = 67.89f, dy2 = 114.07f),
        PathNode.QuadTo(x1 = 780f, y1 = -467.38f, x2 = 780f, y2 = -407.94f),
        PathNode.RelativeQuadTo(dx1 = 0f, dy1 = 131.43f, dx2 = -85.8f, dy2 = 219.69f),
        PathNode.QuadTo(x1 = 608.4f, y1 = -100f, x2 = 479.93f, y2 = -100f),
        PathNode.RelativeQuadTo(dx1 = -128.47f, dy1 = 0f, dx2 = -214.2f, dy2 = -88.25f),
        PathNode.Close,
        PathNode.RelativeMoveTo(dx = 396.42f, dy = -32.1f),
        PathNode.RelativeQuadTo(dx1 = 72.46f, dy1 = -74.96f, dx2 = 72.46f, dy2 = -187.65f),
        PathNode.RelativeQuadTo(dx1 = 0f, dy1 = -77.46f, dx2 = -64.96f, dy2 = -175.65f),
        PathNode.RelativeQuadTo(dx1 = -64.96f, dy1 = -98.2f, dx2 = -189.65f, dy2 = -209.43f),
        PathNode.RelativeQuadTo(dx1 = -124.69f, dy1 = 111.23f, dx2 = -189.65f, dy2 = 209.43f),
        PathNode.RelativeQuadTo(dx1 = -64.96f, dy1 = 98.19f, dx2 = -64.96f, dy2 = 175.65f),
        PathNode.RelativeQuadTo(dx1 = 0f, dy1 = 112.69f, dx2 = 72.46f, dy2 = 187.65f),
        PathNode.RelativeQuadTo(dx1 = 72.46f, dy1 = 74.96f, dx2 = 182.15f, dy2 = 74.96f),
        PathNode.RelativeQuadTo(dx1 = 109.69f, dy1 = 0f, dx2 = 182.15f, dy2 = -74.96f),
        PathNode.Close,
        PathNode.MoveTo(x = 480f, y = -476.92f),
        PathNode.Close,
        PathNode.RelativeMoveTo(dx = 0.92f, dy = 265.07f),
        PathNode.RelativeQuadTo(dx1 = 13.31f, dy1 = -0.38f, dx2 = 20.27f, dy2 = -4.98f),
        PathNode.RelativeQuadTo(dx1 = 6.96f, dy1 = -4.59f, dx2 = 6.96f, dy2 = -13.62f),
        PathNode.RelativeQuadTo(dx1 = 0f, dy1 = -9.01f, dx2 = -7.07f, dy2 = -13.67f),
        PathNode.RelativeQuadTo(dx1 = -7.07f, dy1 = -4.65f, dx2 = -20.77f, dy2 = -4.65f),
        PathNode.RelativeQuadTo(dx1 = -41.62f, dy1 = 0.77f, dx2 = -88.19f, dy2 = -26.88f),
        PathNode.RelativeQuadTo(dx1 = -46.58f, dy1 = -27.66f, dx2 = -58.58f, dy2 = -94.66f),
        PathNode.RelativeQuadTo(dx1 = -1.62f, dy1 = -9f, dx2 = -7.27f, dy2 = -13.73f),
        PathNode.RelativeQuadTo(dx1 = -5.65f, dy1 = -4.73f, dx2 = -12.5f, dy2 = -4.73f),
        PathNode.RelativeQuadTo(dx1 = -8.69f, dy1 = 0f, dx2 = -13.73f, dy2 = 6.77f),
        PathNode.RelativeQuadTo(dx1 = -5.04f, dy1 = 6.77f, dx2 = -3.42f, dy2 = 15.77f),
        PathNode.RelativeQuadTo(dx1 = 15f, dy1 = 82.46f, dx2 = 72.15f, dy2 = 118.61f),
        PathNode.RelativeQuadTo(dx1 = 57.15f, dy1 = 36.16f, dx2 = 112.15f, dy2 = 35.77f),
        PathNode.Close
    )
)

private val drop: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "Drop",
        layers = listOf(
            IconLayer.Plain(
                elements = listOf(
                    IconElement(shape = dropOutline1, paint = IconPaint.Fill, placement = IconPlacement(translateX = 5f, translateY = 95f, scale = 0.09375f))
                )
            )
        )
    )
}