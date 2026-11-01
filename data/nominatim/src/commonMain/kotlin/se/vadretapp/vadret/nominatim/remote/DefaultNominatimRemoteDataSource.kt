package se.vadretapp.vadret.nominatim.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.parameter

private const val RESPONSE_FORMAT = "json"

private const val CITY_ZOOM_LEVEL = 16

internal class DefaultNominatimRemoteDataSource(
    private val httpClient: Lazy<HttpClient>
) : NominatimRemoteDataSource {

    override suspend fun reverse(
        latitude: Double,
        longitude: Double
    ): NominatimDto =
        httpClient.value.get(urlString = "https://nominatim.openstreetmap.org/reverse") {
            expectSuccess = true
            parameter(key = "format", value = RESPONSE_FORMAT)
            parameter(key = "lat", value = latitude)
            parameter(key = "lon", value = longitude)
            parameter(key = "zoom", value = CITY_ZOOM_LEVEL)
        }.body()
}