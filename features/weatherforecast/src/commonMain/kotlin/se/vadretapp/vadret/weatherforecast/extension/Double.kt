package se.vadretapp.vadret.weatherforecast.extension

import se.vadretapp.vadret.ui.format.toOneDecimalText
import kotlin.math.roundToInt

private const val TENTHS_PER_UNIT = 10

internal fun Double.toTemperatureText(): String {
    val tenths: Int = (this * TENTHS_PER_UNIT).roundToInt()
    return if (tenths % TENTHS_PER_UNIT == 0) {
        (tenths / TENTHS_PER_UNIT).toString()
    } else {
        toOneDecimalText()
    }
}