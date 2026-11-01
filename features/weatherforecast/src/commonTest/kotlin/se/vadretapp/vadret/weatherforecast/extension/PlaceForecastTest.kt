package se.vadretapp.vadret.weatherforecast.extension

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.wsymb2_overcast
import se.vadretapp.vadret.icons.ClearSky
import se.vadretapp.vadret.icons.ClearSkyNight
import se.vadretapp.vadret.icons.Overcast
import se.vadretapp.vadret.icons.VadretIconGraphic
import se.vadretapp.vadret.icons.VadretIcons
import se.vadretapp.vadret.icons.VariableCloudiness
import se.vadretapp.vadret.icons.VariableCloudinessNight
import se.vadretapp.vadret.weatherforecast.model.Analysis
import se.vadretapp.vadret.weatherforecast.model.CurrentWeather
import se.vadretapp.vadret.weatherforecast.model.Forecast
import se.vadretapp.vadret.weatherforecast.model.ForecastDay
import se.vadretapp.vadret.weatherforecast.model.ForecastHour
import se.vadretapp.vadret.weatherforecast.model.PlaceForecast
import se.vadretapp.vadret.weatherforecast.model.WeatherForecast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private val stockholm: TimeZone = TimeZone.of(zoneId = "Europe/Stockholm")

private fun forecast(
    validAt: Instant,
    symbolCode: Int = 6,
    windFromDirection: Double = 160.0,
    airTemperature: Double = 10.9
): Forecast =
    Forecast(
        validAt = validAt,
        intervalStartedAt = validAt - 1.hours,
        airTemperature = airTemperature,
        windFromDirection = windFromDirection,
        windSpeed = 5.4,
        relativeHumidity = 86,
        precipitationType = 0,
        symbolCode = symbolCode,
        precipitationAmount = 0.0
    )

private fun analysis(
    analysedAt: Instant,
    airTemperature: Double = 10.9
): Analysis =
    Analysis(
        analysedAt = analysedAt,
        airTemperature = airTemperature,
        windFromDirection = 160.0,
        windSpeed = 5.4,
        relativeHumidity = 86,
        precipitationType = 0,
        symbolCode = 6,
        precipitationAmountLastHour = 0.0
    )

class PlaceForecastTest {

    private fun weatherForecast(
        forecasts: List<Forecast>,
        analyses: List<Analysis> = emptyList(),
        languageTag: String = "en",
        now: Instant = Instant.parse(input = "2026-10-24T12:00:00Z")
    ): WeatherForecast =
        PlaceForecast(
            placeName = "Stockholm",
            municipality = "Stockholm",
            county = "Stockholm",
            latitude = 59.33,
            longitude = 18.07,
            forecasts = forecasts,
            analyses = analyses
        ).toWeatherForecast(
            now = now,
            timeZone = stockholm,
            languageTag = languageTag
        )

    private fun currentWeather(forecast: Forecast): CurrentWeather {
        val currentWeather: CurrentWeather? = weatherForecast(forecasts = listOf(forecast)).current
        assertNotNull(actual = currentWeather)
        return currentWeather
    }

    @Test
    fun `hours are grouped by local date`() {
        val days: List<ForecastDay> = weatherForecast(
            forecasts = listOf(
                forecast(validAt = Instant.parse(input = "2026-10-24T20:00:00Z")),
                forecast(validAt = Instant.parse(input = "2026-10-24T21:00:00Z")),
                forecast(validAt = Instant.parse(input = "2026-10-24T22:00:00Z"))
            )
        ).days

        assertEquals(
            expected = listOf("day-2026-10-24", "day-2026-10-25"),
            actual = days.map { day: ForecastDay -> day.key }
        )
        assertEquals(
            expected = listOf(
                listOf("hour-2026-10-24T20:00:00Z", "hour-2026-10-24T21:00:00Z"),
                listOf("hour-2026-10-24T22:00:00Z")
            ),
            actual = days.map { day: ForecastDay -> day.hours.map { hour: ForecastHour -> hour.key } }
        )
        assertEquals(expected = "00:00", actual = days.last().hours.single().timeText)
    }

    @Test
    fun `every weekday gets its own capitalised header`() {
        val monday = LocalDate(year = 2026, month = 10, day = 19)
        val dates: List<LocalDate> = (0 until DayOfWeek.entries.size).map { offset: Int -> monday.plus(period = DatePeriod(days = offset)) }

        val days: List<ForecastDay> = weatherForecast(
            forecasts = dates.map { date: LocalDate -> forecast(validAt = date.atTime(hour = 12, minute = 0).toInstant(timeZone = stockholm)) }
        ).days

        assertEquals(
            expected = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"),
            actual = days.map { day: ForecastDay -> day.weekday }
        )
        assertEquals(
            expected = (19..25).map { day: Int -> "October $day, 2026" },
            actual = days.map { day: ForecastDay -> day.dateText }
        )
    }

    @Test
    fun `swedish weekdays start with a capital letter`() {
        val days: List<ForecastDay> = weatherForecast(
            forecasts = listOf(forecast(validAt = Instant.parse(input = "2026-10-25T12:00:00Z"))),
            languageTag = "sv-SE"
        ).days

        assertEquals(expected = "Söndag", actual = days.single().weekday)
    }

