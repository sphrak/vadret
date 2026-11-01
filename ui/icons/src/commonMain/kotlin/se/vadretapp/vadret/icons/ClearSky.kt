package se.vadretapp.vadret.icons

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathNode

val VadretIcons.Weather.ClearSky: VadretIconGraphic
    get() = clearSky

private val clearSky: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "ClearSky",
        layers = listOf(
            IconLayer.Plain(
                elements = listOf(
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 74f, y = 50f), PathNode.LineTo(x = 80f, y = 50f))), paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Miter)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 66.97f, y = 66.97f), PathNode.LineTo(x = 71.21f, y = 71.21f))), paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Miter)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 50f, y = 74f), PathNode.LineTo(x = 50f, y = 80f))), paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Miter)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 33.03f, y = 66.97f), PathNode.LineTo(x = 28.79f, y = 71.21f))), paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Miter)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 26f, y = 50f), PathNode.LineTo(x = 20f, y = 50f))), paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Miter)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 33.03f, y = 33.03f), PathNode.LineTo(x = 28.79f, y = 28.79f))), paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Miter)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 50f, y = 26f), PathNode.LineTo(x = 50f, y = 20f))), paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Miter)),
                    IconElement(shape = IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = 66.97f, y = 33.03f), PathNode.LineTo(x = 71.21f, y = 28.79f))), paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Miter)),
                    IconElement(shape = IconShape.Circle(centerX = 50f, centerY = 50f, radius = 17f), paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Butt, join = StrokeJoin.Miter))
                )
            )
        )
    )
}