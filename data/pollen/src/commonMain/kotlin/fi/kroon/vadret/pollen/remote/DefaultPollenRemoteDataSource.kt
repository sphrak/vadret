package fi.kroon.vadret.pollen.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.parameter

private const val PAGE_LIMIT = 100
private const val CURRENT_FORECASTS_ONLY = true

internal class DefaultPollenRemoteDataSource(
    private val httpClient: Lazy<HttpClient>
) : PollenRemoteDataSource {

    override suspend fun getRegions(): List<PollenRegionDto> =
        httpClient.value.get(urlString = "https://api.pollenrapporten.se/v1/regions") {
            expectSuccess = true
            parameter(key = "limit", value = PAGE_LIMIT)
        }.body<PollenPageDto<PollenRegionDto>>().items

    override suspend fun getPollenTypes(): List<PollenTypeDto> =
        httpClient.value.get(urlString = "https://api.pollenrapporten.se/v1/pollen-types") {
            expectSuccess = true
            parameter(key = "limit", value = PAGE_LIMIT)
        }.body<PollenPageDto<PollenTypeDto>>().items

    override suspend fun getCurrentForecasts(regionId: String): List<PollenForecastDto> =
        httpClient.value.get(urlString = "https://api.pollenrapporten.se/v1/forecasts") {
            expectSuccess = true
            parameter(key = "region_id", value = regionId)
            parameter(key = "current", value = CURRENT_FORECASTS_ONLY)
            parameter(key = "limit", value = PAGE_LIMIT)
        }.body<PollenPageDto<PollenForecastDto>>().items
}