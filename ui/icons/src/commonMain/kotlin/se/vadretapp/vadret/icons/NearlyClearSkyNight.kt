package se.vadretapp.vadret.icons

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathNode

val VadretIcons.Weather.NearlyClearSkyNight: VadretIconGraphic
    get() = nearlyClearSkyNight

private val nearlyClearSkyNightOutline1: IconShape.Outline = IconShape.Outline(
    nodes = listOf(
        PathNode.MoveTo(x = 59.83f, y = 14.55f),
        PathNode.ArcTo(horizontalEllipseRadius = 16f, verticalEllipseRadius = 16f, theta = 0f, isMoreThanHalf = true, isPositiveArc = true, arcStartX = 52.02f, arcStartY = 40.6f),
        PathNode.ArcTo(horizontalEllipseRadius = 13.6f, verticalEllipseRadius = 13.6f, theta = 0f, isMoreThanHalf = true, isPositiveArc = false, arcStartX = 59.83f, arcStartY = 14.55f),
        PathNode.Close
    )
)

private val nearlyClearSkyNight: VadretIconGraphic by lazy {
    VadretIconGraphic(
        name = "NearlyClearSkyNight",
        layers = listOf(
            IconLayer.Masked(
                cutouts = listOf(
                    IconCutout(shape = cloudSilhouette, placement = IconPlacement(translateX = 6f, translateY = 102f, scale = 0.085417f), marginWidth = 42.15f)
                ),
                elements = listOf(
                    IconElement(shape = nearlyClearSkyNightOutline1, paint = IconPaint.Stroke(width = 4.5f, cap = StrokeCap.Butt, join = StrokeJoin.Round))
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