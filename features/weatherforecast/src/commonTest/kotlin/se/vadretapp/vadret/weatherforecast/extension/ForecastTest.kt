package se.vadretapp.vadret.weatherforecast.extension

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import se.vadretapp.vadret.icons.ClearSky
import se.vadretapp.vadret.icons.ClearSkyNight
import se.vadretapp.vadret.icons.Overcast
import se.vadretapp.vadret.icons.VadretIcons
import se.vadretapp.vadret.icons.VariableCloudiness
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.west
import se.vadretapp.vadret.ui.resources.wsymb2_overcast
import se.vadretapp.vadret.weatherforecast.model.Analysis
import se.vadretapp.vadret.weatherforecast.model.ComingDay
import se.vadretapp.vadret.weatherforecast.model.ComingHour
import se.vadretapp.vadret.weatherforecast.model.Forecast
import se.vadretapp.vadret.weatherforecast.model.PlaceForecast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private val stockholm: TimeZone = TimeZone.of(zoneId = "Europe/Stockholm")
private const val RANGE_TOLERANCE = 0.0001f
private const val NOVEMBER = 11
private const val KIRUNA_LATITUDE = 67.85
private const val KIRUNA_LONGITUDE = 20.23

private fun localTime(
    month: Int = NOVEMBER,
    day: Int,
    hour: Int,
    minute: Int = 0
): Instant =
    LocalDate(
        year = 2026,
        month = month,
        day = day
    ).atTime(
        hour = hour,
        minute = minute
    ).toInstant(timeZone = stockholm)

private fun forecast(
    validAt: Instant,
    intervalStartedAt: Instant = validAt - 1.hours,
    airTemperature: Double = 5.0,
    windFromDirection: Double = 180.0,
    windSpeed: Double = 3.0,
    symbolCode: Int = 6,
    precipitationAmount: Double? = 0.0
): Forecast =
    Forecast(
        validAt = validAt,
        intervalStartedAt = intervalStartedAt,
        airTemperature = airTemperature,
        windFromDirection = windFromDirection,
        windSpeed = windSpeed,
        relativeHumidity = 80,
        precipitationType = 0,
        symbolCode = symbolCode,
        precipitationAmount = precipitationAmount
    )

private fun analysis(
    analysedAt: Instant,
    airTemperature: Double = 5.0,
    symbolCode: Int = 6,
    precipitationAmountLastHour: Double? = 0.0
): Analysis =
    Analysis(
        analysedAt = analysedAt,
        airTemperature = airTemperature,
        windFromDirection = 180.0,
        windSpeed = 3.0,
        relativeHumidity = 80,
        precipitationType = 0,
        symbolCode = symbolCode,
        precipitationAmountLastHour = precipitationAmountLastHour
    )

class ForecastTest {

    private fun comingDays(
        forecasts: List<Forecast>,
        analyses: List<Analysis> = emptyList(),
        now: Instant = localTime(
            day = 10,
            hour = 10
        ),
        languageTag: String = "en",
        hourFormat: HourFormat = HourFormat.TWENTY_FOUR_HOUR,
        latitude: Double = 59.33,
        longitude: Double = 18.07
    ): List<ComingDay> =
        PlaceForecast(
            placeName = "Stockholm",
            municipality = "Stockholm",
            county = "Stockholm",
            latitude = latitude,
            longitude = longitude,
            forecasts = forecasts,
            analyses = analyses,
            fetchedAt = null,
            isRefreshFailed = false
        ).toWeatherForecast(
            now = now,
            timeZone = stockholm,
            languageTag = languageTag,
            hourFormat = hourFormat
        ).comingDays

    @Test
    fun `the hours of the coming days follow the 12 hour clock with AM and PM whatever the language`() {
        val forecasts: List<Forecast> = listOf(
            forecast(validAt = localTime(day = 10, hour = 11)),
            forecast(validAt = localTime(day = 10, hour = 12)),
            forecast(validAt = localTime(day = 11, hour = 6)),
            forecast(validAt = localTime(day = 11, hour = 12))
        )

        assertEquals(
            expected = listOf("6 AM", "12 PM") to listOf("6 AM", "12 PM"),
            actual = listOf("en", "sv-SE").map { languageTag: String ->
                comingDays(
                    forecasts = forecasts,
                    languageTag = languageTag,
                    hourFormat = HourFormat.TWELVE_HOUR
                ).last().hours.map { comingHour: ComingHour -> comingHour.timeText }
            }.zipWithNext().single()
        )
    }

