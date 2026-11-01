package se.vadretapp.vadret.wind.remote

internal enum class WindParameter(
    val parameterName: String
) {
    SPEED(parameterName = "wind_speed"),
    FROM_DIRECTION(parameterName = "wind_from_direction")
}