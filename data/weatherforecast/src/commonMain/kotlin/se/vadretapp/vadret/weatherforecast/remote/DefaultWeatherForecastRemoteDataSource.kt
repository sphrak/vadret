package se.vadretapp.vadret.weatherforecast.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.CacheControl
import io.ktor.http.HttpHeaders
import se.vadretapp.vadret.weatherforecast.extension.toCoordinateText

private const val MAXIMUM_ANALYSIS_AGE_SECONDS = 600

internal class DefaultWeatherForecastRemoteDataSource(
    private val httpClient: Lazy<HttpClient>
) : WeatherForecastRemoteDataSource {

    override suspend fun getWeatherForecast(
        latitude: Double,
        longitude: Double
    ): WeatherForecastDto {
        val formattedLongitude: String = longitude.toCoordinateText()
        val formattedLatitude: String = latitude.toCoordinateText()
        return httpClient.value.get(urlString = "https://opendata-download-metfcst.smhi.se/api/category/snow1g/version/1/geotype/point/lon/$formattedLongitude/lat/$formattedLatitude/data.json") {
            expectSuccess = true
        }.body()
    }

    override suspend fun getAnalysis(
        latitude: Double,
        longitude: Double
    ): AnalysisDto {
        val formattedLongitude: String = longitude.toCoordinateText()
        val formattedLatitude: String = latitude.toCoordinateText()
        return httpClient.value.get(urlString = "https://opendata-download-metanalys.smhi.se/api/category/mesan2g/version/3/geotype/point/lon/$formattedLongitude/lat/$formattedLatitude/data.json") {
            expectSuccess = true
            header(
                key = HttpHeaders.CacheControl,
                value = CacheControl.MaxAge(maxAgeSeconds = MAXIMUM_ANALYSIS_AGE_SECONDS).toString()
            )
        }.body()
    }
}