    @Test
    fun `days are grouped by local date and only the first hour of today is now`() {
        val days: List<ComingDay> = comingDays(
            forecasts = listOf(
                forecast(validAt = localTime(day = 10, hour = 11)),
                forecast(validAt = localTime(day = 10, hour = 12)),
                forecast(validAt = localTime(day = 11, hour = 6)),
                forecast(validAt = localTime(day = 11, hour = 12))
            )
        )

        assertEquals(
            expected = listOf("coming-day-2026-11-10", "coming-day-2026-11-11"),
            actual = days.map { comingDay: ComingDay -> comingDay.key }
        )
        assertEquals(
            expected = listOf(true, false),
            actual = days.map { comingDay: ComingDay -> comingDay.isToday }
        )
        assertEquals(
            expected = listOf(false, false),
            actual = days.map { comingDay: ComingDay -> comingDay.isExpanded }
        )
        assertEquals(
            expected = listOf(listOf(true, false), listOf(false, false)),
            actual = days.map { comingDay: ComingDay ->
                comingDay.hours.map { comingHour: ComingHour -> comingHour.isNow }
            }
        )
        assertEquals(
            expected = listOf("coming-hour-2026-11-10T10:00:00Z", "coming-hour-2026-11-10T11:00:00Z"),
            actual = days.first().hours.map { comingHour: ComingHour -> comingHour.key }
        )
        assertEquals(
            expected = listOf("06", "12"),
            actual = days.last().hours.map { comingHour: ComingHour -> comingHour.timeText }
        )
    }

    @Test
    fun `a trailing day without daytime hours is left out`() {
        val days: List<ComingDay> = comingDays(
            forecasts = listOf(
                forecast(validAt = localTime(day = 10, hour = 12)),
                forecast(validAt = localTime(day = 11, hour = 0))
            )
        )

        assertEquals(
            expected = listOf("coming-day-2026-11-10"),
            actual = days.map { comingDay: ComingDay -> comingDay.key }
        )
    }

    @Test
    fun `no day is left when none is today and none has daytime hours`() {
        val days: List<ComingDay> = comingDays(
            forecasts = listOf(forecast(validAt = localTime(day = 11, hour = 0)))
        )

        assertEquals(expected = emptyList(), actual = days)
    }

    @Test
    fun `today is kept even late in the evening`() {
        val days: List<ComingDay> = comingDays(
            forecasts = listOf(
                forecast(validAt = localTime(day = 10, hour = 22)),
                forecast(validAt = localTime(day = 10, hour = 23)),
                forecast(validAt = localTime(day = 11, hour = 12))
            ),
            now = localTime(
                day = 10,
                hour = 21
            )
        )

        assertEquals(
            expected = listOf(true, false),
            actual = days.map { comingDay: ComingDay -> comingDay.isToday }
        )
    }

    @Test
    fun `precipitation belongs to the day its interval falls in`() {
        val days: List<ComingDay> = comingDays(
            forecasts = listOf(
                forecast(validAt = localTime(day = 10, hour = 12), precipitationAmount = 0.5),
                forecast(validAt = localTime(day = 10, hour = 18), precipitationAmount = 1.0),
                forecast(validAt = localTime(day = 11, hour = 0), precipitationAmount = 0.3),
                forecast(validAt = localTime(day = 11, hour = 6), precipitationAmount = 0.05),
                forecast(validAt = localTime(day = 11, hour = 12), precipitationAmount = 0.0)
            )
        )

        assertEquals(
            expected = listOf("1.8", null),
            actual = days.map { comingDay: ComingDay -> comingDay.precipitationText }
        )
        assertEquals(
            expected = listOf(listOf("0.5", "1.0"), listOf("0.3", null, null)),
            actual = days.map { comingDay: ComingDay ->
                comingDay.hours.map { comingHour: ComingHour -> comingHour.precipitationText }
            }
        )
    }

