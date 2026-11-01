package se.vadretapp.vadret.weatherforecast.extension

import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.pow

private const val COORDINATE_DECIMALS = 6
private const val DECIMAL_BASE = 10.0
private const val HALF = 0.5
private const val NO_SIGN = ""
private const val NEGATIVE_SIGN = "-"
private const val DECIMAL_SEPARATOR = "."
private const val ZERO_DIGIT = '0'

private val coordinateScale: Long = DECIMAL_BASE.pow(COORDINATE_DECIMALS).toLong()

internal fun Double.toCoordinateText(): String {
    val scaledMagnitude: Long = floor(x = absoluteValue * coordinateScale + HALF).toLong()
    val wholeDegrees: Long = scaledMagnitude / coordinateScale
    val fraction: String = (scaledMagnitude % coordinateScale)
        .toString()
        .padStart(length = COORDINATE_DECIMALS, padChar = ZERO_DIGIT)
        .trimEnd(ZERO_DIGIT)
    val sign: String = if (this < 0 && scaledMagnitude > 0) {
        NEGATIVE_SIGN
    } else {
        NO_SIGN
    }
    return if (fraction.isEmpty()) {
        "$sign$wholeDegrees"
    } else {
        "$sign$wholeDegrees$DECIMAL_SEPARATOR$fraction"
    }
}