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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.testing.FakeDiskCache
import se.vadretapp.vadret.testing.FixedClock
import se.vadretapp.vadret.testing.LogLine
import se.vadretapp.vadret.testing.mockHttpClient
import se.vadretapp.vadret.testing.networkUnavailableException
import se.vadretapp.vadret.testing.withRecordedLogs
import se.vadretapp.vadret.weatherforecast.local.DefaultWeatherForecastLocalDataSource
import se.vadretapp.vadret.weatherforecast.model.Analysis
import se.vadretapp.vadret.weatherforecast.model.Forecast
import se.vadretapp.vadret.weatherforecast.model.PlaceWeather
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

private const val ANALYSIS_HOST = "opendata-download-metanalys.smhi.se"
private const val NO_HOURS = """{"timeSeries":[]}"""
private const val WEATHER_CACHE_KEY = "weather.json"

private val now: Instant = Instant.parse(input = "2026-11-06T14:05:00Z")

private val nothingSaved = PlaceWeather(
    forecasts = emptyList(),
    analyses = emptyList(),
    fetchedAt = null
)

private fun json(body: String): MockRequestHandler =
    {
        respond(
            content = body,
            headers = jsonHeaders
        )
    }

private fun status(httpStatusCode: HttpStatusCode): MockRequestHandler =
    {
        respond(
            content = "",
            status = httpStatusCode
        )
    }

private fun forecastBody(airTemperature: Double): String = """{"timeSeries":[${timeSeries(time = "2026-10-24T12:00:00Z", airTemperature = airTemperature)}]}"""

class DefaultWeatherForecastRepositoryTest {

    private val testScope = TestScope(StandardTestDispatcher())
    private val requests = mutableListOf<HttpRequestData>()
    private val diskCache = FakeDiskCache()

    private fun repository(
        forecastHandler: MockRequestHandler = json(body = NO_HOURS),
        analysisHandler: MockRequestHandler = json(body = NO_HOURS)
    ): WeatherForecastRepository =
        DefaultWeatherForecastRepository(
            weatherForecastRemoteDataSource = DefaultWeatherForecastRemoteDataSource(
                httpClient = lazy {
                    mockHttpClient { request: HttpRequestData ->
                        requests.add(request)
                        if (request.url.host == ANALYSIS_HOST) {
                            analysisHandler(request)
                        } else {
                            forecastHandler(request)
                        }
                    }
                }
            ),
            weatherForecastLocalDataSource = DefaultWeatherForecastLocalDataSource(
                diskCache = diskCache,
                coroutineDispatcher = StandardTestDispatcher(scheduler = testScope.testScheduler)
            ),
            clock = FixedClock(now = now)
        )

    private fun unreachableRepository(): WeatherForecastRepository =
        repository(
            forecastHandler = { throw networkUnavailableException(host = "opendata-download-metfcst.smhi.se") },
            analysisHandler = { throw networkUnavailableException(host = ANALYSIS_HOST) }
        )

    private suspend fun WeatherForecastRepository.refreshStockholm(): Either<Failure, PlaceWeather> =
        refreshWeather(
            latitude = 59.3293,
            longitude = 18.0686
        )

    private suspend fun WeatherForecastRepository.savedAirTemperatures(
        latitude: Double,
        longitude: Double
    ): List<Double>? =
        when (
            val saved: Either<Failure, PlaceWeather> = observeWeather(
                latitude = latitude,
                longitude = longitude
            ).first()
        ) {
            is Either.Left -> null
            is Either.Right -> saved.b.forecasts.map { forecast: Forecast -> forecast.airTemperature }
        }

    @Test
    fun `the forecast and the analysis are asked for at six decimals at most`() =
        testScope.runTest {
            repository().refreshWeather(
                latitude = 59.32938123,
                longitude = 18.0686
            )

            assertEquals(
                expected = listOf(
                    "https://opendata-download-metanalys.smhi.se/api/category/mesan2g/version/3/geotype/point/lon/18.0686/lat/59.329381/data.json",
                    "https://opendata-download-metfcst.smhi.se/api/category/snow1g/version/1/geotype/point/lon/18.0686/lat/59.329381/data.json"
                ),
                actual = requests.map { request: HttpRequestData -> request.url.toString() }.sorted()
            )
        }

    @Test
    fun `only the analysis refuses a cached answer older than ten minutes`() =
        testScope.runTest {
            repository().refreshStockholm()

            assertEquals(
                expected = mapOf(
                    ANALYSIS_HOST to "max-age=600",
                    "opendata-download-metfcst.smhi.se" to null
                ),
                actual = requests.associate { request: HttpRequestData -> request.url.host to request.headers[HttpHeaders.CacheControl] }
            )
        }