    @Test
    fun `a long interval that ends after midnight counts for the day it mostly covers`() {
        val days: List<ComingDay> = comingDays(
            forecasts = listOf(
                forecast(
                    validAt = localTime(day = 10, hour = 13),
                    intervalStartedAt = localTime(day = 10, hour = 1),
                    precipitationAmount = 0.4
                ),
                forecast(
                    validAt = localTime(day = 11, hour = 1),
                    intervalStartedAt = localTime(day = 10, hour = 13),
                    precipitationAmount = 2.0
                ),
                forecast(
                    validAt = localTime(day = 11, hour = 13),
                    intervalStartedAt = localTime(day = 11, hour = 1),
                    precipitationAmount = 0.5
                )
            )
        )

        assertEquals(
            expected = listOf("2.4", "0.5"),
            actual = days.map { comingDay: ComingDay -> comingDay.precipitationText }
        )
    }

    @Test
    fun `an unknown amount makes the day total unknown`() {
        val days: List<ComingDay> = comingDays(
            forecasts = listOf(
                forecast(validAt = localTime(day = 10, hour = 12), precipitationAmount = 3.0),
                forecast(validAt = localTime(day = 10, hour = 13), precipitationAmount = null),
                forecast(validAt = localTime(day = 11, hour = 12), precipitationAmount = 1.0)
            )
        )

        assertEquals(
            expected = listOf(null, "1.0"),
            actual = days.map { comingDay: ComingDay -> comingDay.precipitationText }
        )
        assertEquals(
            expected = listOf("3.0", null),
            actual = days.first().hours.map { comingHour: ComingHour -> comingHour.precipitationText }
        )
    }

    @Test
    fun `temperature ranges share one scale across all days`() {
        val days: List<ComingDay> = comingDays(
            forecasts = listOf(
                forecast(validAt = localTime(day = 10, hour = 12), airTemperature = 2.4),
                forecast(validAt = localTime(day = 10, hour = 15), airTemperature = 7.6),
                forecast(validAt = localTime(day = 11, hour = 6), airTemperature = -1.6),
                forecast(validAt = localTime(day = 11, hour = 12), airTemperature = 4.0)
            )
        )

        assertEquals(
            expected = listOf("2" to "8", "-2" to "4"),
            actual = days.map { comingDay: ComingDay ->
                comingDay.lowTemperatureText to comingDay.highTemperatureText
            }
        )
        assertEquals(
            expected = 4.0f / 9.2f,
            actual = days.first().temperatureRangeStart,
            absoluteTolerance = RANGE_TOLERANCE
        )
        assertEquals(
            expected = 1f,
            actual = days.first().temperatureRangeEnd,
            absoluteTolerance = RANGE_TOLERANCE
        )
        assertEquals(
            expected = 0f,
            actual = days.last().temperatureRangeStart,
            absoluteTolerance = RANGE_TOLERANCE
        )
        assertEquals(
            expected = 5.6f / 9.2f,
            actual = days.last().temperatureRangeEnd,
            absoluteTolerance = RANGE_TOLERANCE
        )
    }

    @Test
    fun `an unchanging temperature fills the whole range`() {
        val day: ComingDay = comingDays(
            forecasts = listOf(
                forecast(validAt = localTime(day = 10, hour = 12)),
                forecast(validAt = localTime(day = 10, hour = 13))
            )
        ).single()

        assertEquals(
            expected = 0f to 1f,
            actual = day.temperatureRangeStart to day.temperatureRangeEnd
        )
    }

    @Test
    fun `the day icon is the most common daytime weather`() {
        val day: ComingDay = comingDays(
            forecasts = listOf(
                forecast(validAt = localTime(day = 10, hour = 3), symbolCode = 1),
                forecast(validAt = localTime(day = 10, hour = 4), symbolCode = 1),
                forecast(validAt = localTime(day = 10, hour = 6), symbolCode = 6),
                forecast(validAt = localTime(day = 10, hour = 12), symbolCode = 3),
                forecast(validAt = localTime(day = 10, hour = 18), symbolCode = 3)
            )
        ).single()

        assertEquals(expected = VadretIcons.Weather.VariableCloudiness, actual = day.icon)
    }

