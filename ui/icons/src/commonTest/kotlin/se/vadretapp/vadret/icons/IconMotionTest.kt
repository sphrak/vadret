package se.vadretapp.vadret.icons

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val TOLERANCE = 0.001f
private const val SLANT = -0.42f

private fun motion(
    kind: IconMotionKind,
    durationMilliseconds: Int = 1_000,
    delayMilliseconds: Int = 0,
    easing: IconEasing = IconEasing.LINEAR
): IconMotion =
    IconMotion(
        kind = kind,
        durationMilliseconds = durationMilliseconds,
        delayMilliseconds = delayMilliseconds,
        easing = easing
    )

class IconMotionTest {

    @Test
    fun `a negative delay starts the motion part of the way through`() {
        val delayedFall: IconMotion = motion(
            kind = IconMotionKind.FALL,
            durationMilliseconds = 900,
            delayMilliseconds = -300
        )
        val fall: IconMotion = motion(
            kind = IconMotionKind.FALL,
            durationMilliseconds = 900
        )

        assertEquals(
            expected = fall.poseAt(timeMilliseconds = 300L).offsetY,
            actual = delayedFall.poseAt(timeMilliseconds = 0L).offsetY,
            absoluteTolerance = TOLERANCE
        )
    }

    @Test
    fun `a drop falls and fades in and out`() {
        val fall: IconMotion = motion(kind = IconMotionKind.FALL)
        val poses: List<IconPose> = listOf(0L, 500L, 999L).map { timeMilliseconds: Long ->
            fall.poseAt(timeMilliseconds = timeMilliseconds)
        }

        assertEquals(expected = 0f, actual = poses[0].alpha, absoluteTolerance = TOLERANCE)
        assertEquals(expected = 1f, actual = poses[1].alpha, absoluteTolerance = TOLERANCE)
        assertTrue(actual = poses[2].alpha < 0.01f)
        assertTrue(actual = poses[0].offsetY < poses[1].offsetY && poses[1].offsetY < poses[2].offsetY)
        assertEquals(expected = 0f, actual = poses[1].offsetX, absoluteTolerance = TOLERANCE)
    }

    @Test
    fun `a snowflake sways from side to side as it falls`() {
        val flake: IconMotion = motion(kind = IconMotionKind.FLAKE)
        val quarter: IconPose = flake.poseAt(timeMilliseconds = 250L)
        val threeQuarters: IconPose = flake.poseAt(timeMilliseconds = 750L)

        assertTrue(actual = quarter.offsetX > 0f && threeQuarters.offsetX < 0f)
        assertTrue(actual = quarter.offsetY < threeQuarters.offsetY)
        assertEquals(expected = 1f, actual = flake.poseAt(timeMilliseconds = 500L).alpha, absoluteTolerance = TOLERANCE)
    }

    @Test
    fun `slanted rain and snow fall sideways like the drops are drawn`() {
        val poses: List<IconPose> = listOf(IconMotionKind.SLANTED_FALL, IconMotionKind.SLANTED_FLAKE).map { kind: IconMotionKind ->
            motion(kind = kind).poseAt(timeMilliseconds = 500L)
        }

        assertEquals(expected = SLANT * poses[0].offsetY, actual = poses[0].offsetX, absoluteTolerance = TOLERANCE)
        assertEquals(expected = SLANT * poses[1].offsetY, actual = poses[1].offsetX, absoluteTolerance = TOLERANCE)
    }

    @Test
    fun `drifting clouds and fog come back to where they started`() {
        listOf(IconMotionKind.DRIFT, IconMotionKind.FOG).forEach { kind: IconMotionKind ->
            val swaying: IconMotion = motion(
                kind = kind,
                easing = IconEasing.EASE_IN_OUT
            )
            val start: IconPose = swaying.poseAt(timeMilliseconds = 0L)
            val end: IconPose = swaying.poseAt(timeMilliseconds = 1_000L)
            val middle: IconPose = swaying.poseAt(timeMilliseconds = 250L)

            assertEquals(expected = start.offsetX, actual = end.offsetX, absoluteTolerance = TOLERANCE)
            assertTrue(actual = middle.offsetX != start.offsetX)
        }
    }

    @Test
    fun `lightning flickers twice and otherwise stays lit`() {
        val bolt: IconMotion = motion(kind = IconMotionKind.BOLT)

        assertEquals(
            expected = listOf(1f, 0.25f, 1f, 0.35f, 1f),
            actual = listOf(500L, 820L, 860L, 900L, 950L).map { timeMilliseconds: Long ->
                bolt.poseAt(timeMilliseconds = timeMilliseconds).alpha
            }
        )
    }
}