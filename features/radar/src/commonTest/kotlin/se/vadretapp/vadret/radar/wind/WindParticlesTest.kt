package se.vadretapp.vadret.radar.wind

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val FIELD_SIZE = 10

private fun uniformField(
    eastward: Float,
    northward: Float
): WindFieldValues =
    WindFieldValues(
        width = FIELD_SIZE,
        height = FIELD_SIZE,
        eastward = FloatArray(size = FIELD_SIZE * FIELD_SIZE) { eastward },
        northward = FloatArray(size = FIELD_SIZE * FIELD_SIZE) { northward },
        speeds = FloatArray(size = FIELD_SIZE * FIELD_SIZE) { 1f },
        colors = IntArray(size = FIELD_SIZE * FIELD_SIZE)
    )

private val wholeField = RasterArea(
    left = 0f,
    top = 0f,
    right = FIELD_SIZE.toFloat(),
    bottom = FIELD_SIZE.toFloat()
)

class WindParticlesTest {

    @Test
    fun `the first step only seeds particles inside the visible area`() {
        val windParticles = WindParticles(particleCount = 50, random = Random(seed = 1))

        windParticles.advance(
            windFieldValues = uniformField(eastward = 1f, northward = 0f),
            visibleArea = wholeField,
            rasterPixelsPerMeterPerSecond = 0.1f
        )

        assertTrue(actual = windParticles.isSeeded)
        repeat(times = 50) { particle: Int ->
            assertTrue(
                actual = wholeField.contains(
                    x = windParticles.x(particle = particle, age = 0),
                    y = windParticles.y(particle = particle, age = 0)
                )
            )
            assertEquals(
                expected = windParticles.x(particle = particle, age = 0),
                actual = windParticles.x(particle = particle, age = TRAIL_LENGTH - 1)
            )
        }
    }

    @Test
    fun `particles move with the wind and keep a trail`() {
        val windParticles = WindParticles(particleCount = 1, random = Random(seed = 2))
        val field: WindFieldValues = uniformField(eastward = 0f, northward = 2f)
        windParticles.advance(
            windFieldValues = field,
            visibleArea = wholeField,
            rasterPixelsPerMeterPerSecond = 0.1f
        )
        val startX: Float = windParticles.x(particle = 0, age = 0)
        val startY: Float = windParticles.y(particle = 0, age = 0)

        windParticles.advance(
            windFieldValues = field,
            visibleArea = wholeField,
            rasterPixelsPerMeterPerSecond = 0.1f
        )

        assertEquals(expected = startX, actual = windParticles.x(particle = 0, age = 0), absoluteTolerance = 1e-5f)
        assertEquals(expected = startY - 0.2f, actual = windParticles.y(particle = 0, age = 0), absoluteTolerance = 1e-5f)
        assertEquals(expected = startY, actual = windParticles.y(particle = 0, age = 1), absoluteTolerance = 1e-5f)
    }

    @Test
    fun `particles that leave the visible area respawn inside it`() {
        val windParticles = WindParticles(particleCount = 20, random = Random(seed = 3))
        val field: WindFieldValues = uniformField(eastward = 100f, northward = 0f)

        repeat(times = 5) {
            windParticles.advance(
                windFieldValues = field,
                visibleArea = wholeField,
                rasterPixelsPerMeterPerSecond = 1f
            )
        }

        repeat(times = 20) { particle: Int ->
            val x: Float = windParticles.x(particle = particle, age = 1)
            assertTrue(actual = x < FIELD_SIZE)
        }
    }

    @Test
    fun `particles that leave while panning fill the part of the screen that came into view`() {
        val windParticles = WindParticles(particleCount = 200, random = Random(seed = 5))
        val field = WindFieldValues(
            width = FIELD_SIZE * 2,
            height = FIELD_SIZE,
            eastward = FloatArray(size = FIELD_SIZE * FIELD_SIZE * 2),
            northward = FloatArray(size = FIELD_SIZE * FIELD_SIZE * 2),
            speeds = FloatArray(size = FIELD_SIZE * FIELD_SIZE * 2) { 1f },
            colors = IntArray(size = FIELD_SIZE * FIELD_SIZE * 2)
        )
        val panned = RasterArea(left = 5f, top = 0f, right = 15f, bottom = FIELD_SIZE.toFloat())
        windParticles.advance(
            windFieldValues = field,
            visibleArea = wholeField,
            rasterPixelsPerMeterPerSecond = 1f
        )
        val leaving: Int = (0 until 200).count { particle: Int -> windParticles.x(particle = particle, age = 0) < 5f }

        windParticles.advance(
            windFieldValues = field,
            visibleArea = panned,
            rasterPixelsPerMeterPerSecond = 1f
        )

        val arrived: Int = (0 until 200).count { particle: Int -> windParticles.x(particle = particle, age = 0) >= FIELD_SIZE }
        assertTrue(actual = leaving > 0)
        assertEquals(expected = 100, actual = arrived)
        assertTrue(
            actual = (0 until 200).all { particle: Int ->
                panned.contains(
                    x = windParticles.x(particle = particle, age = 0),
                    y = windParticles.y(particle = particle, age = 0)
                )
            }
        )
    }

    @Test
    fun `zooming out spreads the particles over the whole screen`() {
        val windParticles = WindParticles(particleCount = 400, random = Random(seed = 6))
        val field = WindFieldValues(
            width = FIELD_SIZE * 2,
            height = FIELD_SIZE * 2,
            eastward = FloatArray(size = FIELD_SIZE * FIELD_SIZE * 4),
            northward = FloatArray(size = FIELD_SIZE * FIELD_SIZE * 4),
            speeds = FloatArray(size = FIELD_SIZE * FIELD_SIZE * 4) { 1f },
            colors = IntArray(size = FIELD_SIZE * FIELD_SIZE * 4)
        )
        val zoomedOut = RasterArea(left = 0f, top = 0f, right = FIELD_SIZE * 2f, bottom = FIELD_SIZE * 2f)
        windParticles.advance(
            windFieldValues = field,
            visibleArea = wholeField,
            rasterPixelsPerMeterPerSecond = 1f
        )

        windParticles.advance(
            windFieldValues = field,
            visibleArea = zoomedOut,
            rasterPixelsPerMeterPerSecond = 1f
        )

        val insidePreviousScreen: Int = (0 until 400).count { particle: Int ->
            wholeField.contains(
                x = windParticles.x(particle = particle, age = 0),
                y = windParticles.y(particle = particle, age = 0)
            )
        }
        assertEquals(expected = 100, actual = insidePreviousScreen)
    }

    @Test
    fun `a reset seeds again`() {
        val windParticles = WindParticles(particleCount = 5, random = Random(seed = 4))
        windParticles.advance(
            windFieldValues = uniformField(eastward = 0f, northward = 0f),
            visibleArea = wholeField,
            rasterPixelsPerMeterPerSecond = 1f
        )

        windParticles.reset()

        assertFalse(actual = windParticles.isSeeded)
    }
}