    @Test
    fun `a tie in daytime weather goes to the worse weather`() {
        val day: ComingDay = comingDays(
            forecasts = listOf(
                forecast(validAt = localTime(day = 10, hour = 9), symbolCode = 3),
                forecast(validAt = localTime(day = 10, hour = 12), symbolCode = 6)
            )
        ).single()

        assertEquals(expected = VadretIcons.Weather.Overcast, actual = day.icon)
        assertEquals(expected = Res.string.wsymb2_overcast, actual = day.descriptionResource)
    }

    @Test
    fun `a late evening today without analysed hours takes its icon from the hours that are left`() {
        val today: ComingDay = comingDays(
            forecasts = listOf(
                forecast(validAt = localTime(day = 10, hour = 20), symbolCode = 3),
                forecast(validAt = localTime(day = 10, hour = 21), symbolCode = 3),
                forecast(validAt = localTime(day = 10, hour = 22), symbolCode = 6)
            ),
            now = localTime(
                day = 10,
                hour = 19
            )
        ).single()

        assertEquals(expected = VadretIcons.Weather.VariableCloudiness, actual = today.icon)
    }

    @Test
    fun `hours show the moon at night and the sun by day`() {
        val day: ComingDay = comingDays(
            forecasts = listOf(
                forecast(validAt = localTime(day = 10, hour = 3), symbolCode = 1),
                forecast(validAt = localTime(day = 10, hour = 12), symbolCode = 1)
            )
        ).single()

        assertEquals(
            expected = listOf(VadretIcons.Weather.ClearSkyNight, VadretIcons.Weather.ClearSky),
            actual = day.hours.map { comingHour: ComingHour -> comingHour.icon }
        )
    }

    @Test
    fun `hour temperatures sit between the coldest and warmest hour of the day`() {
        val day: ComingDay = comingDays(
            forecasts = listOf(
                forecast(validAt = localTime(day = 10, hour = 10), airTemperature = 2.0),
                forecast(validAt = localTime(day = 10, hour = 11), airTemperature = 6.0),
                forecast(validAt = localTime(day = 10, hour = 12), airTemperature = 10.0)
            )
        ).single()

        assertEquals(
            expected = listOf(0f, 0.5f, 1f),
            actual = day.hours.map { comingHour: ComingHour -> comingHour.temperatureLevel }
        )
    }

    @Test
    fun `a day with little temperature change is centred on four degrees`() {
        val day: ComingDay = comingDays(
            forecasts = listOf(
                forecast(validAt = localTime(day = 10, hour = 10), airTemperature = 5.0),
                forecast(validAt = localTime(day = 10, hour = 11), airTemperature = 6.0)
            )
        ).single()

        assertEquals(
            expected = listOf(0.375f, 0.625f),
            actual = day.hours.map { comingHour: ComingHour -> comingHour.temperatureLevel }
        )
    }

    @Test
    fun `wind reaches the top of its chart at twelve metres per second`() {
        val day: ComingDay = comingDays(
            forecasts = listOf(0.0, 6.0, 12.0, 15.0).mapIndexed { index: Int, windSpeed: Double ->
                forecast(
                    validAt = localTime(day = 10, hour = 10 + index),
                    windSpeed = windSpeed
                )
            }
        ).single()

        assertEquals(
            expected = listOf(0f, 0.5f, 1f, 1f),
            actual = day.hours.map { comingHour: ComingHour -> comingHour.windLevel }
        )
        assertEquals(
            expected = listOf("0.0", "6.0", "12.0", "15.0"),
            actual = day.hours.map { comingHour: ComingHour -> comingHour.windSpeedText }
        )
    }

    @Test
    fun `the wind arrow points where a west wind blows`() {
        val hour: ComingHour = comingDays(
            forecasts = listOf(
                forecast(
                    validAt = localTime(day = 10, hour = 12),
                    windFromDirection = 270.0
                )
            )
        ).single().hours.single()

        assertEquals(expected = 90f, actual = hour.windArrowRotation)
        assertEquals(expected = Res.string.west, actual = hour.windDirectionResource)
    }

    @Test
    fun `today starts at the current hour`() {
        val today: ComingDay = comingDays(
            forecasts = (3..23).map { hour: Int -> forecast(validAt = localTime(day = 10, hour = hour)) },
            now = localTime(day = 10, hour = 3)
        ).single()

        assertEquals(expected = 0, actual = today.firstShownHourIndex)
    }

