package se.vadretapp.vadret.aggregatedfeed.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import se.vadretapp.vadret.geohash.model.Geohash

internal class DefaultForecastWarningRemoteDataSource(
    private val httpClient: Lazy<HttpClient>
) : ForecastWarningRemoteDataSource {

    override suspend fun getWarnings(
        installationToken: String,
        geohashes: List<Geohash>,
        isAuroraIncluded: Boolean
    ): ForecastWarningsDto =
        httpClient.value.get(urlString = "https://api.vadretapp.se/api/v1/warnings") {
            expectSuccess = true
            bearerAuth(token = installationToken)
            parameter(
                key = "geohashes",
                value = geohashes.joinToString(separator = GEOHASH_SEPARATOR) { geohash: Geohash -> geohash.value }
            )
            parameter(
                key = "aurora",
                value = isAuroraIncluded
            )
        }.body()
}

private const val GEOHASH_SEPARATOR = ","