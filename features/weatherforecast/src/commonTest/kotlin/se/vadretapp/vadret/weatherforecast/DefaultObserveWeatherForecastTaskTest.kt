package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.nominatim.model.Locality
import se.vadretapp.vadret.weatherforecast.model.Forecast
import se.vadretapp.vadret.weatherforecast.model.PlaceForecast
import se.vadretapp.vadret.weatherforecast.model.PlaceWeather
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val savedForecast: Forecast = noon.copy(airTemperature = SAVED_AIR_TEMPERATURE)

class DefaultObserveWeatherForecastTaskTest {

    private val locationRepository = FakeLocationRepository()
    private val weatherForecastRepository = FakeWeatherForecastRepository(analyses = listOf(morning).asRight())
    private val nominatimRepository = FakeNominatimRepository()
    private val observeWeatherForecastTask: ObserveWeatherForecastTask = DefaultObserveWeatherForecastTask(
        getChosenPlaceTask = FakeGetChosenPlaceTask(
            locationRepository = locationRepository,
            nominatimRepository = nominatimRepository
        ),
        weatherForecastRepository = weatherForecastRepository
    )

    @Test
    fun `a stored place is forecast under its own name`() =
        runTest {
            assertEquals<Either<Failure, PlaceForecast>>(
                expected = PlaceForecast(
                    placeName = "Stockholm",
                    municipality = "Stockholm",
                    county = "Stockholm",
                    latitude = 59.3293,
                    longitude = 18.0686,
                    forecasts = listOf(noon),
                    analyses = listOf(morning),
                    outdatedSince = null
                ).asRight(),
                actual = observeWeatherForecastTask(isRefresh = false).first()
            )
        }

    @Test
    fun `the device location is forecast and placed by OpenStreetMap`() =
        runTest {
            locationRepository.isAutomatic = true.asRight()

            assertEquals<Either<Failure, PlaceForecast>>(
                expected = PlaceForecast(
                    placeName = "Göteborg",
                    municipality = "Göteborgs kommun",
                    county = "Västra Götalands län",
                    latitude = 57.7,
                    longitude = 11.97,
                    forecasts = listOf(noon),
                    analyses = listOf(morning),
                    outdatedSince = null
                ).asRight(),
                actual = observeWeatherForecastTask(isRefresh = false).first()
            )
            assertEquals(expected = listOf(57.7 to 11.97), actual = weatherForecastRepository.requestedCoordinates)
        }

    @Test
    fun `a place that cannot be named still gets its forecast`() =
        runTest {
            locationRepository.isAutomatic = true.asRight()
            val failures: List<Either<Failure, Locality>> = listOf(
                Failure(errorCode = ErrorCode.HTTP_FORBIDDEN).asLeft(),
                Locality(
                    name = null,
                    municipality = null,
                    county = null
                ).asRight()
            )

            val places: List<Triple<String?, String?, String?>> = failures.map { locality: Either<Failure, Locality> ->
                nominatimRepository.locality = locality
                val placeForecast: PlaceForecast = (observeWeatherForecastTask(isRefresh = false).first() as Either.Right).b
                Triple(
                    first = placeForecast.placeName,
                    second = placeForecast.municipality,
                    third = placeForecast.county
                )
            }

            assertEquals(
                expected = List(size = failures.size) {
                    Triple(
                        first = null,
                        second = null,
                        third = null
                    )
                },
                actual = places
            )
        }

    @Test
    fun `failures before the forecast stop it`() =
        runTest {
            val modeFailure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
            val locationFailure = Failure(errorCode = ErrorCode.LOCATION_PROVIDER_DISABLED)
            val forecastFailure = Failure(errorCode = ErrorCode.WEATHER_FORECAST_NOT_AVAILABLE_FOR_LOCATION)

            locationRepository.isAutomatic = modeFailure.asLeft()
            val withoutMode: Either<Failure, PlaceForecast> = observeWeatherForecastTask(isRefresh = false).first()
            locationRepository.isAutomatic = true.asRight()
            locationRepository.deviceLocation = locationFailure.asLeft()
            val withoutLocation: Either<Failure, PlaceForecast> = observeWeatherForecastTask(isRefresh = false).first()
            locationRepository.isAutomatic = false.asRight()
            weatherForecastRepository.forecasts = forecastFailure.asLeft()
            val withoutForecast: Either<Failure, PlaceForecast> = observeWeatherForecastTask(isRefresh = false).first()

            assertEquals(
                expected = listOf(modeFailure, locationFailure, forecastFailure).map { failure: Failure -> failure.asLeft() },
                actual = listOf(withoutMode, withoutLocation, withoutForecast)
            )
            assertTrue(actual = weatherForecastRepository.requestedCoordinates.size == 1)
        }

    @Test
    fun `saved weather shows at once and the refreshed weather after it`() =
        runTest {
            weatherForecastRepository.savedWeather.value = mapOf(
                (59.3293 to 18.0686) to PlaceWeather(
                    forecasts = listOf(savedForecast),
                    analyses = emptyList(),
                    fetchedAt = FAKE_FETCHED_AT
                )
            )
            val refreshGate = CompletableDeferred<Unit>()
            weatherForecastRepository.forecastGates[59.3293 to 18.0686] = refreshGate
            val seen = mutableListOf<List<Forecast>>()

            val observing = launch {
                observeWeatherForecastTask(isRefresh = false)
                    .take(count = 2)
                    .collect { placeForecast: Either<Failure, PlaceForecast> -> seen += (placeForecast as Either.Right).b.forecasts }
            }
            testScheduler.advanceUntilIdle()
            val beforeRefresh: List<List<Forecast>> = seen.toList()
            refreshGate.complete(value = Unit)
            observing.join()

            assertEquals(
                expected = listOf(listOf(savedForecast)) to listOf(listOf(savedForecast), listOf(noon)),
                actual = beforeRefresh to seen
            )
        }

    @Test
    fun `a refresh shows the refreshed weather first`() =
        runTest {
            weatherForecastRepository.savedWeather.value = mapOf(
                (59.3293 to 18.0686) to PlaceWeather(
                    forecasts = listOf(savedForecast),
                    analyses = emptyList(),
                    fetchedAt = FAKE_FETCHED_AT
                )
            )

            assertEquals(
                expected = listOf(noon),
                actual = (observeWeatherForecastTask(isRefresh = true).first() as Either.Right).b.forecasts
            )
        }
}

private const val SAVED_AIR_TEMPERATURE = -3.0