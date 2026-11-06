package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.weatherforecast.model.PlaceForecast
import se.vadretapp.vadret.weatherforecast.model.WeatherPlaceKind
import se.vadretapp.vadret.weatherforecast.usecase.DefaultObservePlaceForecastTask
import se.vadretapp.vadret.weatherforecast.usecase.ObservePlaceForecastTask
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultObservePlaceForecastTaskTest {

    private val weatherForecastRepository = FakeWeatherForecastRepository()
    private val nominatimRepository = FakeNominatimRepository()
    private val observePlaceForecastTask: ObservePlaceForecastTask = DefaultObservePlaceForecastTask(
        weatherForecastRepository = weatherForecastRepository,
        nominatimRepository = nominatimRepository
    )

    @Test
    fun `a place gets its forecast and the name Nominatim knows it by`() =
        runTest {
            val placeForecast: Either<Failure, PlaceForecast> = observePlaceForecastTask(
                latitude = GOTHENBURG_LATITUDE,
                longitude = GOTHENBURG_LONGITUDE,
                isRefresh = false
            ).first()

            assertEquals(
                expected = PlaceForecast(
                    placeName = "Göteborg",
                    municipality = "Göteborgs kommun",
                    county = "Västra Götalands län",
                    latitude = GOTHENBURG_LATITUDE,
                    longitude = GOTHENBURG_LONGITUDE,
                    forecasts = listOf(noon),
                    analyses = emptyList(),
                    fetchedAt = FAKE_FETCHED_AT,
                    isRefreshFailed = false
                ).asRight(),
                actual = placeForecast
            )
            assertEquals(expected = listOf(GOTHENBURG_LATITUDE to GOTHENBURG_LONGITUDE), actual = weatherForecastRepository.requestedCoordinates)
        }

    @Test
    fun `a place Nominatim cannot name still gets its forecast`() =
        runTest {
            nominatimRepository.locality = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()

            val placeForecast: Either<Failure, PlaceForecast> = observePlaceForecastTask(
                latitude = GOTHENBURG_LATITUDE,
                longitude = GOTHENBURG_LONGITUDE,
                isRefresh = false
            ).first()

            assertEquals(
                expected = PlaceForecast(
                    placeName = null,
                    municipality = null,
                    county = null,
                    latitude = GOTHENBURG_LATITUDE,
                    longitude = GOTHENBURG_LONGITUDE,
                    forecasts = listOf(noon),
                    analyses = emptyList(),
                    fetchedAt = FAKE_FETCHED_AT,
                    isRefreshFailed = false
                ).asRight(),
                actual = placeForecast
            )
        }

    @Test
    fun `a forecast that fails is the failure`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.WEATHER_ANALYSIS_NOT_AVAILABLE_FOR_LOCATION)
            weatherForecastRepository.forecasts = failure.asLeft()

            val placeForecast: Either<Failure, PlaceForecast> = observePlaceForecastTask(
                latitude = GOTHENBURG_LATITUDE,
                longitude = GOTHENBURG_LONGITUDE,
                isRefresh = false
            ).first()

            assertEquals(expected = failure.asLeft(), actual = placeForecast)
        }

    @Test
    fun `a place gets the analyses of the hours that already passed`() =
        runTest {
            weatherForecastRepository.analyses = listOf(morning).asRight()

            val placeForecast: Either<Failure, PlaceForecast> = observePlaceForecastTask(
                latitude = GOTHENBURG_LATITUDE,
                longitude = GOTHENBURG_LONGITUDE,
                isRefresh = false
            ).first()

            assertEquals(
                expected = PlaceForecast(
                    placeName = "Göteborg",
                    municipality = "Göteborgs kommun",
                    county = "Västra Götalands län",
                    latitude = GOTHENBURG_LATITUDE,
                    longitude = GOTHENBURG_LONGITUDE,
                    forecasts = listOf(noon),
                    analyses = listOf(morning),
                    fetchedAt = FAKE_FETCHED_AT,
                    isRefreshFailed = false
                ).asRight(),
                actual = placeForecast
            )
        }

    @Test
    fun `a dropped pin is refreshed without being saved`() =
        runTest {
            observePlaceForecastTask(
                latitude = GOTHENBURG_LATITUDE,
                longitude = GOTHENBURG_LONGITUDE,
                isRefresh = false
            ).first()

            assertEquals(
                expected = listOf(WeatherPlaceKind.DROPPED_PIN) to emptyMap(),
                actual = weatherForecastRepository.requestedWeatherPlaceKinds to weatherForecastRepository.savedWeather.value
            )
        }

    @Test
    fun `a refresh asks for the weather before showing it`() =
        runTest {
            assertEquals(
                expected = listOf(noon) to 1,
                actual = (
                    observePlaceForecastTask(
                        latitude = GOTHENBURG_LATITUDE,
                        longitude = GOTHENBURG_LONGITUDE,
                        isRefresh = true
                    ).first() as Either.Right
                    ).b.forecasts to weatherForecastRepository.requestedCoordinates.size
            )
        }
}

private const val GOTHENBURG_LATITUDE = 57.7089
private const val GOTHENBURG_LONGITUDE = 11.9746