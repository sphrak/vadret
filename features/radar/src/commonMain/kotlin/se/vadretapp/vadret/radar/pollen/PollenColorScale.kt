package se.vadretapp.vadret.radar.pollen

import androidx.compose.ui.graphics.Color
import se.vadretapp.vadret.pollen.model.PollenLevel

private const val NO_POLLEN_COLOR = 0xFFB8BEC6
private const val LOW_COLOR = 0xFFFFF0A8
private const val LOW_TO_MODERATE_COLOR = 0xFFFFDB6E
private const val MODERATE_COLOR = 0xFFFFC23D
private const val MODERATE_TO_HIGH_COLOR = 0xFFFF9838
private const val HIGH_COLOR = 0xFFF26430
private const val HIGH_TO_VERY_HIGH_COLOR = 0xFFD43A33
private const val VERY_HIGH_COLOR = 0xFF9C1F3F

internal object PollenColorScale {

    val legendColors: List<Color> = listOf(
        Color(color = LOW_COLOR),
        Color(color = LOW_TO_MODERATE_COLOR),
        Color(color = MODERATE_COLOR),
        Color(color = MODERATE_TO_HIGH_COLOR),
        Color(color = HIGH_COLOR),
        Color(color = HIGH_TO_VERY_HIGH_COLOR),
        Color(color = VERY_HIGH_COLOR)
    )

    fun argb(pollenLevel: PollenLevel): Int =
        when (pollenLevel) {
            PollenLevel.NONE -> NO_POLLEN_COLOR
            PollenLevel.LOW -> LOW_COLOR
            PollenLevel.LOW_TO_MODERATE -> LOW_TO_MODERATE_COLOR
            PollenLevel.MODERATE -> MODERATE_COLOR
            PollenLevel.MODERATE_TO_HIGH -> MODERATE_TO_HIGH_COLOR
            PollenLevel.HIGH -> HIGH_COLOR
            PollenLevel.HIGH_TO_VERY_HIGH -> HIGH_TO_VERY_HIGH_COLOR
            PollenLevel.VERY_HIGH -> VERY_HIGH_COLOR
        }.toInt()
}