    @Test
    fun `today starts at midnight with the hours already analysed`() {
        val today: ComingDay = comingDays(
            forecasts = (11..23).map { hour: Int -> forecast(validAt = localTime(day = 10, hour = hour)) },
            analyses = listOf(
                analysis(analysedAt = localTime(day = 9, hour = 22)),
                analysis(analysedAt = localTime(day = 9, hour = 23))
            ) + (0..10).map { hour: Int -> analysis(analysedAt = localTime(day = 10, hour = hour)) }
        ).single()

        assertEquals(
            expected = (0..23).map { hour: Int -> hour.toString().padStart(length = 2, padChar = '0') },
            actual = today.hours.map { comingHour: ComingHour -> comingHour.timeText }
        )
    }

    @Test
    fun `a forecast hour wins over an analysis of the same hour`() {
        val today: ComingDay = comingDays(
            forecasts = listOf(
                forecast(
                    validAt = localTime(day = 10, hour = 11),
                    airTemperature = 6.0
                )
            ),
            analyses = listOf(
                analysis(
                    analysedAt = localTime(day = 10, hour = 10),
                    airTemperature = 4.0
                ),
                analysis(
                    analysedAt = localTime(day = 10, hour = 11),
                    airTemperature = 9.0
                )
            )
        ).single()

        assertEquals(
            expected = listOf("4", "6"),
            actual = today.hours.map { comingHour: ComingHour -> comingHour.temperatureText }
        )
    }

    @Test
    fun `the hour closest to now is now and shown first with a tie going to the earlier hour`() {
        val nowHours: List<Pair<Int, List<Boolean>>> = listOf(29, 30, 31).map { minute: Int ->
            val today: ComingDay = comingDays(
                forecasts = (11..13).map { hour: Int -> forecast(validAt = localTime(day = 10, hour = hour)) },
                analyses = (8..10).map { hour: Int -> analysis(analysedAt = localTime(day = 10, hour = hour)) },
                now = localTime(
                    day = 10,
                    hour = 10,
                    minute = minute
                )
            ).single()
            today.firstShownHourIndex to today.hours.map { comingHour: ComingHour -> comingHour.isNow }
        }

        assertEquals(
            expected = listOf(
                2 to listOf(false, false, true, false, false, false),
                2 to listOf(false, false, true, false, false, false),
                3 to listOf(false, false, false, true, false, false)
            ),
            actual = nowHours
        )
    }

    @Test
    fun `just before midnight the now hour is the first hour of tomorrow`() {
        val days: List<ComingDay> = comingDays(
            forecasts = (0..23).map { hour: Int -> forecast(validAt = localTime(day = 11, hour = hour)) },
            analyses = (0..23).map { hour: Int -> analysis(analysedAt = localTime(day = 10, hour = hour)) },
            now = localTime(
                day = 10,
                hour = 23,
                minute = 45
            )
        )

        assertEquals(
            expected = listOf(23, 0),
            actual = days.map { comingDay: ComingDay -> comingDay.firstShownHourIndex }
        )
        assertEquals(
            expected = listOf(emptyList(), listOf("00")),
            actual = days.map { comingDay: ComingDay ->
                comingDay.hours
                    .filter { comingHour: ComingHour -> comingHour.isNow }
                    .map { comingHour: ComingHour -> comingHour.timeText }
            }
        )
    }

    @Test
    fun `an hour more than an hour away is not now`() {
        val days: List<ComingDay> = comingDays(
            forecasts = listOf(
                forecast(validAt = localTime(day = 10, hour = 12)),
                forecast(validAt = localTime(day = 10, hour = 13))
            ),
            now = localTime(
                day = 10,
                hour = 10
            )
        )

        assertEquals(
            expected = listOf(0 to listOf(false, false)),
            actual = days.map { comingDay: ComingDay ->
                comingDay.firstShownHourIndex to comingDay.hours.map { comingHour: ComingHour -> comingHour.isNow }
            }
        )
    }

