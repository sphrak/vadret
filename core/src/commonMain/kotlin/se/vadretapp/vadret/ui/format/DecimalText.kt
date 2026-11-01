package se.vadretapp.vadret.ui.format

import kotlin.math.absoluteValue
import kotlin.math.floor

private const val TENTHS_PER_UNIT = 10
private const val HALF = 0.5
private const val NO_SIGN = ""
private const val NEGATIVE_SIGN = "-"
private const val DECIMAL_SEPARATOR = "."

fun Double.toOneDecimalText(): String {
    val tenthsMagnitude: Long = floor(x = absoluteValue * TENTHS_PER_UNIT + HALF).toLong()
    val sign: String = if (this < 0 && tenthsMagnitude > 0) {
        NEGATIVE_SIGN
    } else {
        NO_SIGN
    }
    return "$sign${tenthsMagnitude / TENTHS_PER_UNIT}$DECIMAL_SEPARATOR${tenthsMagnitude % TENTHS_PER_UNIT}"
}