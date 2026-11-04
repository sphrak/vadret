package se.vadretapp.vadret.radar.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.icons.rememberIsMotionEnabled
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.radar.ui.overlay.MapProjection
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.pegman_hint
import kotlin.math.roundToInt

private val pegmanButtonSize: Dp = 44.dp
private val pegmanButtonShape = RoundedCornerShape(size = 8.dp)
private val pegmanButtonElevation: Dp = 4.dp
private val buttonFigureHeight: Dp = 21.dp
private val figureHeight: Dp = 34.dp

private val hoverAboveLanding: Dp = 12.dp
private val carriedShadowSize = DpSize(
    width = 16.dp,
    height = 6.dp
)
private val standingShadowSize = DpSize(
    width = 20.dp,
    height = 6.dp
)
private val landingDotRadius: Dp = 4.dp
private val landingDotRingWidth: Dp = 2.dp
private val carriedShadowColor: Color = Color.Black.copy(alpha = 0.45f)
private val standingShadowColor: Color = Color.Black.copy(alpha = 0.55f)
private val heldByTheHead = TransformOrigin(
    pivotFractionX = 0.5f,
    pivotFractionY = headCenterFraction
)
private val swaySpring: SpringSpec<Float> = spring(
    dampingRatio = 0.3f,
    stiffness = Spring.StiffnessLow
)
private val pickUpSpring: SpringSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium
)
private val landingSpring: SpringSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow
)

// A bounce past standing would cross the arms and legs.
private val settleSpring: SpringSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)

private const val SWAY_DEGREES_PER_DP = 0.6f
private const val MAXIMUM_SWAY_DEGREES = 25f
private const val FULL_SCALE = 1f
private const val FEET = 1f
private const val MIDDLE = 0.5f
private const val HALF = 2f

private val pickedUpScale: Float = buttonFigureHeight / figureHeight

// Hanging below the fingertip keeps the finger off both the figure and the spot it lands on.
private val headBelowFinger: Dp = 24.dp
private val landingBelowHead: Dp = figureHeight * (FEET - headCenterFraction) + hoverAboveLanding
private val landingBelowFinger: Dp = headBelowFinger + landingBelowHead

@Composable
internal fun PegmanButton(
    isFigureLifted: Boolean,
    onTapped: () -> Unit,
    onDragged: (landing: Offset) -> Unit,
    onDropped: () -> Unit,
    onDragCancelled: () -> Unit,
    modifier: Modifier = Modifier
) {
    var buttonOrigin: Offset by remember { mutableStateOf(value = Offset.Zero) }
    val currentOnDragged: (Offset) -> Unit by rememberUpdatedState(newValue = onDragged)
    val currentOnDropped: () -> Unit by rememberUpdatedState(newValue = onDropped)
    val currentOnDragCancelled: () -> Unit by rememberUpdatedState(newValue = onDragCancelled)
    val pegmanHint: String = stringResource(resource = Res.string.pegman_hint)
    Surface(
        onClick = onTapped,
        shape = pegmanButtonShape,
        color = VadretTheme.colorScheme.surface,
        shadowElevation = pegmanButtonElevation,
        modifier = modifier
            .size(size = pegmanButtonSize)
            .semantics { contentDescription = pegmanHint }
            .onGloballyPositioned { coordinates -> buttonOrigin = coordinates.positionInRoot() }
            .pointerInput(Unit) {
                val fingerToLanding = Offset(
                    x = 0f,
                    y = landingBelowFinger.toPx()
                )
                var landing: Offset = Offset.Zero
                detectDragGestures(
                    onDragStart = { start: Offset ->
                        landing = start + fingerToLanding
                        currentOnDragged(buttonOrigin + landing)
                    },
                    onDrag = { change: PointerInputChange, _: Offset ->
                        change.consume()
                        landing = change.position + fingerToLanding
                        currentOnDragged(buttonOrigin + landing)
                    },
                    onDragEnd = {
                        if (size.toSize().toRect().contains(offset = landing)) {
                            currentOnDragCancelled()
                        } else {
                            currentOnDropped()
                        }
                    },
                    onDragCancel = { currentOnDragCancelled() }
                )
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            PegmanFigure(
                height = buttonFigureHeight,
                isSilhouette = isFigureLifted
            )
        }
    }
}

