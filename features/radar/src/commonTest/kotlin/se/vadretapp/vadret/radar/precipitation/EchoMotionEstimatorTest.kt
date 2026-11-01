package se.vadretapp.vadret.radar.precipitation

import se.vadretapp.vadret.radar.projection.PrecipitationColorScale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val WIDTH = 320
private const val HEIGHT = 256
private const val TOLERANCE = 1.5f
private const val STILL_TOLERANCE = 1f
private const val NO_ECHO = 0
private const val STRONGEST_ECHO = 254
private const val SHOWN_RAIN = 1f
private const val WEAK_ECHO = 80
private const val EASTWARD_SHIFT = 8
private const val SOUTHWARD_SHIFT = 4

private class Shower(
    val x: Float,
    val y: Float,
    val radius: Float,
    val strength: Float
)

private val showers: List<Shower> = listOf(
    Shower(x = 70f, y = 60f, radius = 18f, strength = 120f),
    Shower(x = 110f, y = 90f, radius = 10f, strength = 150f),
    Shower(x = 90f, y = 140f, radius = 24f, strength = 100f),
    Shower(x = 150f, y = 70f, radius = 14f, strength = 90f),
    Shower(x = 60f, y = 190f, radius = 12f, strength = 140f)
)

class EchoMotionEstimatorTest {

    private fun echoes(level: (x: Int, y: Int) -> Int): ByteArray =
        ByteArray(size = WIDTH * HEIGHT) { index: Int ->
            level(index % WIDTH, index / WIDTH).coerceIn(minimumValue = NO_ECHO, maximumValue = STRONGEST_ECHO).toByte()
        }

    private fun rain(
        x: Int,
        y: Int
    ): Int {
        val level: Float = showers.sumOf { shower: Shower ->
            val distanceSquared: Float = (x - shower.x) * (x - shower.x) + (y - shower.y) * (y - shower.y)
            (shower.strength * exp(-distanceSquared / (2f * shower.radius * shower.radius))).toDouble()
        }.toFloat()
        return if (level > SHOWN_RAIN) {
            PrecipitationColorScale.firstVisibleEchoLevel + level.roundToInt()
        } else {
            NO_ECHO
        }
    }

    private fun estimate(
        previous: ByteArray,
        next: ByteArray
    ): MotionField =
        EchoMotionEstimator.estimate(
            previous = previous,
            next = next,
            width = WIDTH,
            height = HEIGHT
        )

    @Test
    fun `the field has one cell for every 32 pixels rounded up`() {
        val motionField: MotionField = EchoMotionEstimator.estimate(
            previous = ByteArray(size = 100 * 70),
            next = ByteArray(size = 100 * 70),
            width = 100,
            height = 70
        )

        assertEquals(expected = listOf(4, 3), actual = listOf(motionField.columns, motionField.rows))
        assertEquals(expected = 12, actual = motionField.eastward.size)
        assertEquals(expected = 12, actual = motionField.southward.size)
    }

    @Test
    fun `rain moving south east is followed in every cell`() {
        val motionField: MotionField = estimate(
            previous = echoes { x: Int, y: Int -> rain(x = x, y = y) },
            next = echoes { x: Int, y: Int -> rain(x = x - EASTWARD_SHIFT, y = y - SOUTHWARD_SHIFT) }
        )

        assertTrue(
            actual = motionField.eastward.all { eastward: Float -> abs(eastward - EASTWARD_SHIFT) <= TOLERANCE },
            message = motionField.eastward.joinToString()
        )
        assertTrue(
            actual = motionField.southward.all { southward: Float -> abs(southward - SOUTHWARD_SHIFT) <= TOLERANCE },
            message = motionField.southward.joinToString()
        )
    }

    @Test
    fun `rain moving north west gives the opposite field`() {
        val motionField: MotionField = estimate(
            previous = echoes { x: Int, y: Int -> rain(x = x, y = y) },
            next = echoes { x: Int, y: Int -> rain(x = x + EASTWARD_SHIFT, y = y + SOUTHWARD_SHIFT) }
        )

        assertTrue(actual = motionField.eastward.all { eastward: Float -> abs(eastward + EASTWARD_SHIFT) <= TOLERANCE })
        assertTrue(actual = motionField.southward.all { southward: Float -> abs(southward + SOUTHWARD_SHIFT) <= TOLERANCE })
    }

    @Test
    fun `rain that stands still moves less than a pixel`() {
        val echoes: ByteArray = echoes { x: Int, y: Int -> rain(x = x, y = y) }

        val motionField: MotionField = estimate(
            previous = echoes,
            next = echoes
        )

        assertTrue(
            actual = motionField.eastward.all { eastward: Float -> abs(eastward) <= STILL_TOLERANCE },
            message = motionField.eastward.joinToString()
        )
        assertTrue(
            actual = motionField.southward.all { southward: Float -> abs(southward) <= STILL_TOLERANCE },
            message = motionField.southward.joinToString()
        )
    }

    @Test
    fun `without rain nothing moves`() {
        val motionField: MotionField = estimate(
            previous = echoes { _: Int, _: Int -> NO_ECHO },
            next = echoes { _: Int, _: Int -> NO_ECHO }
        )

        assertTrue(actual = motionField.eastward.all { eastward: Float -> eastward == 0f })
        assertTrue(actual = motionField.southward.all { southward: Float -> southward == 0f })
    }

    @Test
    fun `echoes too weak to be drawn do not move the field`() {
        val motionField: MotionField = estimate(
            previous = echoes { x: Int, _: Int -> WEAK_ECHO * (x % 2) },
            next = echoes { x: Int, _: Int -> WEAK_ECHO * ((x + 1) % 2) }
        )

        assertTrue(actual = motionField.eastward.all { eastward: Float -> eastward == 0f })
        assertTrue(actual = motionField.southward.all { southward: Float -> southward == 0f })
    }
}