package se.vadretapp.vadret.weatherforecast.util

import kotlin.math.pow

private val windSpeedRange: ClosedFloatingPointRange<Double> = 2.0..35.0
private val temperatureRange: ClosedFloatingPointRange<Double> = -40.0..10.0

private const val BASE = 13.12
private const val TEMPERATURE_FACTOR = 0.6215
private const val WIND_FACTOR = 13.956
private const val TEMPERATURE_WIND_FACTOR = 0.48669
private const val WIND_EXPONENT = 0.16

internal object WindChill {

    fun calculate(
        temperatureCelcius: Double,
        windSpeedMeterPerSecond: Double
    ): Double? {
        if (windSpeedMeterPerSecond !in windSpeedRange || temperatureCelcius !in temperatureRange) {
            return null
        }
        val windFactor: Double = windSpeedMeterPerSecond.pow(WIND_EXPONENT)
        return BASE +
            TEMPERATURE_FACTOR * temperatureCelcius -
            WIND_FACTOR * windFactor +
            TEMPERATURE_WIND_FACTOR * temperatureCelcius * windFactor
    }
}