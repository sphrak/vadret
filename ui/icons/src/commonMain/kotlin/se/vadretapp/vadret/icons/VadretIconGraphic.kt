package se.vadretapp.vadret.icons

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathNode

@Immutable
class VadretIconGraphic internal constructor(
    internal val name: String,
    internal val layers: List<IconLayer>
) {

    internal val hasMotion: Boolean = layers.any { iconLayer: IconLayer ->
        iconLayer.elements.any { iconElement: IconElement -> iconElement.motion != null }
    }
}

internal sealed interface IconLayer {

    val elements: List<IconElement>

    class Plain(
        override val elements: List<IconElement>
    ) : IconLayer

    class Masked(
        val cutouts: List<IconCutout>,
        override val elements: List<IconElement>
    ) : IconLayer
}

internal class IconElement(
    val shape: IconShape,
    val paint: IconPaint,
    val placement: IconPlacement = IconPlacement(),
    val motion: IconMotion? = null
)

internal class IconCutout(
    val shape: IconShape.Outline,
    val placement: IconPlacement,
    val marginWidth: Float?
)

internal class IconPlacement(
    val translateX: Float = 0f,
    val translateY: Float = 0f,
    val scale: Float = 1f
)

internal sealed interface IconShape {

    class Outline(
        val nodes: List<PathNode>,
        val fillType: PathFillType = PathFillType.NonZero
    ) : IconShape

    class Circle(
        val centerX: Float,
        val centerY: Float,
        val radius: Float
    ) : IconShape
}

internal sealed interface IconPaint {

    data object Fill : IconPaint

    class Stroke(
        val width: Float,
        val cap: StrokeCap,
        val join: StrokeJoin
    ) : IconPaint
}