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
import se.vadretapp.vadret.weatherforecast.model.Forecasts
import se.vadretapp.vadret.weatherforecast.model.PlaceWeather
import se.vadretapp.vadret.weatherforecast.model.WeatherPlaceKind
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

private fun analysisBody(airTemperature: Double): String = """{"timeSeries":[${analysisTimeSeries(time = "2026-10-24T10:00:00Z", airTemperature = airTemperature)}]}"""

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

    private suspend fun WeatherForecastRepository.refreshForecasts(
        latitude: Double = 59.3293,
        longitude: Double = 18.0686,
        weatherPlaceKind: WeatherPlaceKind = WeatherPlaceKind.FAVORITE
    ): Either<Failure, Forecasts> =
        refreshForecasts(
            latitude = latitude,
            longitude = longitude,
            weatherPlaceKind = weatherPlaceKind
        )

    private suspend fun savedWeather(
        latitude: Double = 59.3293,
        longitude: Double = 18.0686
    ): PlaceWeather? =
        (
            unreachableRepository().observeWeather(
                latitude = latitude,
                longitude = longitude
            ).first() as Either.Right
            ).b

    private suspend fun savedAirTemperatures(
        latitude: Double = 59.3293,
        longitude: Double = 18.0686
    ): List<Double>? =
        savedWeather(
            latitude = latitude,
            longitude = longitude
        )?.forecasts?.map { forecast: Forecast -> forecast.airTemperature }

    @Test
    fun `the forecast and the analysis are asked for at six decimals at most`() =
        testScope.runTest {
            repository().refreshForecasts(
                latitude = 59.32938123,
                longitude = 18.0686
            )
            repository().refreshAnalyses(
                latitude = 59.32938123,
                longitude = 18.0686,
                weatherPlaceKind = WeatherPlaceKind.FAVORITE
            )

            assertEquals(
                expected = listOf(
                    "https://opendata-download-metfcst.smhi.se/api/category/snow1g/version/1/geotype/point/lon/18.0686/lat/59.329381/data.json",
                    "https://opendata-download-metanalys.smhi.se/api/category/mesan2g/version/3/geotype/point/lon/18.0686/lat/59.329381/data.json"
                ),
                actual = requests.map { request: HttpRequestData -> request.url.toString() }
            )
        }

    @Test
    fun `only the analysis refuses a cached answer older than ten minutes`() =
        testScope.runTest {
            repository().refreshForecasts()
            repository().refreshAnalyses(
                latitude = 59.3293,
                longitude = 18.0686,
                weatherPlaceKind = WeatherPlaceKind.FAVORITE
            )

            assertEquals(
                expected = listOf(null, "max-age=600"),
                actual = requests.map { request: HttpRequestData -> request.headers[HttpHeaders.CacheControl] }
            )
        }

    @Test
    fun `the time series become forecasts in time order and carry the time they were fetched`() =
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

            assertEquals<Either<Failure, Forecasts>>(
                expected = Forecasts(
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
                    fetchedAt = now
                ).asRight(),
                actual = repository(forecastHandler = json(body = """{"createdTime":"2026-10-24T10:00:00Z","timeSeries":[$laterHour,$earlierHour]}""")).refreshForecasts()
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

            assertEquals(
                expected = listOf(null, null, null, 1.2),
                actual = (repository(forecastHandler = json(body = """{"timeSeries":[$hours]}""")).refreshForecasts() as Either.Right).b.forecasts.map { forecast: Forecast -> forecast.precipitationAmount }
            )
        }

    @Test
    fun `a time series without its interval start is an invalid response`() =
        testScope.runTest {
            withRecordedLogs { recordingLogSink ->
                assertEquals(
                    expected = Failure(errorCode = ErrorCode.INVALID_RESPONSE).asLeft() to listOf(LogLevel.WARN),
                    actual = repository(forecastHandler = json(body = """{"timeSeries":[{"time":"2026-10-24T12:00:00Z","data":{}}]}""")).refreshForecasts() to
                        recordingLogSink.lines.map { line: LogLine -> line.level }
                )
            }
        }

    @Test
    fun `a place outside the forecast area is told apart from other failures`() =
        testScope.runTest {
            withRecordedLogs { recordingLogSink ->
                val results: List<Either<Failure, Forecasts>> = listOf(
                    status(httpStatusCode = HttpStatusCode.NotFound),
                    status(httpStatusCode = HttpStatusCode.ServiceUnavailable),
                    json(body = """{"timeSeries":[${timeSeries(time = "noon", airTemperature = 1.0)}]}"""),
                    json(body = """{"unexpected":true}""")
                ).map { forecastHandler: MockRequestHandler -> repository(forecastHandler = forecastHandler).refreshForecasts() }

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
            val refreshed: Either<Failure, List<Analysis>> = repository(
                analysisHandler = json(
                    body = """{"referenceTime":"2026-10-26T17:00:00Z","timeSeries":[${analysisTimeSeries(time = "2026-10-26T17:00:00Z", airTemperature = 11.1)},${analysisTimeSeries(time = "2026-10-26T16:00:00Z", airTemperature = 11.3)}]}"""
                )
            ).refreshAnalyses(
                latitude = 59.3293,
                longitude = 18.0686,
                weatherPlaceKind = WeatherPlaceKind.FAVORITE
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
                actual = refreshed
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

            val refreshed: Either<Failure, List<Analysis>> = repository(analysisHandler = json(body = """{"timeSeries":[$hours]}""")).refreshAnalyses(
                latitude = 59.3293,
                longitude = 18.0686,
                weatherPlaceKind = WeatherPlaceKind.FAVORITE
            )

            assertEquals(
                expected = listOf(null, null, null, 0.22),
                actual = (refreshed as Either.Right).b.map { analysis: Analysis -> analysis.precipitationAmountLastHour }
            )
        }

    @Test
    fun `a place outside the analysis area is told apart from other failures`() =
        testScope.runTest {
            withRecordedLogs { recordingLogSink ->
                val results: List<Either<Failure, List<Analysis>>> = listOf(
                    status(httpStatusCode = HttpStatusCode.NotFound),
                    status(httpStatusCode = HttpStatusCode.ServiceUnavailable),
                    json(body = """{"timeSeries":[{"time":"2026-10-26T12:00:00Z","data":{}}]}""")
                ).map { analysisHandler: MockRequestHandler ->
                    repository(analysisHandler = analysisHandler).refreshAnalyses(
                        latitude = 70.0,
                        longitude = 40.0,
                        weatherPlaceKind = WeatherPlaceKind.FAVORITE
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

    @Test
    fun `a refreshed forecast and analysis are saved together for the place`() =
        testScope.runTest {
            val weatherForecastRepository: WeatherForecastRepository = repository(
                forecastHandler = json(body = forecastBody(airTemperature = 7.5)),
                analysisHandler = json(body = analysisBody(airTemperature = 6.1))
            )
            val nothingSaved: PlaceWeather? = savedWeather()

            weatherForecastRepository.refreshAnalyses(
                latitude = 59.3293,
                longitude = 18.0686,
                weatherPlaceKind = WeatherPlaceKind.FAVORITE
            )
            val onlyAnalysed: PlaceWeather? = savedWeather()
            weatherForecastRepository.refreshForecasts()
            val saved: PlaceWeather? = savedWeather()

            assertEquals(
                expected = Triple(
                    first = null,
                    second = null,
                    third = Triple(
                        first = listOf(7.5),
                        second = listOf(6.1),
                        third = now
                    )
                ),
                actual = Triple(
                    first = nothingSaved,
                    second = onlyAnalysed,
                    third = Triple(
                        first = saved?.forecasts?.map { forecast: Forecast -> forecast.airTemperature },
                        second = saved?.analyses?.map { analysis: Analysis -> analysis.airTemperature },
                        third = saved?.fetchedAt
                    )
                )
            )
        }

    @Test
    fun `the saved weather is found by place to about a kilometre`() =
        testScope.runTest {
            repository(forecastHandler = json(body = forecastBody(airTemperature = 7.5))).refreshForecasts()

            assertEquals(
                expected = listOf(7.5) to null,
                actual = savedAirTemperatures(
                    latitude = 59.3271,
                    longitude = 18.0712
                ) to savedAirTemperatures(
                    latitude = 57.7089,
                    longitude = 11.9746
                )
            )
        }

    @Test
    fun `a dropped pin is fetched without being saved`() =
        testScope.runTest {
            val refreshed: Either<Failure, Forecasts> = repository(forecastHandler = json(body = forecastBody(airTemperature = 7.5))).refreshForecasts(weatherPlaceKind = WeatherPlaceKind.DROPPED_PIN)

            assertEquals(
                expected = listOf(7.5) to emptyMap(),
                actual = (refreshed as Either.Right).b.forecasts.map { forecast: Forecast -> forecast.airTemperature } to diskCache.entries
            )
        }

    @Test
    fun `the chosen place keeps one saved place however far it moves while favorites keep theirs`() =
        testScope.runTest {
            repository(forecastHandler = json(body = forecastBody(airTemperature = 1.0))).refreshForecasts(
                latitude = 57.7089,
                longitude = 11.9746,
                weatherPlaceKind = WeatherPlaceKind.FAVORITE
            )
            listOf(59.3293, 63.8258).forEach { latitude: Double ->
                repository(forecastHandler = json(body = forecastBody(airTemperature = latitude))).refreshForecasts(
                    latitude = latitude,
                    longitude = 18.0686,
                    weatherPlaceKind = WeatherPlaceKind.CHOSEN_PLACE
                )
            }

            assertEquals(
                expected = Triple(
                    first = listOf(1.0),
                    second = null,
                    third = listOf(63.8258)
                ),
                actual = Triple(
                    first = savedAirTemperatures(
                        latitude = 57.7089,
                        longitude = 11.9746
                    ),
                    second = savedAirTemperatures(
                        latitude = 59.3293,
                        longitude = 18.0686
                    ),
                    third = savedAirTemperatures(
                        latitude = 63.8258,
                        longitude = 18.0686
                    )
                )
            )
        }

    @Test
    fun `a refresh that fails keeps the saved weather`() =
        testScope.runTest {
            repository(forecastHandler = json(body = forecastBody(airTemperature = 7.5))).refreshForecasts()

            withRecordedLogs {
                assertEquals<Pair<Either<Failure, Forecasts>, List<Double>?>>(
                    expected = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft() to listOf(7.5),
                    actual = unreachableRepository().refreshForecasts() to savedAirTemperatures()
                )
            }
        }

    @Test
    fun `a refresh replaces the saved forecast of the place`() =
        testScope.runTest {
            listOf(7.5, 9.0).forEach { airTemperature: Double ->
                repository(forecastHandler = json(body = forecastBody(airTemperature = airTemperature))).refreshForecasts()
            }

            assertEquals(
                expected = listOf(9.0),
                actual = savedAirTemperatures()
            )
        }

    @Test
    fun `only the ten places refreshed last keep their weather`() =
        testScope.runTest {
            val latitudes: List<Double> = List(size = 11) { index: Int -> 55.0 + index }
            latitudes.forEach { latitude: Double ->
                repository(forecastHandler = json(body = forecastBody(airTemperature = latitude))).refreshForecasts(
                    latitude = latitude,
                    longitude = 15.0
                )
            }

            assertEquals(
                expected = listOf(null) + latitudes.drop(n = 1).map { latitude: Double -> listOf(latitude) },
                actual = latitudes.map { latitude: Double ->
                    savedAirTemperatures(
                        latitude = latitude,
                        longitude = 15.0
                    )
                }
            )
        }

    @Test
    fun `unreadable saved weather shows as nothing saved and is left to be replaced`() =
        testScope.runTest {
            diskCache.write(
                key = WEATHER_CACHE_KEY,
                value = "not json"
            )

            withRecordedLogs {
                val unreadable: PlaceWeather? = savedWeather()
                repository(forecastHandler = json(body = forecastBody(airTemperature = 7.5))).refreshForecasts()

                assertEquals(
                    expected = null to listOf(7.5),
                    actual = unreadable to savedAirTemperatures()
                )
            }
        }
}