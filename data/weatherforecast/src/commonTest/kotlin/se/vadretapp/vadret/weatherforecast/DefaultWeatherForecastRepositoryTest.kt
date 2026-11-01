package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.testing.LogLine
import se.vadretapp.vadret.testing.mockHttpClient
import se.vadretapp.vadret.testing.withRecordedLogs
import se.vadretapp.vadret.weatherforecast.model.Analysis
import se.vadretapp.vadret.weatherforecast.model.Forecast
import se.vadretapp.vadret.weatherforecast.remote.DefaultWeatherForecastRemoteDataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

private fun timeSeries(
    time: String,
    airTemperature: Double,
    intervalStartTime: String = time,
    precipitationAmountMean: String? = "0.3"
): String =
    timeSeriesWithAmount(
        time = time,
        intervalStartTime = intervalStartTime,
        airTemperature = airTemperature,
        precipitationAmountField = if (precipitationAmountMean == null) {
            ""
        } else {
            "\"precipitation_amount_mean\": $precipitationAmountMean,"
        }
    )

private fun timeSeriesWithAmount(
    time: String,
    intervalStartTime: String,
    airTemperature: Double,
    precipitationAmountField: String
): String =
    """
    {
      "time": "$time",
      "intervalParametersStartTime": "$intervalStartTime",
      "data": {
        "air_temperature": $airTemperature,
        "wind_from_direction": 225.0,
        "wind_speed": 4.2,
        "relative_humidity": 81,
        "thunderstorm_probability": 0,
        "predominant_precipitation_type_at_surface": 1,
        "probability_of_precipitation": 40,
        "precipitation_amount_min": 0.0,
        "precipitation_amount_max": 0.4,
        $precipitationAmountField
        "symbol_code": 18
      }
    }
    """.trimIndent()

private fun analysisTimeSeries(
    time: String,
    airTemperature: Double,
    precipitationAmountLastHour: String? = "0.04"
): String {
    val precipitationAmountField: String = if (precipitationAmountLastHour == null) {
        ""
    } else {
        "\"precipitation_amount_last_1_hours\": $precipitationAmountLastHour,"
    }
    return """
    {
      "time": "$time",
      "data": {
        "air_temperature": $airTemperature,
        "wind_from_direction": 203,
        "wind_speed": 4.4,
        "wind_speed_of_gust": 7.8,
        "relative_humidity": 82,
        "cloud_area_fraction": 8,
        $precipitationAmountField
        "predominant_precipitation_type_at_surface": 0,
        "symbol_code": 6
      }
    }
    """.trimIndent()
}

class DefaultWeatherForecastRepositoryTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun repository(handler: MockRequestHandler): WeatherForecastRepository =
        DefaultWeatherForecastRepository(
            weatherForecastRemoteDataSource = DefaultWeatherForecastRemoteDataSource(
                httpClient = lazy {
                    mockHttpClient { request: HttpRequestData ->
                        requests.add(request)
                        handler(request)
                    }
                }
            )
        )

    @Test
    fun `the forecast is asked for at six decimals at most`() =
        runTest {
            repository {
                respond(
                    content = """{"timeSeries":[]}""",
                    headers = jsonHeaders
                )
            }.getForecasts(
                latitude = 59.32938123,
                longitude = 18.0686
            )

            assertEquals(
                expected = "https://opendata-download-metfcst.smhi.se/api/category/snow1g/version/1/geotype/point/lon/18.0686/lat/59.329381/data.json",
                actual = requests.single().url.toString()
            )
        }

    @Test
    fun `the time series become forecasts in time order`() =
        runTest {
            val laterHour: String = timeSeries(
                time = "2026-10-24T12:00:00Z",
                airTemperature = 8.5,
                intervalStartTime = "2026-10-24T11:00:00Z"
            )
            val earlierHour: String = timeSeries(
                time = "2026-10-24T11:00:00Z",
                airTemperature = 7.0,
                intervalStartTime = "2026-10-24T10:00:00Z"
            )
            val body = """
            {
              "createdTime": "2026-10-24T10:00:00Z",
              "timeSeries": [
                $laterHour,
                $earlierHour
              ]
            }
            """.trimIndent()

            val result: Either<Failure, List<Forecast>> = repository {
                respond(
                    content = body,
                    headers = jsonHeaders
                )
            }.getForecasts(
                latitude = 59.3293,
                longitude = 18.0686
            )

            assertEquals<Either<Failure, List<Forecast>>>(
                expected = listOf(
                    Forecast(
                        validAt = Instant.parse(input = "2026-10-24T11:00:00Z"),
                        intervalStartedAt = Instant.parse(input = "2026-10-24T10:00:00Z"),
                        airTemperature = 7.0,
                        windFromDirection = 225.0,
                        windSpeed = 4.2,
                        relativeHumidity = 81,
                        precipitationType = 1,
                        symbolCode = 18,
                        precipitationAmount = 0.3
                    ),
                    Forecast(
                        validAt = Instant.parse(input = "2026-10-24T12:00:00Z"),
                        intervalStartedAt = Instant.parse(input = "2026-10-24T11:00:00Z"),
                        airTemperature = 8.5,
                        windFromDirection = 225.0,
                        windSpeed = 4.2,
                        relativeHumidity = 81,
                        precipitationType = 1,
                        symbolCode = 18,
                        precipitationAmount = 0.3
                    )
                ).asRight(),
                actual = result
            )
        }

    @Test
    fun `a missing or impossible precipitation amount is unknown`() =
        runTest {
            val body = """
            {
              "timeSeries": [
                ${timeSeries(time = "2026-10-24T11:00:00Z", airTemperature = 7.0, precipitationAmountMean = "9999")},
                ${timeSeries(time = "2026-10-24T12:00:00Z", airTemperature = 7.0, precipitationAmountMean = null)},
                ${timeSeries(time = "2026-10-24T13:00:00Z", airTemperature = 7.0, precipitationAmountMean = "-0.1")},
                ${timeSeries(time = "2026-10-24T14:00:00Z", airTemperature = 7.0, precipitationAmountMean = "1.2")}
              ]
            }
            """.trimIndent()

            val result: Either<Failure, List<Forecast>> = repository {
                respond(
                    content = body,
                    headers = jsonHeaders
                )
            }.getForecasts(
                latitude = 59.3293,
                longitude = 18.0686
            )

            assertEquals(
                expected = listOf(null, null, null, 1.2),
                actual = (result as Either.Right).b.map { forecast: Forecast -> forecast.precipitationAmount }
            )
        }

    @Test
    fun `a time series without its interval start is an invalid response`() =
        runTest {
            withRecordedLogs { recordingLogSink ->
                val result: Either<Failure, List<Forecast>> = repository {
                    respond(
                        content = """{"timeSeries":[{"time":"2026-10-24T12:00:00Z","data":{}}]}""",
                        headers = jsonHeaders
                    )
                }.getForecasts(
                    latitude = 59.3293,
                    longitude = 18.0686
                )

                assertEquals(
                    expected = Failure(errorCode = ErrorCode.INVALID_RESPONSE).asLeft(),
                    actual = result
                )
                assertEquals(
                    expected = listOf(LogLevel.WARN),
                    actual = recordingLogSink.lines.map { line: LogLine -> line.level }
                )
            }
        }

    @Test
    fun `a place outside the forecast area is told apart from other failures`() =
        runTest {
            withRecordedLogs { recordingLogSink ->
                val results: List<Either<Failure, List<Forecast>>> = listOf(
                    repository {
                        respond(
                            content = "",
                            status = HttpStatusCode.NotFound
                        )
                    },
                    repository {
                        respond(
                            content = "",
                            status = HttpStatusCode.ServiceUnavailable
                        )
                    },
                    repository {
                        respond(
                            content = """{"timeSeries":[${timeSeries(time = "noon", airTemperature = 1.0)}]}""",
                            headers = jsonHeaders
                        )
                    },
                    repository {
                        respond(
                            content = """{"unexpected":true}""",
                            headers = jsonHeaders
                        )
                    }
                ).map { weatherForecastRepository: WeatherForecastRepository ->
                    weatherForecastRepository.getForecasts(
                        latitude = 70.0,
                        longitude = 40.0
                    )
                }

                assertEquals(
                    expected = listOf(
                        ErrorCode.WEATHER_FORECAST_NOT_AVAILABLE_FOR_LOCATION,
                        ErrorCode.HTTP_SERVICE_UNAVAILABLE,
                        ErrorCode.INVALID_RESPONSE,
                        ErrorCode.INVALID_RESPONSE
                    ).map { errorCode: ErrorCode -> Failure(errorCode = errorCode).asLeft() },
                    actual = results
                )
                assertEquals(
                    expected = List(size = results.size) { LogLevel.WARN },
                    actual = recordingLogSink.lines.map { line: LogLine -> line.level }
                )
            }
        }

    @Test
    fun `the analysis is asked for at six decimals at most`() =
        runTest {
            repository {
                respond(
                    content = """{"timeSeries":[]}""",
                    headers = jsonHeaders
                )
            }.getAnalyses(
                latitude = 59.32938123,
                longitude = 18.0686
            )

            assertEquals(
                expected = "https://opendata-download-metanalys.smhi.se/api/category/mesan2g/version/3/geotype/point/lon/18.0686/lat/59.329381/data.json",
                actual = requests.single().url.toString()
            )
        }

    @Test
    fun `only the analysis refuses a cached answer older than ten minutes`() =
        runTest {
            val weatherForecastRepository: WeatherForecastRepository = repository {
                respond(
                    content = """{"timeSeries":[]}""",
                    headers = jsonHeaders
                )
            }

            weatherForecastRepository.getAnalyses(
                latitude = 59.3293,
                longitude = 18.0686
            )
            weatherForecastRepository.getForecasts(
                latitude = 59.3293,
                longitude = 18.0686
            )

            assertEquals(
                expected = listOf("max-age=600", null),
                actual = requests.map { request: HttpRequestData -> request.headers[HttpHeaders.CacheControl] }
            )
        }

    @Test
    fun `the analysed hours come in time order`() =
        runTest {
            val body = """
            {
              "referenceTime": "2026-10-26T17:00:00Z",
              "timeSeries": [
                ${analysisTimeSeries(time = "2026-10-26T17:00:00Z", airTemperature = 11.1)},
                ${analysisTimeSeries(time = "2026-10-26T16:00:00Z", airTemperature = 11.3)}
              ]
            }
            """.trimIndent()

            val result: Either<Failure, List<Analysis>> = repository {
                respond(
                    content = body,
                    headers = jsonHeaders
                )
            }.getAnalyses(
                latitude = 59.3293,
                longitude = 18.0686
            )

            assertEquals<Either<Failure, List<Analysis>>>(
                expected = listOf(
                    Analysis(
                        analysedAt = Instant.parse(input = "2026-10-26T16:00:00Z"),
                        airTemperature = 11.3,
                        windFromDirection = 203.0,
                        windSpeed = 4.4,
                        relativeHumidity = 82,
                        precipitationType = 0,
                        symbolCode = 6,
                        precipitationAmountLastHour = 0.04
                    ),
                    Analysis(
                        analysedAt = Instant.parse(input = "2026-10-26T17:00:00Z"),
                        airTemperature = 11.1,
                        windFromDirection = 203.0,
                        windSpeed = 4.4,
                        relativeHumidity = 82,
                        precipitationType = 0,
                        symbolCode = 6,
                        precipitationAmountLastHour = 0.04
                    )
                ).asRight(),
                actual = result
            )
        }

    @Test
    fun `a missing or impossible analysed precipitation amount is unknown`() =
        runTest {
            val body = """
            {
              "timeSeries": [
                ${analysisTimeSeries(time = "2026-10-26T11:00:00Z", airTemperature = 7.0, precipitationAmountLastHour = "9999")},
                ${analysisTimeSeries(time = "2026-10-26T12:00:00Z", airTemperature = 7.0, precipitationAmountLastHour = null)},
                ${analysisTimeSeries(time = "2026-10-26T13:00:00Z", airTemperature = 7.0, precipitationAmountLastHour = "-0.1")},
                ${analysisTimeSeries(time = "2026-10-26T14:00:00Z", airTemperature = 7.0, precipitationAmountLastHour = "0.22")}
              ]
            }
            """.trimIndent()

            val result: Either<Failure, List<Analysis>> = repository {
                respond(
                    content = body,
                    headers = jsonHeaders
                )
            }.getAnalyses(
                latitude = 59.3293,
                longitude = 18.0686
            )

            assertEquals(
                expected = listOf(null, null, null, 0.22),
                actual = (result as Either.Right).b.map { analysis: Analysis -> analysis.precipitationAmountLastHour }
            )
        }

    @Test
    fun `a place outside the analysis area is told apart from other failures`() =
        runTest {
            withRecordedLogs { recordingLogSink ->
                val results: List<Either<Failure, List<Analysis>>> = listOf(
                    repository {
                        respond(
                            content = "",
                            status = HttpStatusCode.NotFound
                        )
                    },
                    repository {
                        respond(
                            content = "",
                            status = HttpStatusCode.ServiceUnavailable
                        )
                    },
                    repository {
                        respond(
                            content = """{"timeSeries":[{"time":"2026-10-26T12:00:00Z","data":{}}]}""",
                            headers = jsonHeaders
                        )
                    }
                ).map { weatherForecastRepository: WeatherForecastRepository ->
                    weatherForecastRepository.getAnalyses(
                        latitude = 70.0,
                        longitude = 40.0
                    )
                }

                assertEquals(
                    expected = listOf(
                        ErrorCode.WEATHER_ANALYSIS_NOT_AVAILABLE_FOR_LOCATION,
                        ErrorCode.HTTP_SERVICE_UNAVAILABLE,
                        ErrorCode.INVALID_RESPONSE
                    ).map { errorCode: ErrorCode -> Failure(errorCode = errorCode).asLeft() },
                    actual = results
                )
                assertEquals(
                    expected = List(size = results.size) { LogLevel.WARN },
                    actual = recordingLogSink.lines.map { line: LogLine -> line.level }
                )
            }
        }
}