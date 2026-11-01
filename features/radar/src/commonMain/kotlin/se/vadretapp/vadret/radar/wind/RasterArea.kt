package se.vadretapp.vadret.radar.wind

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

private const val NO_AREA = 0f

internal data class RasterArea(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {

    val isEmpty: Boolean = right <= left || bottom <= top

    val area: Float = if (isEmpty) {
        NO_AREA
    } else {
        (right - left) * (bottom - top)
    }

    fun uncoveredBy(other: RasterArea): List<RasterArea> =
        listOf(
            RasterArea(
                left = left,
                top = top,
                right = min(a = other.left, b = right),
                bottom = bottom
            ),
            RasterArea(
                left = max(a = other.right, b = left),
                top = top,
                right = right,
                bottom = bottom
            ),
            RasterArea(
                left = max(a = left, b = other.left),
                top = top,
                right = min(a = right, b = other.right),
                bottom = min(a = other.top, b = bottom)
            ),
            RasterArea(
                left = max(a = left, b = other.left),
                top = max(a = other.bottom, b = top),
                right = min(a = right, b = other.right),
                bottom = bottom
            )
        ).filterNot { strip: RasterArea -> strip.isEmpty }

    fun contains(
        x: Float,
        y: Float
    ): Boolean = x >= left && x < right && y >= top && y < bottom

    fun randomX(random: Random): Float = left + random.nextFloat() * (right - left)

    fun randomY(random: Random): Float = top + random.nextFloat() * (bottom - top)
}