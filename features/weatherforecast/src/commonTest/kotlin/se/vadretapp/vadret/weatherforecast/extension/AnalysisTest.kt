package se.vadretapp.vadret.weatherforecast.extension

import se.vadretapp.vadret.weatherforecast.model.Analysis
import se.vadretapp.vadret.weatherforecast.model.Forecast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class AnalysisTest {

    @Test
    fun `an analysed hour reads as a forecast for the hour before it`() {
        val analysis = Analysis(
            analysedAt = Instant.parse(input = "2026-10-26T17:00:00Z"),
            airTemperature = 11.1,
            windFromDirection = 203.0,
            windSpeed = 4.4,
            relativeHumidity = 82,
            precipitationType = 1,
            symbolCode = 18,
            precipitationAmountLastHour = null
        )

        assertEquals(
            expected = Forecast(
                validAt = Instant.parse(input = "2026-10-26T17:00:00Z"),
                intervalStartedAt = Instant.parse(input = "2026-10-26T16:00:00Z"),
                airTemperature = 11.1,
                windFromDirection = 203.0,
                windSpeed = 4.4,
                relativeHumidity = 82,
                precipitationType = 1,
                symbolCode = 18,
                precipitationAmount = null
            ),
            actual = analysis.toForecast()
        )
    }
}