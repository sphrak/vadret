package se.vadretapp.vadret.icons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.LongState
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private const val VIEWPORT_SIZE = 100f
private const val MASK_START = -20f
private const val MASK_END = 120f
private const val NO_TIME = 0L
private const val FRAME_MILLISECONDS = 16L
private const val LONGEST_PHASE_MILLISECONDS = 24_000

private val defaultIconSize = 24.dp
private val maskBounds = Rect(
    left = MASK_START,
    top = MASK_START,
    right = MASK_END,
    bottom = MASK_END
)
private val previewIconSize = 48.dp
private val previewSpacing = 8.dp
private val lightPreviewInk = Color(color = 0xFF4A4F5E)
private val darkPreviewInk = Color(color = 0xFFC4C8D4)
private val lightPreviewBackground = Color(color = 0xFFF3F4F8)
private val darkPreviewBackground = Color(color = 0xFF1C1E25)

@Composable
fun VadretIcon(
    icon: VadretIconGraphic,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier,
    isAnimated: Boolean = true
) {
    val isMotionShown: Boolean = isAnimated && icon.hasMotion && rememberIsMotionEnabled()
    val clock: LongState = rememberIconClock(isRunning = isMotionShown)
    val phaseMilliseconds: Int = remember { Random.nextInt(until = LONGEST_PHASE_MILLISECONDS) }
    val semanticsModifier: Modifier = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    } else {
        Modifier
    }
    Spacer(
        modifier = modifier
            .size(size = defaultIconSize)
            .then(other = semanticsModifier)
            .graphicsLayer()
            .drawWithCache {
                val outlines: Map<IconShape.Outline, Path> = icon.outlines()
                onDrawBehind {
                    val timeMilliseconds: Long = if (isMotionShown) {
                        clock.longValue + phaseMilliseconds
                    } else {
                        NO_TIME
                    }
                    scale(
                        scale = size.minDimension / VIEWPORT_SIZE,
                        pivot = Offset.Zero
                    ) {
                        icon.layers.forEach { iconLayer: IconLayer ->
                            drawLayer(
                                iconLayer = iconLayer,
                                outlines = outlines,
                                tint = tint,
                                timeMilliseconds = timeMilliseconds,
                                isMotionShown = isMotionShown
                            )
                        }
                    }
                }
            }
    )
}

@Composable
private fun rememberIconClock(isRunning: Boolean): LongState {
    val frameMilliseconds: MutableLongState = remember { mutableLongStateOf(value = NO_TIME) }
    LaunchedEffect(isRunning) {
        while (isRunning) {
            withFrameMillis { millis: Long ->
                if (millis - frameMilliseconds.longValue >= FRAME_MILLISECONDS) {
                    frameMilliseconds.longValue = millis
                }
            }
        }
    }
    return frameMilliseconds
}

private fun VadretIconGraphic.outlines(): Map<IconShape.Outline, Path> {
    val shapes: List<IconShape> = layers.flatMap { iconLayer: IconLayer ->
        iconLayer.elements.map { iconElement: IconElement -> iconElement.shape } +
            if (iconLayer is IconLayer.Masked) {
                iconLayer.cutouts.map { iconCutout: IconCutout -> iconCutout.shape }
            } else {
                emptyList()
            }
    }
    return shapes
        .filterIsInstance<IconShape.Outline>()
        .distinct()
        .associateWith { outline: IconShape.Outline -> outline.toPath() }
}

private fun IconShape.Outline.toPath(): Path {
    val path = Path()
    path.fillType = fillType
    return nodes.toPath(target = path)
}

