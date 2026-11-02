package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.weatherforecast.model.PlaceForecast
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultGetPlaceForecastTaskTest {

    private val weatherForecastRepository = FakeWeatherForecastRepository()
    private val nominatimRepository = FakeNominatimRepository()
    private val getPlaceForecastTask = DefaultGetPlaceForecastTask(
        weatherForecastRepository = weatherForecastRepository,
        nominatimRepository = nominatimRepository
    )

    @Test
    fun `a place gets its forecast and the name Nominatim knows it by`() =
        runTest {
            val placeForecast: Either<Failure, PlaceForecast> = getPlaceForecastTask(
                latitude = GOTHENBURG_LATITUDE,
                longitude = GOTHENBURG_LONGITUDE
            )

            assertEquals(
                expected = PlaceForecast(
                    placeName = "Göteborg",
                    municipality = "Göteborgs kommun",
                    county = "Västra Götalands län",
                    latitude = GOTHENBURG_LATITUDE,
                    longitude = GOTHENBURG_LONGITUDE,
                    forecasts = listOf(noon),
                    analyses = emptyList()
                ).asRight(),
                actual = placeForecast
            )
            assertEquals(expected = listOf(GOTHENBURG_LATITUDE to GOTHENBURG_LONGITUDE), actual = weatherForecastRepository.requestedCoordinates)
        }

    @Test
    fun `a place Nominatim cannot name still gets its forecast`() =
        runTest {
            nominatimRepository.locality = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()

            val placeForecast: Either<Failure, PlaceForecast> = getPlaceForecastTask(
                latitude = GOTHENBURG_LATITUDE,
                longitude = GOTHENBURG_LONGITUDE
            )

            assertEquals(
                expected = PlaceForecast(
                    placeName = null,
                    municipality = null,
                    county = null,
                    latitude = GOTHENBURG_LATITUDE,
                    longitude = GOTHENBURG_LONGITUDE,
                    forecasts = listOf(noon),
                    analyses = emptyList()
                ).asRight(),
                actual = placeForecast
            )
        }

    @Test
    fun `a forecast that fails is the failure`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.WEATHER_ANALYSIS_NOT_AVAILABLE_FOR_LOCATION)
            weatherForecastRepository.forecasts = failure.asLeft()

            val placeForecast: Either<Failure, PlaceForecast> = getPlaceForecastTask(
                latitude = GOTHENBURG_LATITUDE,
                longitude = GOTHENBURG_LONGITUDE
            )

            assertEquals(expected = failure.asLeft(), actual = placeForecast)
        }

    @Test
    fun `a place gets the analyses of the hours that already passed`() =
        runTest {
            weatherForecastRepository.analyses = listOf(morning).asRight()

            val placeForecast: Either<Failure, PlaceForecast> = getPlaceForecastTask(
                latitude = GOTHENBURG_LATITUDE,
                longitude = GOTHENBURG_LONGITUDE
            )

            assertEquals(
                expected = PlaceForecast(
                    placeName = "Göteborg",
                    municipality = "Göteborgs kommun",
                    county = "Västra Götalands län",
                    latitude = GOTHENBURG_LATITUDE,
                    longitude = GOTHENBURG_LONGITUDE,
                    forecasts = listOf(noon),
                    analyses = listOf(morning)
                ).asRight(),
                actual = placeForecast
            )
            assertEquals(expected = listOf(GOTHENBURG_LATITUDE to GOTHENBURG_LONGITUDE), actual = weatherForecastRepository.requestedAnalysisCoordinates)
        }

    @Test
    fun `analyses that fail leave the place with its forecast`() =
        runTest {
            weatherForecastRepository.analyses = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()

            val placeForecast: Either<Failure, PlaceForecast> = getPlaceForecastTask(
                latitude = GOTHENBURG_LATITUDE,
                longitude = GOTHENBURG_LONGITUDE
            )

            assertEquals(
                expected = PlaceForecast(
                    placeName = "Göteborg",
                    municipality = "Göteborgs kommun",
                    county = "Västra Götalands län",
                    latitude = GOTHENBURG_LATITUDE,
                    longitude = GOTHENBURG_LONGITUDE,
                    forecasts = listOf(noon),
                    analyses = emptyList()
                ).asRight(),
                actual = placeForecast
            )
        }

    @Test
    fun `a forecast that fails stops waiting for the analyses`() =
        runTest(context = UnconfinedTestDispatcher()) {
            val failure = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE)
            weatherForecastRepository.forecasts = failure.asLeft()
            weatherForecastRepository.analysisGate = CompletableDeferred()

            val placeForecast: Either<Failure, PlaceForecast> = getPlaceForecastTask(
                latitude = GOTHENBURG_LATITUDE,
                longitude = GOTHENBURG_LONGITUDE
            )

            assertEquals(expected = failure.asLeft(), actual = placeForecast)
            assertEquals(expected = true, actual = weatherForecastRepository.isAnalysisCancelled)
        }
}

private const val GOTHENBURG_LATITUDE = 57.7089
private const val GOTHENBURG_LONGITUDE = 11.9746