    @Test
    fun `today takes its icon from the analysed daytime hours too`() {
        val today: ComingDay = comingDays(
            forecasts = (20..23).map { hour: Int ->
                forecast(
                    validAt = localTime(day = 10, hour = hour),
                    symbolCode = 6
                )
            },
            analyses = (8..18).map { hour: Int ->
                analysis(
                    analysedAt = localTime(day = 10, hour = hour),
                    symbolCode = 1
                )
            },
            now = localTime(
                day = 10,
                hour = 19
            )
        ).single()

        assertEquals(expected = VadretIcons.Weather.ClearSky, actual = today.icon)
    }

    @Test
    fun `the analysed hours count towards the whole of today`() {
        val today: ComingDay = comingDays(
            forecasts = listOf(
                forecast(
                    validAt = localTime(day = 10, hour = 11),
                    airTemperature = 6.0,
                    precipitationAmount = 0.5
                )
            ),
            analyses = listOf(
                analysis(
                    analysedAt = localTime(day = 10, hour = 5),
                    airTemperature = -2.0,
                    precipitationAmountLastHour = 1.0
                ),
                analysis(
                    analysedAt = localTime(day = 10, hour = 10),
                    airTemperature = 3.0,
                    precipitationAmountLastHour = 0.0
                )
            )
        ).single()

        assertEquals(
            expected = Triple(
                first = "-2",
                second = "6",
                third = "1.5"
            ),
            actual = Triple(
                first = today.lowTemperatureText,
                second = today.highTemperatureText,
                third = today.precipitationText
            )
        )
    }

    @Test
    fun `a later day starts at the hour the sun rises`() {
        val tomorrow: ComingDay = comingDays(
            forecasts = (0..23).map { hour: Int -> forecast(validAt = localTime(day = 11, hour = hour)) }
        ).single()

        assertEquals(expected = 7, actual = tomorrow.firstShownHourIndex)
    }

    @Test
    fun `a day with six hour steps starts at the step before sunrise`() {
        val tomorrow: ComingDay = comingDays(
            forecasts = listOf(0, 6, 12, 18).map { hour: Int ->
                forecast(
                    validAt = localTime(day = 11, hour = hour),
                    intervalStartedAt = localTime(day = 11, hour = hour) - 6.hours
                )
            }
        ).single()

        assertEquals(expected = 1, actual = tomorrow.firstShownHourIndex)
    }

    @Test
    fun `a day of polar night in kiruna starts at its first hour`() {
        val day: ComingDay = comingDays(
            forecasts = (0..23).map { hour: Int -> forecast(validAt = localTime(month = 12, day = 22, hour = hour)) },
            now = localTime(
                month = 12,
                day = 21,
                hour = 10
            ),
            latitude = KIRUNA_LATITUDE,
            longitude = KIRUNA_LONGITUDE
        ).single()

        assertEquals(expected = 0, actual = day.firstShownHourIndex)
    }

    @Test
    fun `a day of midnight sun in kiruna starts at its first hour`() {
        val day: ComingDay = comingDays(
            forecasts = (0..23).map { hour: Int -> forecast(validAt = localTime(month = 6, day = 22, hour = hour)) },
            now = localTime(
                month = 6,
                day = 21,
                hour = 10
            ),
            latitude = KIRUNA_LATITUDE,
            longitude = KIRUNA_LONGITUDE
        ).single()

        assertEquals(expected = 0, actual = day.firstShownHourIndex)
    }

    @Test
    fun `every weekday gets a short name and date`() {
        val days: List<ComingDay> = comingDays(
            forecasts = (16 until 16 + DayOfWeek.entries.size).map { day: Int ->
                forecast(validAt = localTime(day = day, hour = 12))
            }
        )

        assertEquals(
            expected = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
            actual = days.map { comingDay: ComingDay -> comingDay.weekdayText }
        )
        assertEquals(
            expected = (16..22).map { day: Int -> "$day Nov" },
            actual = days.map { comingDay: ComingDay -> comingDay.dateText }
        )
    }

    @Test
    fun `swedish short weekdays start with a capital letter`() {
        val day: ComingDay = comingDays(
            forecasts = listOf(forecast(validAt = localTime(day = 22, hour = 12))),
            languageTag = "sv-SE"
        ).single()

        assertEquals(expected = "Sön", actual = day.weekdayText)
    }
}