    @Test
    fun `the time series become forecasts in time order`() =
        testScope.runTest {
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

            val refreshed: Either<Failure, PlaceWeather> = repository(forecastHandler = json(body = """{"createdTime":"2026-10-24T10:00:00Z","timeSeries":[$laterHour,$earlierHour]}""")).refreshStockholm()

            assertEquals<Either<Failure, PlaceWeather>>(
                expected = PlaceWeather(
                    forecasts = listOf(
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
                    ),
                    analyses = emptyList(),
                    fetchedAt = now
                ).asRight(),
                actual = refreshed
            )
        }

    @Test
    fun `a missing or impossible precipitation amount is unknown`() =
        testScope.runTest {
            val hours: String = listOf(
                timeSeries(time = "2026-10-24T11:00:00Z", airTemperature = 7.0, precipitationAmountMean = "9999"),
                timeSeries(time = "2026-10-24T12:00:00Z", airTemperature = 7.0, precipitationAmountMean = null),
                timeSeries(time = "2026-10-24T13:00:00Z", airTemperature = 7.0, precipitationAmountMean = "-0.1"),
                timeSeries(time = "2026-10-24T14:00:00Z", airTemperature = 7.0, precipitationAmountMean = "1.2")
            ).joinToString(separator = ",")

            val refreshed: Either<Failure, PlaceWeather> = repository(forecastHandler = json(body = """{"timeSeries":[$hours]}""")).refreshStockholm()

            assertEquals(
                expected = listOf(null, null, null, 1.2),
                actual = (refreshed as Either.Right).b.forecasts.map { forecast: Forecast -> forecast.precipitationAmount }
            )
        }

    @Test
    fun `a time series without its interval start is an invalid response`() =
        testScope.runTest {
            withRecordedLogs { recordingLogSink ->
                assertEquals(
                    expected = Failure(errorCode = ErrorCode.INVALID_RESPONSE).asLeft() to listOf(LogLevel.WARN),
                    actual = repository(forecastHandler = json(body = """{"timeSeries":[{"time":"2026-10-24T12:00:00Z","data":{}}]}""")).refreshStockholm() to
                        recordingLogSink.lines.map { line: LogLine -> line.level }
                )
            }
        }

