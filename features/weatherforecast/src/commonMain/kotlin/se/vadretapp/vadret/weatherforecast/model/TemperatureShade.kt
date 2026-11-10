package se.vadretapp.vadret.weatherforecast.model

enum class TemperatureShade(val temperature: Double) {
    WARM(temperature = 1.0),
    ZERO(temperature = 0.0),
    MINUS_FIVE(temperature = -5.0),
    MINUS_TEN(temperature = -10.0),
    MINUS_FIFTEEN(temperature = -15.0),
    MINUS_TWENTY(temperature = -20.0)
}