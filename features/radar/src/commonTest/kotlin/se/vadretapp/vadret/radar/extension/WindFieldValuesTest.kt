package se.vadretapp.vadret.radar.extension

import se.vadretapp.vadret.radar.wind.WindFieldValues
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertSame

private const val TRANSPARENT = 0

class WindFieldValuesTest {

    private fun windFieldValues(
        eastward: Float,
        northward: Float,
        speed: Float,
        color: Int
    ): WindFieldValues =
        WindFieldValues(
            width = 2,
            height = 1,
            eastward = floatArrayOf(eastward, Float.NaN),
            northward = floatArrayOf(northward, Float.NaN),
            speeds = floatArrayOf(speed, Float.NaN),
            colors = intArrayOf(color, TRANSPARENT)
        )

    private val first: WindFieldValues = windFieldValues(
        eastward = 2f,
        northward = -4f,
        speed = 4f,
        color = 0xFF204060.toInt()
    )

    private val next: WindFieldValues = windFieldValues(
        eastward = 6f,
        northward = 4f,
        speed = 8f,
        color = 0xFF6080A0.toInt()
    )

    @Test
    fun `the start and the end are the two hours themselves`() {
        val start: WindFieldValues = first.towards(nextWindFieldValues = next, progress = 0f)
        val end: WindFieldValues = first.towards(nextWindFieldValues = next, progress = 1f)

        assertContentEquals(expected = first.speeds, actual = start.speeds)
        assertContentEquals(expected = first.colors, actual = start.colors)
        assertContentEquals(expected = next.eastward, actual = end.eastward)
        assertContentEquals(expected = next.colors, actual = end.colors)
    }

    @Test
    fun `halfway the wind and its colour are in between`() {
        val halfway: WindFieldValues = first.towards(nextWindFieldValues = next, progress = 0.5f)

        assertContentEquals(expected = floatArrayOf(4f, Float.NaN), actual = halfway.eastward)
        assertContentEquals(expected = floatArrayOf(0f, Float.NaN), actual = halfway.northward)
        assertContentEquals(expected = floatArrayOf(6f, Float.NaN), actual = halfway.speeds)
        assertContentEquals(expected = intArrayOf(0xFF406080.toInt(), TRANSPARENT), actual = halfway.colors)
    }

    @Test
    fun `fields of different sizes jump to the next hour`() {
        val wider = WindFieldValues(
            width = 3,
            height = 1,
            eastward = floatArrayOf(1f, 1f, 1f),
            northward = floatArrayOf(1f, 1f, 1f),
            speeds = floatArrayOf(1f, 1f, 1f),
            colors = intArrayOf(TRANSPARENT, TRANSPARENT, TRANSPARENT)
        )

        assertSame(expected = wider, actual = first.towards(nextWindFieldValues = wider, progress = 0.5f))
    }
}