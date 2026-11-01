package se.vadretapp.vadret.radar.extension

import kotlin.math.PI

private const val DEGREES_IN_HALF_TURN = 180.0
private const val DEGREES_TO_RADIANS: Double = PI / DEGREES_IN_HALF_TURN
private const val RADIANS_TO_DEGREES: Double = DEGREES_IN_HALF_TURN / PI

internal fun Double.toRadians(): Double = this * DEGREES_TO_RADIANS

internal fun Double.toDegrees(): Double = this * RADIANS_TO_DEGREES