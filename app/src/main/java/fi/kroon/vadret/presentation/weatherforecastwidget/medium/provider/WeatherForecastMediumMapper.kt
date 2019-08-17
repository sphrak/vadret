package fi.kroon.vadret.presentation.weatherforecastwidget.medium.provider

import fi.kroon.vadret.data.weatherforecast.model.Parameter
import fi.kroon.vadret.data.weatherforecast.model.TimeSerie
import fi.kroon.vadret.presentation.weatherforecastwidget.medium.provider.model.WeatherForecastMediumModel
import fi.kroon.vadret.util.MPS_TO_KMPH_FACTOR
import fi.kroon.vadret.util.WINDCHILL_FORMULA_MAXIMUM
import fi.kroon.vadret.util.WINDCHILL_FORMULA_MINIMUM
import fi.kroon.vadret.util.extension.toWindChill

object WeatherForecastMediumMapper {

    operator fun invoke(
        timeSerieList: List<TimeSerie>,
        localityName: String?
    ): List<WeatherForecastMediumModel> {

        var temperature: Double = 0.0
        var windSpeed: Double = 0.0

        var precipitationCode: Int = 0
        var feelsLikeTemperature: String? = null
        var riskForThunder: Boolean = false
        var humidityPercent: Int = 0
        var thunderProbability: Int? = null
        var weatherIconResource: Int? = null

        timeSerieList.first().parameters.forEach { parameter: Parameter ->
            when (parameter.name) {
                "ws" -> windSpeed = parameter.values.first()
                "r" -> humidityPercent = parameter.values.first().toInt()
                "pcat" -> precipitationCode = parameter.values.first().toInt()
                "t" -> temperature = parameter.values.first()
                "tstm" -> thunderProbability = parameter.values.first().toInt()
                "Wsymb2" -> weatherIconResource = parameter.values.first().toInt()
            }
        }

        thunderProbability?.let {
            riskForThunder = it >= 50
        }

        if (temperature < WINDCHILL_FORMULA_MAXIMUM) {
            feelsLikeTemperature = if (windSpeed * MPS_TO_KMPH_FACTOR > WINDCHILL_FORMULA_MINIMUM) {
                temperature.toWindChill(windSpeed)
            } else null
        }

        return listOf(
            WeatherForecastMediumModel(
                localityName = localityName,
                temperature = temperature,
                humidityPercent = humidityPercent,
                feelsLikeTemperature = feelsLikeTemperature,
                precipitationCode = precipitationCode,
                thunderProbability = thunderProbability,
                windSpeed = windSpeed,
                wsymb2Icon = weatherIconResource,
                riskForThunder = riskForThunder
            )
        )
    }
}