package se.vadretapp.vadret.weatherforecast.util

import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.east
import se.vadretapp.vadret.core.resources.east_north_east
import se.vadretapp.vadret.core.resources.east_south_east
import se.vadretapp.vadret.core.resources.empty_wind_direction
import se.vadretapp.vadret.core.resources.north
import se.vadretapp.vadret.core.resources.north_east
import se.vadretapp.vadret.core.resources.north_north_east
import se.vadretapp.vadret.core.resources.north_north_west
import se.vadretapp.vadret.core.resources.north_west
import se.vadretapp.vadret.core.resources.south
import se.vadretapp.vadret.core.resources.south_east
import se.vadretapp.vadret.core.resources.south_south_east
import se.vadretapp.vadret.core.resources.south_south_west
import se.vadretapp.vadret.core.resources.south_west
import se.vadretapp.vadret.core.resources.west
import se.vadretapp.vadret.core.resources.west_north_west
import se.vadretapp.vadret.core.resources.west_south_west
import se.vadretapp.vadret.core.resources.wsymb2_clear_sky
import se.vadretapp.vadret.core.resources.wsymb2_cloudy_sky
import se.vadretapp.vadret.core.resources.wsymb2_fog
import se.vadretapp.vadret.core.resources.wsymb2_halfclear_sky
import se.vadretapp.vadret.core.resources.wsymb2_heavy_rain
import se.vadretapp.vadret.core.resources.wsymb2_heavy_rain_showers
import se.vadretapp.vadret.core.resources.wsymb2_heavy_sleet
import se.vadretapp.vadret.core.resources.wsymb2_heavy_sleet_showers
import se.vadretapp.vadret.core.resources.wsymb2_heavy_snow_showers
import se.vadretapp.vadret.core.resources.wsymb2_heavy_snowfall
import se.vadretapp.vadret.core.resources.wsymb2_light_rain
import se.vadretapp.vadret.core.resources.wsymb2_light_rain_showers
import se.vadretapp.vadret.core.resources.wsymb2_light_sleet
import se.vadretapp.vadret.core.resources.wsymb2_light_sleet_showers
import se.vadretapp.vadret.core.resources.wsymb2_light_snow_showers
import se.vadretapp.vadret.core.resources.wsymb2_light_snowfall
import se.vadretapp.vadret.core.resources.wsymb2_moderate_rain
import se.vadretapp.vadret.core.resources.wsymb2_moderate_rain_showers
import se.vadretapp.vadret.core.resources.wsymb2_moderate_sleet
import se.vadretapp.vadret.core.resources.wsymb2_moderate_sleet_showers
import se.vadretapp.vadret.core.resources.wsymb2_moderate_snow_showers
import se.vadretapp.vadret.core.resources.wsymb2_moderate_snowfall
import se.vadretapp.vadret.core.resources.wsymb2_nearly_clear_sky
import se.vadretapp.vadret.core.resources.wsymb2_overcast
import se.vadretapp.vadret.core.resources.wsymb2_thunder
import se.vadretapp.vadret.core.resources.wsymb2_thunderstorm
import se.vadretapp.vadret.core.resources.wsymb2_variable_cloudiness
import se.vadretapp.vadret.icons.ClearSky
import se.vadretapp.vadret.icons.ClearSkyNight
import se.vadretapp.vadret.icons.CloudySky
import se.vadretapp.vadret.icons.Fog
import se.vadretapp.vadret.icons.HalfClearSky
import se.vadretapp.vadret.icons.HalfClearSkyNight
import se.vadretapp.vadret.icons.HeavyRain
import se.vadretapp.vadret.icons.HeavyRainShowers
import se.vadretapp.vadret.icons.HeavySleet
import se.vadretapp.vadret.icons.HeavySleetShowers
import se.vadretapp.vadret.icons.HeavySnowShowers
import se.vadretapp.vadret.icons.HeavySnowfall
import se.vadretapp.vadret.icons.LightRain
import se.vadretapp.vadret.icons.LightRainShowers
import se.vadretapp.vadret.icons.LightSleet
import se.vadretapp.vadret.icons.LightSleetShowers
import se.vadretapp.vadret.icons.LightSnowShowers
import se.vadretapp.vadret.icons.LightSnowfall
import se.vadretapp.vadret.icons.ModerateRain
import se.vadretapp.vadret.icons.ModerateRainShowers
import se.vadretapp.vadret.icons.ModerateSleet
import se.vadretapp.vadret.icons.ModerateSleetShowers
import se.vadretapp.vadret.icons.ModerateSnowShowers
import se.vadretapp.vadret.icons.ModerateSnowfall
import se.vadretapp.vadret.icons.NearlyClearSky
import se.vadretapp.vadret.icons.NearlyClearSkyNight
import se.vadretapp.vadret.icons.Overcast
import se.vadretapp.vadret.icons.Thunder
import se.vadretapp.vadret.icons.Thunderstorm
import se.vadretapp.vadret.icons.VadretIconGraphic
import se.vadretapp.vadret.icons.VadretIcons
import se.vadretapp.vadret.icons.VariableCloudiness
import se.vadretapp.vadret.icons.VariableCloudinessNight
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.weatherforecast")

internal object WeatherForecastUtil {

