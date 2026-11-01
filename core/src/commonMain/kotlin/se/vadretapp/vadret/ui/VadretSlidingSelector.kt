package se.vadretapp.vadret.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

private const val PILL_CORNER_RADIUS_FRACTION = 0.5f
private const val UNCOVERED = 0f
private const val COVERED = 1f

private val indicatorSpring: AnimationSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)

private data class OptionBounds(
    val left: Float,
    val width: Float
)

@Composable
fun VadretSlidingSelector(
    optionCount: Int,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    role: Role,
    modifier: Modifier = Modifier,
    isEqualWidth: Boolean = false,
    unselectedContentColor: Color = VadretTheme.colorScheme.onSurfaceVariant,
    optionContent: @Composable RowScope.(index: Int, contentColor: Color, coverage: Float) -> Unit
) {
    val indicatorColor: Color = VadretTheme.colorScheme.primaryContainer
    val selectedContentColor: Color = VadretTheme.colorScheme.onPrimaryContainer
    val optionBounds: SnapshotStateList<OptionBounds?> = remember(optionCount) {
        mutableStateListOf<OptionBounds?>().apply { repeat(times = optionCount) { add(null) } }
    }
    val indicatorLeft: Animatable<Float, AnimationVector1D> = remember { Animatable(initialValue = 0f) }
    val indicatorWidth: Animatable<Float, AnimationVector1D> = remember { Animatable(initialValue = 0f) }
    var isIndicatorPlaced: Boolean by remember { mutableStateOf(false) }
    val target: OptionBounds? = optionBounds.getOrNull(index = selectedIndex)

    LaunchedEffect(target) {
        if (target == null) {
            return@LaunchedEffect
        }
        if (isIndicatorPlaced) {
            launch { indicatorLeft.animateTo(targetValue = target.left, animationSpec = indicatorSpring) }
            indicatorWidth.animateTo(targetValue = target.width, animationSpec = indicatorSpring)
        } else {
            indicatorLeft.snapTo(targetValue = target.left)
            indicatorWidth.snapTo(targetValue = target.width)
            isIndicatorPlaced = true
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .selectableGroup()
            .drawBehind {
                if (isIndicatorPlaced) {
                    val pillCornerRadius: Float = size.height * PILL_CORNER_RADIUS_FRACTION
                    drawRoundRect(
                        color = indicatorColor,
                        topLeft = Offset(x = indicatorLeft.value, y = 0f),
                        size = Size(width = indicatorWidth.value, height = size.height),
                        cornerRadius = CornerRadius(x = pillCornerRadius, y = pillCornerRadius)
                    )
                }
            }
    ) {
        repeat(times = optionCount) { index: Int ->
            val isSelected: Boolean = index == selectedIndex
            val bounds: OptionBounds? = optionBounds.getOrNull(index = index)
            val coverage: Float = if (isIndicatorPlaced && bounds != null) {
                bounds.coverage(
                    indicatorLeft = indicatorLeft.value,
                    indicatorWidth = indicatorWidth.value
                )
            } else if (isSelected) {
                COVERED
            } else {
                UNCOVERED
            }
            val contentColor: Color = lerp(
                start = unselectedContentColor,
                stop = selectedContentColor,
                fraction = coverage
            )
            val widthModifier: Modifier = if (isEqualWidth) {
                Modifier.weight(weight = 1f)
            } else {
                Modifier
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = widthModifier
                    .fillMaxHeight()
                    .onPlaced { coordinates ->
                        optionBounds[index] = OptionBounds(
                            left = coordinates.positionInParent().x,
                            width = coordinates.size.width.toFloat()
                        )
                    }
                    .selectable(
                        selected = isSelected,
                        interactionSource = null,
                        indication = null,
                        role = role,
                        onClick = { onOptionSelected(index) }
                    )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    optionContent(index, contentColor, coverage)
                }
            }
        }
    }
}

private fun OptionBounds.coverage(
    indicatorLeft: Float,
    indicatorWidth: Float
): Float {
    if (width <= UNCOVERED) {
        return UNCOVERED
    }
    val overlap: Float = min(left + width, indicatorLeft + indicatorWidth) - max(left, indicatorLeft)
    return (overlap / width).coerceIn(
        minimumValue = UNCOVERED,
        maximumValue = COVERED
    )
}