@Composable
internal fun DroppedPegman(
    location: Location,
    mapProjection: MapProjection,
    modifier: Modifier = Modifier
) {
    val isMotionEnabled: Boolean = rememberIsMotionEnabled()
    val hover: Float = with(receiver = LocalDensity.current) { hoverAboveLanding.toPx() }
    val fall: Animatable<Float, AnimationVector1D> = remember(location) {
        Animatable(
            initialValue = if (isMotionEnabled) {
                -hover
            } else {
                0f
            }
        )
    }
    val spread: Animatable<Float, AnimationVector1D> = remember(location) {
        Animatable(
            initialValue = if (isMotionEnabled) {
                CARRIED
            } else {
                STANDING
            }
        )
    }
    LaunchedEffect(fall) {
        fall.animateTo(
            targetValue = 0f,
            animationSpec = landingSpring
        )
    }
    LaunchedEffect(spread) {
        spread.animateTo(
            targetValue = STANDING,
            animationSpec = settleSpring
        )
    }
    val point: () -> Offset? = {
        mapProjection.screenOffset(
            latitude = location.latitude,
            longitude = location.longitude
        )
    }
    FigureShadow(
        point = point,
        shadowSize = standingShadowSize,
        shadowColor = standingShadowColor
    )
    PegmanFigure(
        height = figureHeight,
        isSilhouette = false,
        spread = { spread.value },
        modifier = modifier
            .anchoredAt(
                verticalAnchor = FEET,
                point = point
            ).graphicsLayer { translationY = fall.value }
    )
}

@Composable
internal fun DraggedPegman(
    landing: () -> Offset,
    modifier: Modifier = Modifier
) {
    val isMotionEnabled: Boolean = rememberIsMotionEnabled()
    val density: Density = LocalDensity.current
    val headAboveLanding: Float = with(receiver = density) { landingBelowHead.toPx() }
    val sway: Animatable<Float, AnimationVector1D> = remember { Animatable(initialValue = 0f) }
    val spread: Animatable<Float, AnimationVector1D> = remember {
        Animatable(
            initialValue = if (isMotionEnabled) {
                STANDING
            } else {
                CARRIED
            }
        )
    }
    LaunchedEffect(spread) {
        spread.animateTo(
            targetValue = CARRIED,
            animationSpec = pickUpSpring
        )
    }
    if (isMotionEnabled) {
        LaunchedEffect(sway) {
            sway.swayAfter(
                x = { landing().x },
                pixelsPerDp = density.density
            )
        }
    }
    FigureShadow(
        point = landing,
        shadowSize = carriedShadowSize,
        shadowColor = carriedShadowColor,
        isLandingMarked = true
    )
    PegmanFigure(
        height = figureHeight,
        isSilhouette = false,
        spread = { spread.value },
        modifier = modifier
            .anchoredAt(verticalAnchor = headCenterFraction) {
                landing() - Offset(
                    x = 0f,
                    y = headAboveLanding
                )
            }.graphicsLayer {
                val scale: Float = pickedUpScale + (FULL_SCALE - pickedUpScale) * spread.value
                scaleX = scale
                scaleY = scale
                rotationZ = sway.value
                transformOrigin = heldByTheHead
            }
    )
}

private suspend fun Animatable<Float, AnimationVector1D>.swayAfter(
    x: () -> Float,
    pixelsPerDp: Float
) {
    var previousX: Float? = null
    snapshotFlow(block = x).collectLatest { currentX: Float ->
        val lastX: Float? = previousX
        previousX = currentX
        if (lastX != null) {
            snapTo(
                targetValue = (value + (currentX - lastX) / pixelsPerDp * SWAY_DEGREES_PER_DP).coerceIn(
                    minimumValue = -MAXIMUM_SWAY_DEGREES,
                    maximumValue = MAXIMUM_SWAY_DEGREES
                )
            )
        }
        animateTo(
            targetValue = 0f,
            animationSpec = swaySpring
        )
    }
}

@Composable
private fun FigureShadow(
    point: () -> Offset?,
    shadowSize: DpSize,
    shadowColor: Color,
    modifier: Modifier = Modifier,
    isLandingMarked: Boolean = false
) {
    val dotColor: Color = VadretTheme.colorScheme.primary
    val ringColor: Color = VadretTheme.colorScheme.background
    Canvas(
        modifier = modifier
            .anchoredAt(
                verticalAnchor = MIDDLE,
                point = point
            ).size(size = shadowSize)
    ) {
        drawOval(color = shadowColor)
        if (isLandingMarked) {
            drawCircle(
                color = ringColor,
                radius = landingDotRadius.toPx()
            )
            drawCircle(
                color = dotColor,
                radius = (landingDotRadius - landingDotRingWidth).toPx()
            )
        }
    }
}

// Read while placing, so a moving finger or camera moves what is placed without recomposing it.
private fun Modifier.anchoredAt(
    verticalAnchor: Float,
    point: () -> Offset?
): Modifier =
    layout { measurable, constraints ->
        val placeable: Placeable = measurable.measure(constraints = constraints)
        layout(
            width = placeable.width,
            height = placeable.height
        ) {
            val anchor: Offset? = point()
            if (anchor != null) {
                placeable.place(
                    x = (anchor.x - placeable.width / HALF).roundToInt(),
                    y = (anchor.y - placeable.height * verticalAnchor).roundToInt()
                )
            }
        }
    }