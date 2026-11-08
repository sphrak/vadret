package se.vadretapp.vadret.aurora.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import se.vadretapp.vadret.geohash.model.Geohash

internal class DefaultAuroraRemoteDataSource(
    private val httpClient: Lazy<HttpClient>
) : AuroraRemoteDataSource {

    override suspend fun getAuroraForecast(
        installationToken: String,
        geohashes: List<Geohash>
    ): AuroraForecastDto =
        httpClient.value.get(urlString = "https://api.vadretapp.se/api/v1/aurora") {
            expectSuccess = true
            bearerAuth(token = installationToken)
            parameter(
                key = "geohashes",
                value = geohashes.joinToString(separator = GEOHASH_SEPARATOR) { geohash: Geohash -> geohash.value }
            )
        }.body()
}

private const val GEOHASH_SEPARATOR = ","