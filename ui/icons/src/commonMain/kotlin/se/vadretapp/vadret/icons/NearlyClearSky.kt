package se.vadretapp.vadret.icons

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathNode

val VadretIcons.Weather.NearlyClearSky: VadretIconGraphic
    get() = nearlyClearSky

private val nearlyClearSky: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "NearlyClearSky",
        layers = listOf(
            IconLayer.Masked(
                cutouts = listOf(
                    IconCutout(shape = cloudSilhouette, placement = IconPlacement(translateX = 6f, translateY = 102f, scale = 0.085417f), marginWidth = 42.15f)
                ),
                elements = listOf(
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 84f, y = 32f), PathNode.LineTo(x = 90f, y = 32f))), paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Miter)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 78.14f, y = 46.14f), PathNode.LineTo(x = 82.38f, y = 50.38f))), paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Miter)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 64f, y = 52f), PathNode.LineTo(x = 64f, y = 58f))), paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Miter)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 49.86f, y = 46.14f), PathNode.LineTo(x = 45.62f, y = 50.38f))), paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Miter)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 44f, y = 32f), PathNode.LineTo(x = 38f, y = 32f))), paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Miter)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 49.86f, y = 17.86f), PathNode.LineTo(x = 45.62f, y = 13.62f))), paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Miter)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 64f, y = 12f), PathNode.LineTo(x = 64f, y = 6f))), paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Miter)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 78.14f, y = 17.86f), PathNode.LineTo(x = 82.38f, y = 13.62f))), paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Miter)),
                    IconElement(shape = IconShape.Circle(centerX = 64f, centerY = 32f, radius = 13f), paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Butt, join = StrokeJoin.Miter))
                )
            ),
            IconLayer.Plain(
                elements = listOf(
                    IconElement(shape = cloudOutline, paint = IconPaint.Fill, placement = IconPlacement(translateX = 6f, translateY = 102f, scale = 0.085417f))
                )
            )
        )
    )
}