    @Test
    fun `a place outside the forecast area is told apart from other failures`() =
        testScope.runTest {
            withRecordedLogs { recordingLogSink ->
                val results: List<Either<Failure, PlaceWeather>> = listOf(
                    status(httpStatusCode = HttpStatusCode.NotFound),
                    status(httpStatusCode = HttpStatusCode.ServiceUnavailable),
                    json(body = """{"timeSeries":[${timeSeries(time = "noon", airTemperature = 1.0)}]}"""),
                    json(body = """{"unexpected":true}""")
                ).map { forecastHandler: MockRequestHandler ->
                    repository(forecastHandler = forecastHandler).refreshWeather(
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
    fun `the analysed hours come in time order`() =
        testScope.runTest {
            val refreshed: Either<Failure, PlaceWeather> = repository(
                analysisHandler = json(
                    body = """{"referenceTime":"2026-10-26T17:00:00Z","timeSeries":[${analysisTimeSeries(time = "2026-10-26T17:00:00Z", airTemperature = 11.1)},${analysisTimeSeries(time = "2026-10-26T16:00:00Z", airTemperature = 11.3)}]}"""
                )
            ).refreshStockholm()

            assertEquals(
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
                ),
                actual = (refreshed as Either.Right).b.analyses
            )
        }

    @Test
    fun `a missing or impossible analysed precipitation amount is unknown`() =
        testScope.runTest {
            val hours: String = listOf(
                analysisTimeSeries(time = "2026-10-26T11:00:00Z", airTemperature = 7.0, precipitationAmountLastHour = "9999"),
                analysisTimeSeries(time = "2026-10-26T12:00:00Z", airTemperature = 7.0, precipitationAmountLastHour = null),
                analysisTimeSeries(time = "2026-10-26T13:00:00Z", airTemperature = 7.0, precipitationAmountLastHour = "-0.1"),
                analysisTimeSeries(time = "2026-10-26T14:00:00Z", airTemperature = 7.0, precipitationAmountLastHour = "0.22")
            ).joinToString(separator = ",")

            val refreshed: Either<Failure, PlaceWeather> = repository(analysisHandler = json(body = """{"timeSeries":[$hours]}""")).refreshStockholm()

            assertEquals(
                expected = listOf(null, null, null, 0.22),
                actual = (refreshed as Either.Right).b.analyses.map { analysis: Analysis -> analysis.precipitationAmountLastHour }
            )
        }

    @Test
    fun `an analysis that cannot be fetched leaves the forecast without its past hours`() =
        testScope.runTest {
            withRecordedLogs { recordingLogSink ->
                val results: List<Either<Failure, PlaceWeather>> = listOf(
                    status(httpStatusCode = HttpStatusCode.NotFound),
                    status(httpStatusCode = HttpStatusCode.ServiceUnavailable),
                    json(body = """{"timeSeries":[{"time":"2026-10-26T12:00:00Z","data":{}}]}""")
                ).map { analysisHandler: MockRequestHandler ->
                    repository(
                        forecastHandler = json(body = forecastBody(airTemperature = 7.5)),
                        analysisHandler = analysisHandler
                    ).refreshStockholm()
                }

                assertEquals(
                    expected = List(size = results.size) { listOf(7.5) to emptyList<Analysis>() },
                    actual = results.map { result: Either<Failure, PlaceWeather> ->
                        val placeWeather: PlaceWeather = (result as Either.Right).b
                        placeWeather.forecasts.map { forecast: Forecast -> forecast.airTemperature } to placeWeather.analyses
                    }
                )
                assertEquals(
                    expected = List(size = results.size) { listOf(LogLevel.WARN, LogLevel.INFO) }.flatten(),
                    actual = recordingLogSink.lines.map { line: LogLine -> line.level }
                )
            }
        }

    @Test
    fun `a forecast that fails stops waiting for the analysis`() =
        testScope.runTest {
            val analysisGate = CompletableDeferred<Unit>()

            withRecordedLogs {
                assertEquals(
                    expected = Failure(errorCode = ErrorCode.HTTP_SERVICE_UNAVAILABLE).asLeft(),
                    actual = repository(
                        forecastHandler = status(httpStatusCode = HttpStatusCode.ServiceUnavailable),
                        analysisHandler = {
                            analysisGate.await()
                            respond(
                                content = NO_HOURS,
                                headers = jsonHeaders
                            )
                        }
                    ).refreshStockholm()
                )
            }
        }

    @Test
    fun `a refresh saves the weather and the place shows it`() =
        testScope.runTest {
            val weatherForecastRepository: WeatherForecastRepository = repository(forecastHandler = json(body = forecastBody(airTemperature = 7.5)))
            val beforeRefresh: Either<Failure, PlaceWeather> = weatherForecastRepository.observeWeather(
                latitude = 59.3293,
                longitude = 18.0686
            ).first()

            weatherForecastRepository.refreshStockholm()

            assertEquals(
                expected = nothingSaved.asRight<PlaceWeather>() to listOf(7.5),
                actual = beforeRefresh to weatherForecastRepository.savedAirTemperatures(
                    latitude = 59.3293,
                    longitude = 18.0686
                )
            )
        }

    @Test
    fun `the saved weather is found by place to about a kilometre`() =
        testScope.runTest {
            repository(forecastHandler = json(body = forecastBody(airTemperature = 7.5))).refreshStockholm()

            assertEquals(
                expected = listOf(7.5) to emptyList(),
                actual = unreachableRepository().savedAirTemperatures(
                    latitude = 59.3271,
                    longitude = 18.0712
                ) to unreachableRepository().savedAirTemperatures(
                    latitude = 57.7089,
                    longitude = 11.9746
                )
            )
        }

    @Test
    fun `a refresh that fails keeps the saved weather`() =
        testScope.runTest {
            repository(forecastHandler = json(body = forecastBody(airTemperature = 7.5))).refreshStockholm()

            withRecordedLogs {
                assertEquals<Pair<Either<Failure, PlaceWeather>, List<Double>?>>(
                    expected = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft() to listOf(7.5),
                    actual = unreachableRepository().refreshStockholm() to unreachableRepository().savedAirTemperatures(
                        latitude = 59.3293,
                        longitude = 18.0686
                    )
                )
            }
        }

    @Test
    fun `a refresh replaces the saved weather of the place`() =
        testScope.runTest {
            listOf(7.5, 9.0).forEach { airTemperature: Double ->
                repository(forecastHandler = json(body = forecastBody(airTemperature = airTemperature))).refreshStockholm()
            }

            assertEquals(
                expected = listOf(9.0),
                actual = unreachableRepository().savedAirTemperatures(
                    latitude = 59.3293,
                    longitude = 18.0686
                )
            )
        }

    @Test
    fun `only the ten places refreshed last keep their weather`() =
        testScope.runTest {
            val latitudes: List<Double> = List(size = 11) { index: Int -> 55.0 + index }
            latitudes.forEach { latitude: Double ->
                repository(forecastHandler = json(body = forecastBody(airTemperature = latitude))).refreshWeather(
                    latitude = latitude,
                    longitude = 15.0
                )
            }

            assertEquals(
                expected = listOf(emptyList<Double>()) + latitudes.drop(n = 1).map { latitude: Double -> listOf(latitude) },
                actual = latitudes.map { latitude: Double ->
                    unreachableRepository().savedAirTemperatures(
                        latitude = latitude,
                        longitude = 15.0
                    )
                }
            )
        }

    @Test
    fun `unreadable saved weather is dropped and shows as nothing saved`() =
        testScope.runTest {
            diskCache.write(
                key = WEATHER_CACHE_KEY,
                value = "not json"
            )

            withRecordedLogs {
                assertEquals(
                    expected = emptyList<Double>() to emptyMap(),
                    actual = unreachableRepository().savedAirTemperatures(
                        latitude = 59.3293,
                        longitude = 18.0686
                    ) to diskCache.entries
                )
            }
        }
}