private fun DrawScope.drawLayer(
    iconLayer: IconLayer,
    outlines: Map<IconShape.Outline, Path>,
    tint: Color,
    timeMilliseconds: Long,
    isMotionShown: Boolean
) {
    if (iconLayer is IconLayer.Masked) {
        drawIntoCanvas { canvas -> canvas.saveLayer(bounds = maskBounds, paint = Paint()) }
    }
    iconLayer.elements.forEach { iconElement: IconElement ->
        drawElement(
            iconElement = iconElement,
            outlines = outlines,
            tint = tint,
            pose = iconElement.poseAt(
                timeMilliseconds = timeMilliseconds,
                isMotionShown = isMotionShown
            )
        )
    }
    if (iconLayer is IconLayer.Masked) {
        iconLayer.cutouts.forEach { iconCutout: IconCutout ->
            cutOut(
                iconCutout = iconCutout,
                outline = outlines.getValue(key = iconCutout.shape)
            )
        }
        drawIntoCanvas { canvas -> canvas.restore() }
    }
}

private fun IconElement.poseAt(
    timeMilliseconds: Long,
    isMotionShown: Boolean
): IconPose {
    val elementMotion: IconMotion? = motion
    return if (isMotionShown && elementMotion != null) {
        elementMotion.poseAt(timeMilliseconds = timeMilliseconds)
    } else {
        restingPose
    }
}

private fun DrawScope.drawElement(
    iconElement: IconElement,
    outlines: Map<IconShape.Outline, Path>,
    tint: Color,
    pose: IconPose
) {
    withTransform({
        translate(
            left = pose.offsetX,
            top = pose.offsetY
        )
        translate(
            left = iconElement.placement.translateX,
            top = iconElement.placement.translateY
        )
        scale(
            scale = iconElement.placement.scale,
            pivot = Offset.Zero
        )
    }) {
        drawShape(
            shape = iconElement.shape,
            paint = iconElement.paint,
            outlines = outlines,
            color = tint.copy(alpha = tint.alpha * pose.alpha)
        )
    }
}

private fun DrawScope.drawShape(
    shape: IconShape,
    paint: IconPaint,
    outlines: Map<IconShape.Outline, Path>,
    color: Color
) {
    val style = when (paint) {
        IconPaint.Fill -> Fill
        is IconPaint.Stroke -> Stroke(
            width = paint.width,
            cap = paint.cap,
            join = paint.join
        )
    }
    when (shape) {
        is IconShape.Outline -> drawPath(
            path = outlines.getValue(key = shape),
            color = color,
            style = style
        )
        is IconShape.Circle -> drawCircle(
            color = color,
            radius = shape.radius,
            center = Offset(
                x = shape.centerX,
                y = shape.centerY
            ),
            style = style
        )
    }
}

private fun DrawScope.cutOut(
    iconCutout: IconCutout,
    outline: Path
) {
    withTransform({
        translate(
            left = iconCutout.placement.translateX,
            top = iconCutout.placement.translateY
        )
        scale(
            scale = iconCutout.placement.scale,
            pivot = Offset.Zero
        )
    }) {
        drawPath(
            path = outline,
            color = Color.Black,
            blendMode = BlendMode.Clear
        )
        val marginWidth: Float? = iconCutout.marginWidth
        if (marginWidth != null) {
            drawPath(
                path = outline,
                color = Color.Black,
                style = Stroke(
                    width = marginWidth,
                    join = StrokeJoin.Round
                ),
                blendMode = BlendMode.Clear
            )
        }
    }
}

@Composable
private fun VadretIconCatalog(
    ink: Color,
    background: Color,
    modifier: Modifier = Modifier
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(space = previewSpacing),
        verticalArrangement = Arrangement.spacedBy(space = previewSpacing),
        modifier = modifier
            .background(color = background)
            .padding(all = previewSpacing)
    ) {
        vadretIconCatalog.forEach { icon: VadretIconGraphic ->
            VadretIcon(
                icon = icon,
                contentDescription = icon.name,
                tint = ink,
                modifier = Modifier.size(size = previewIconSize)
            )
        }
    }
}

@Preview(widthDp = 420)
@Composable
private fun LightVadretIconsPreview() {
    VadretIconCatalog(
        ink = lightPreviewInk,
        background = lightPreviewBackground
    )
}

@Preview(widthDp = 420)
@Composable
private fun DarkVadretIconsPreview() {
    VadretIconCatalog(
        ink = darkPreviewInk,
        background = darkPreviewBackground
    )
}