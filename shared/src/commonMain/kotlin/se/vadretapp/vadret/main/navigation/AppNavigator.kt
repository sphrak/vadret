package se.vadretapp.vadret.main.navigation

internal interface AppNavigator {

    fun open(key: VadretKey)

    fun close(key: VadretKey)

    fun openPlaceForecast(placeForecastKey: PlaceForecastKey)

    fun closePlaceForecast(placeForecastKey: PlaceForecastKey)

    fun openWeatherTab()
}