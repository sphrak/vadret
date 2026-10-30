package se.vadretapp.vadret.pollen.remote

import kotlinx.datetime.LocalDate

internal interface PollenRemoteDataSource {

    suspend fun getRegions(): List<PollenRegionDto>

    suspend fun getPollenTypes(): List<PollenTypeDto>

    suspend fun getForecasts(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<PollenForecastDto>
}