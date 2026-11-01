package se.vadretapp.vadret.aggregatedfeed.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get

internal class DefaultSmhiWarningRemoteDataSource(
    private val httpClient: Lazy<HttpClient>
) : SmhiWarningRemoteDataSource {

    override suspend fun getWarnings(): List<SmhiWarningDto> =
        httpClient.value.get(urlString = "https://opendata-download-warnings.smhi.se/ibww/api/version/1/warning.json") {
            expectSuccess = true
        }.body()
}