    fun getWeatherSymbolIcon(
        symbolCode: Int,
        isDaytime: Boolean
    ): VadretIconGraphic =
        when (symbolCode) {
            1 -> if (isDaytime) {
                VadretIcons.Weather.ClearSky
            } else {
                VadretIcons.Weather.ClearSkyNight
            }
            2 -> if (isDaytime) {
                VadretIcons.Weather.NearlyClearSky
            } else {
                VadretIcons.Weather.NearlyClearSkyNight
            }
            3 -> if (isDaytime) {
                VadretIcons.Weather.VariableCloudiness
            } else {
                VadretIcons.Weather.VariableCloudinessNight
            }
            4 -> if (isDaytime) {
                VadretIcons.Weather.HalfClearSky
            } else {
                VadretIcons.Weather.HalfClearSkyNight
            }
            5 -> VadretIcons.Weather.CloudySky
            6 -> VadretIcons.Weather.Overcast
            7 -> VadretIcons.Weather.Fog
            8 -> VadretIcons.Weather.LightRainShowers
            9 -> VadretIcons.Weather.ModerateRainShowers
            10 -> VadretIcons.Weather.HeavyRainShowers
            11 -> VadretIcons.Weather.Thunderstorm
            12 -> VadretIcons.Weather.LightSleetShowers
            13 -> VadretIcons.Weather.ModerateSleetShowers
            14 -> VadretIcons.Weather.HeavySleetShowers
            15 -> VadretIcons.Weather.LightSnowShowers
            16 -> VadretIcons.Weather.ModerateSnowShowers
            17 -> VadretIcons.Weather.HeavySnowShowers
            18 -> VadretIcons.Weather.LightRain
            19 -> VadretIcons.Weather.ModerateRain
            20 -> VadretIcons.Weather.HeavyRain
            21 -> VadretIcons.Weather.Thunder
            22 -> VadretIcons.Weather.LightSleet
            23 -> VadretIcons.Weather.ModerateSleet
            24 -> VadretIcons.Weather.HeavySleet
            25 -> VadretIcons.Weather.LightSnowfall
            26 -> VadretIcons.Weather.ModerateSnowfall
            27 -> VadretIcons.Weather.HeavySnowfall
            else -> {
                logger.warn { "Unknown weather symbol $symbolCode" }
                VadretIcons.Weather.Overcast
            }
        }

    fun getWsymb2Resource(symbolCode: Int): StringResource =
        when (symbolCode) {
            1 -> Res.string.wsymb2_clear_sky
            2 -> Res.string.wsymb2_nearly_clear_sky
            3 -> Res.string.wsymb2_variable_cloudiness
            4 -> Res.string.wsymb2_halfclear_sky
            5 -> Res.string.wsymb2_cloudy_sky
            6 -> Res.string.wsymb2_overcast
            7 -> Res.string.wsymb2_fog
            8 -> Res.string.wsymb2_light_rain_showers
            9 -> Res.string.wsymb2_moderate_rain_showers
            10 -> Res.string.wsymb2_heavy_rain_showers
            11 -> Res.string.wsymb2_thunderstorm
            12 -> Res.string.wsymb2_light_sleet_showers
            13 -> Res.string.wsymb2_moderate_sleet_showers
            14 -> Res.string.wsymb2_heavy_sleet_showers
            15 -> Res.string.wsymb2_light_snow_showers
            16 -> Res.string.wsymb2_moderate_snow_showers
            17 -> Res.string.wsymb2_heavy_snow_showers
            18 -> Res.string.wsymb2_light_rain
            19 -> Res.string.wsymb2_moderate_rain
            20 -> Res.string.wsymb2_heavy_rain
            21 -> Res.string.wsymb2_thunder
            22 -> Res.string.wsymb2_light_sleet
            23 -> Res.string.wsymb2_moderate_sleet
            24 -> Res.string.wsymb2_heavy_sleet
            25 -> Res.string.wsymb2_light_snowfall
            26 -> Res.string.wsymb2_moderate_snowfall
            27 -> Res.string.wsymb2_heavy_snowfall
            else -> {
                Res.string.wsymb2_clear_sky
            }
        }

    fun handleWindDirection(degree: Double): StringResource =
        when {
            // North: 0.0 - 11.24
            (degree <= 11.25 && 0.0 <= degree) -> Res.string.north
            // North, North East: 11.25 - 33.74
            (degree <= 33.75 && 11.25 < degree) -> Res.string.north_north_east
            // North East:  33.75 - 56.24
            (degree <= 56.25 && 33.75 < degree) -> Res.string.north_east
            // East, North East:
            (degree <= 78.75 && 56.25 < degree) -> Res.string.east_north_east
            // East:
            (degree <= 101.25 && 78.75 < degree) -> Res.string.east
            // East, South East:
            (degree <= 123.75 && 101.25 < degree) -> Res.string.east_south_east
            // South East
            (degree <= 146.25 && 123.75 < degree) -> Res.string.south_east
            // South, South East
            (degree <= 168.75 && 146.25 < degree) -> Res.string.south_south_east
            // South
            (degree <= 191.25 && 168.75 < degree) -> Res.string.south
            // South, South West
            (degree <= 213.75 && 191.25 < degree) -> Res.string.south_south_west
            // South West
            (degree <= 236.25 && 213.75 < degree) -> Res.string.south_west
            // West, South West
            (degree <= 258.75 && 236.25 < degree) -> Res.string.west_south_west
            // West
            (degree <= 281.25 && 258.75 < degree) -> Res.string.west
            // West, North West
            (degree <= 303.75 && 281.25 < degree) -> Res.string.west_north_west
            // North West
            (degree <= 326.25 && 303.75 < degree) -> Res.string.north_west
            // North, North West
            (degree <= 348.75 && 326.25 < degree) -> Res.string.north_north_west
            // North: 348.75 - 360.0
            (degree <= 360 && 348.75 < degree) -> Res.string.north
            else -> {
                logger.warn { "Wind direction outside 0 to 360: $degree" }
                Res.string.empty_wind_direction
            }
        }
}