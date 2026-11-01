package se.vadretapp.vadret.radar.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

internal const val TAPS_TO_TOGGLE = 7
private const val TAPS_BEFORE_HINT = 5
private const val MAXIMUM_TAP_GAP_MILLISECONDS = 500L
private const val FIRST_TAP = 1
private const val NO_TAPS = 0

private val hiddenToggleSize = 48.dp

internal fun nextTapCount(
    tapCount: Int,
    tapGapMilliseconds: Long
): Int {
    val isQuickTap: Boolean = tapGapMilliseconds <= MAXIMUM_TAP_GAP_MILLISECONDS
    return if (isQuickTap && tapCount < TAPS_TO_TOGGLE) {
        tapCount + 1
    } else {
        FIRST_TAP
    }
}

@Composable
internal fun HiddenToggle(
    onAlmostThere: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnAlmostThere: () -> Unit by rememberUpdatedState(newValue = onAlmostThere)
    val currentOnToggle: () -> Unit by rememberUpdatedState(newValue = onToggle)
    Box(
        modifier = modifier
            .size(size = hiddenToggleSize)
            .pointerInput(key1 = Unit) {
                var tapCount: Int = NO_TAPS
                var lastTapMilliseconds: Long = Long.MIN_VALUE
                awaitEachGesture {
                    awaitFirstDown()
                    val up: PointerInputChange = waitForUpOrCancellation() ?: return@awaitEachGesture
                    tapCount = nextTapCount(
                        tapCount = tapCount,
                        tapGapMilliseconds = up.uptimeMillis - lastTapMilliseconds
                    )
                    lastTapMilliseconds = up.uptimeMillis
                    if (tapCount == TAPS_BEFORE_HINT) {
                        currentOnAlmostThere()
                    }
                    if (tapCount == TAPS_TO_TOGGLE) {
                        currentOnToggle()
                    }
                }
            }
    )
}