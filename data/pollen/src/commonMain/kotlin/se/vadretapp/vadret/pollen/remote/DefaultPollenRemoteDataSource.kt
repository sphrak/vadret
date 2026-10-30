package se.vadretapp.vadret.pollen.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.datetime.LocalDate

private const val PAGE_LIMIT = 100
private const val MAXIMUM_FORECAST_PAGES = 10

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

    override suspend fun getForecasts(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<PollenForecastDto> {
        val pollenForecastDtos: MutableList<PollenForecastDto> = mutableListOf()
        var pageIndex = 0
        do {
            val forecastPage: PollenPageDto<PollenForecastDto> = getForecastPage(
                startDate = startDate,
                endDate = endDate,
                offset = pageIndex * PAGE_LIMIT
            )
            pollenForecastDtos.addAll(elements = forecastPage.items)
            pageIndex += 1
            val hasMorePages: Boolean = forecastPage.items.isNotEmpty() && pollenForecastDtos.size < (forecastPage.meta?.totalRecords ?: pollenForecastDtos.size)
        } while (hasMorePages && pageIndex < MAXIMUM_FORECAST_PAGES)
        return pollenForecastDtos
    }

    private suspend fun getForecastPage(
        startDate: LocalDate,
        endDate: LocalDate,
        offset: Int
    ): PollenPageDto<PollenForecastDto> =
        httpClient.value.get(urlString = "https://api.pollenrapporten.se/v1/forecasts") {
            expectSuccess = true
            parameter(key = "start_date", value = startDate)
            parameter(key = "end_date", value = endDate)
            parameter(key = "limit", value = PAGE_LIMIT)
            parameter(key = "offset", value = offset)
        }.body<PollenPageDto<PollenForecastDto>>()

    override suspend fun getCurrentForecasts(): List<PollenForecastDto> =
        httpClient.value.get(urlString = "https://api.pollenrapporten.se/v1/forecasts") {
            expectSuccess = true
            parameter(key = "current", value = true)
            parameter(key = "limit", value = PAGE_LIMIT)
        }.body<PollenPageDto<PollenForecastDto>>().items
}