package se.vadretapp.vadret.radar.ui

import kotlin.test.Test
import kotlin.test.assertEquals

private const val QUICK_GAP_MILLISECONDS = 200L
private const val SLOW_GAP_MILLISECONDS = 900L
private const val LONGEST_QUICK_GAP_MILLISECONDS = 500L

class HiddenToggleTest {

    @Test
    fun `quick taps count up to the toggle`() {
        val tapCounts: List<Int> = (1..TAPS_TO_TOGGLE).runningFold(initial = 0) { tapCount: Int, _: Int ->
            nextTapCount(
                tapCount = tapCount,
                tapGapMilliseconds = QUICK_GAP_MILLISECONDS
            )
        }.drop(n = 1)

        assertEquals(expected = (1..TAPS_TO_TOGGLE).toList(), actual = tapCounts)
    }

    @Test
    fun `a slow tap starts counting again`() {
        assertEquals(
            expected = 1,
            actual = nextTapCount(
                tapCount = 5,
                tapGapMilliseconds = SLOW_GAP_MILLISECONDS
            )
        )
    }

    @Test
    fun `the longest quick gap still counts`() {
        assertEquals(
            expected = 4,
            actual = nextTapCount(
                tapCount = 3,
                tapGapMilliseconds = LONGEST_QUICK_GAP_MILLISECONDS
            )
        )
    }

    @Test
    fun `tapping on after a toggle needs seven more taps`() {
        assertEquals(
            expected = 1,
            actual = nextTapCount(
                tapCount = TAPS_TO_TOGGLE,
                tapGapMilliseconds = QUICK_GAP_MILLISECONDS
            )
        )
    }
}