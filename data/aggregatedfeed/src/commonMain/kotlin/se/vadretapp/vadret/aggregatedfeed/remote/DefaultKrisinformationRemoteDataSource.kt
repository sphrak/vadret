package se.vadretapp.vadret.aggregatedfeed.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get

internal class DefaultKrisinformationRemoteDataSource(
    private val httpClient: Lazy<HttpClient>
) : KrisinformationRemoteDataSource {

    override suspend fun getNews(): List<KrisinformationFeedDto> =
        httpClient.value.get(urlString = "https://api.krisinformation.se/v3/news") {
            expectSuccess = true
        }.body()

    override suspend fun getPublicAlerts(): List<KrisinformationFeedDto> =
        httpClient.value.get(urlString = "https://api.krisinformation.se/v3/vmas") {
            expectSuccess = true
        }.body()
}