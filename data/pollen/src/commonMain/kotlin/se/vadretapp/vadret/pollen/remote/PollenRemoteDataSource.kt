package se.vadretapp.vadret.pollen.remote

internal interface PollenRemoteDataSource {

    suspend fun getRegions(): List<PollenRegionDto>

    suspend fun getPollenTypes(): List<PollenTypeDto>

    suspend fun getCurrentForecasts(regionId: String): List<PollenForecastDto>
}