    @Test
    fun `the current weather describes the first forecast`() {
        val currentWeather: CurrentWeather = currentWeather(forecast = forecast(validAt = Instant.parse(input = "2026-10-24T12:00:00Z")))

        assertEquals(expected = "10.9", actual = currentWeather.temperatureText)
        assertEquals(expected = Res.string.wsymb2_overcast, actual = currentWeather.descriptionResource)
        assertEquals(expected = VadretIcons.Weather.Overcast, actual = currentWeather.icon)
        assertEquals(expected = 86, actual = currentWeather.humidityPercent)
        assertEquals(expected = 5.4, actual = currentWeather.windSpeed)
    }

    @Test
    fun `the current weather describes the hour closest to now`() {
        val analysedHour: Analysis = analysis(
            analysedAt = Instant.parse(input = "2026-10-24T12:00:00Z"),
            airTemperature = 9.0
        )
        val forecastHour: Forecast = forecast(
            validAt = Instant.parse(input = "2026-10-24T13:00:00Z"),
            airTemperature = 11.0
        )

        val temperatures: List<String?> = listOf("2026-10-24T12:29:00Z", "2026-10-24T12:31:00Z").map { now: String ->
            weatherForecast(
                forecasts = listOf(forecastHour),
                analyses = listOf(analysedHour),
                now = Instant.parse(input = now)
            ).current?.temperatureText
        }

        assertEquals(expected = listOf("9", "11"), actual = temperatures)
    }

    @Test
    fun `the current temperatures drop a trailing zero`() {
        val mild: CurrentWeather = currentWeather(
            forecast = forecast(
                validAt = Instant.parse(input = "2026-10-24T12:00:00Z"),
                airTemperature = 11.0
            )
        )
        val cold: CurrentWeather = currentWeather(
            forecast = forecast(
                validAt = Instant.parse(input = "2026-10-24T12:00:00Z"),
                airTemperature = -10.0
            )
        )

        assertEquals(
            expected = listOf("11" to "11", "-10" to "-17.7"),
            actual = listOf(mild, cold).map { currentWeather: CurrentWeather ->
                currentWeather.temperatureText to currentWeather.feelsLikeTemperatureText
            }
        )
    }

    @Test
    fun `the hour list only holds forecast hours`() {
        val hours: List<ForecastHour> = weatherForecast(
            forecasts = listOf(forecast(validAt = Instant.parse(input = "2026-10-24T13:00:00Z"))),
            analyses = listOf(analysis(analysedAt = Instant.parse(input = "2026-10-24T11:00:00Z")))
        ).days.flatMap { day: ForecastDay -> day.hours }

        assertEquals(
            expected = listOf("hour-2026-10-24T13:00:00Z"),
            actual = hours.map { hour: ForecastHour -> hour.key }
        )
    }

    @Test
    fun `the wind arrow points where the wind blows`() {
        assertEquals(
            expected = 340f,
            actual = currentWeather(forecast = forecast(validAt = Instant.parse(input = "2026-10-24T12:00:00Z"))).windArrowRotation
        )
        assertEquals(
            expected = 112f,
            actual = currentWeather(
                forecast = forecast(
                    validAt = Instant.parse(input = "2026-10-24T12:00:00Z"),
                    windFromDirection = 292.0
                )
            ).windArrowRotation
        )
    }

    @Test
    fun `clear sky shows the sun by day and the moon by night`() {
        val hours: List<ForecastHour> = weatherForecast(
            forecasts = listOf(
                forecast(validAt = Instant.parse(input = "2026-10-24T10:00:00Z"), symbolCode = 1),
                forecast(validAt = Instant.parse(input = "2026-10-24T20:00:00Z"), symbolCode = 1),
                forecast(validAt = Instant.parse(input = "2026-10-24T10:00:00Z"), symbolCode = 3),
                forecast(validAt = Instant.parse(input = "2026-10-24T20:00:00Z"), symbolCode = 3)
            )
        ).days.flatMap { day: ForecastDay -> day.hours }

        assertEquals(
            expected = listOf(
                VadretIcons.Weather.ClearSky,
                VadretIcons.Weather.ClearSkyNight,
                VadretIcons.Weather.VariableCloudiness,
                VadretIcons.Weather.VariableCloudinessNight
            ),
            actual = hours.map { hour: ForecastHour -> hour.icon }
        )
    }

    @Test
    fun `cloud and precipitation symbols look the same by day and night`() {
        val symbolCodes: List<Int> = (5..27).toList()

        val dayIcons: List<VadretIconGraphic> = weatherForecast(
            forecasts = symbolCodes.map { symbolCode: Int ->
                forecast(
                    validAt = Instant.parse(input = "2026-10-24T10:00:00Z"),
                    symbolCode = symbolCode
                )
            }
        ).days.flatMap { day: ForecastDay -> day.hours.map { hour: ForecastHour -> hour.icon } }
        val nightIcons: List<VadretIconGraphic> = weatherForecast(
            forecasts = symbolCodes.map { symbolCode: Int ->
                forecast(
                    validAt = Instant.parse(input = "2026-10-24T20:00:00Z"),
                    symbolCode = symbolCode
                )
            }
        ).days.flatMap { day: ForecastDay -> day.hours.map { hour: ForecastHour -> hour.icon } }

        assertEquals(expected = dayIcons, actual = nightIcons)
    }

    @Test
    fun `no forecasts give no weather`() {
        val weatherForecast: WeatherForecast = weatherForecast(forecasts = emptyList())

        assertNull(actual = weatherForecast.current)
        assertEquals(expected = emptyList(), actual = weatherForecast.days)
        assertEquals(expected = emptyList(), actual = weatherForecast.comingDays)
    }
}