package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.nominatim.model.Locality
import se.vadretapp.vadret.weatherforecast.model.PlaceForecast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultGetWeatherForecastTaskTest {

    private val locationRepository = FakeLocationRepository()
    private val weatherForecastRepository = FakeWeatherForecastRepository(analyses = listOf(morning).asRight())
    private val nominatimRepository = FakeNominatimRepository()
    private val getWeatherForecastTask: GetWeatherForecastTask = DefaultGetWeatherForecastTask(
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
                    analyses = listOf(morning)
                ).asRight(),
                actual = getWeatherForecastTask()
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
                    analyses = listOf(morning)
                ).asRight(),
                actual = getWeatherForecastTask()
            )
            assertEquals(expected = listOf(57.7 to 11.97), actual = weatherForecastRepository.requestedCoordinates)
            assertEquals(expected = listOf(57.7 to 11.97), actual = weatherForecastRepository.requestedAnalysisCoordinates)
        }

    @Test
    fun `a failed forecast stops asking for the past hours`() =
        runTest(context = UnconfinedTestDispatcher()) {
            val forecastFailure = Failure(errorCode = ErrorCode.HTTP_SERVICE_UNAVAILABLE)
            weatherForecastRepository.analysisGate = CompletableDeferred()
            weatherForecastRepository.forecasts = forecastFailure.asLeft()

            val result: Either<Failure, PlaceForecast> = getWeatherForecastTask()

            assertEquals(expected = forecastFailure.asLeft(), actual = result)
            assertTrue(actual = weatherForecastRepository.isAnalysisCancelled)
        }

    @Test
    fun `a place without its past hours still gets its forecast`() =
        runTest {
            weatherForecastRepository.analyses = Failure(errorCode = ErrorCode.HTTP_SERVICE_UNAVAILABLE).asLeft()

            assertEquals<Either<Failure, PlaceForecast>>(
                expected = PlaceForecast(
                    placeName = "Stockholm",
                    municipality = "Stockholm",
                    county = "Stockholm",
                    latitude = 59.3293,
                    longitude = 18.0686,
                    forecasts = listOf(noon),
                    analyses = emptyList()
                ).asRight(),
                actual = getWeatherForecastTask()
            )
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
                val placeForecast: PlaceForecast = (getWeatherForecastTask() as Either.Right).b
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
            val withoutMode: Either<Failure, PlaceForecast> = getWeatherForecastTask()
            locationRepository.isAutomatic = true.asRight()
            locationRepository.deviceLocation = locationFailure.asLeft()
            val withoutLocation: Either<Failure, PlaceForecast> = getWeatherForecastTask()
            locationRepository.isAutomatic = false.asRight()
            weatherForecastRepository.forecasts = forecastFailure.asLeft()
            val withoutForecast: Either<Failure, PlaceForecast> = getWeatherForecastTask()

            assertEquals(
                expected = listOf(modeFailure, locationFailure, forecastFailure).map { failure: Failure -> failure.asLeft() },
                actual = listOf(withoutMode, withoutLocation, withoutForecast)
            )
            assertTrue(actual = weatherForecastRepository.requestedCoordinates.size == 1)
        }
}