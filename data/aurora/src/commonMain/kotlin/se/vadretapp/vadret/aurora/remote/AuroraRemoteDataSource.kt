package se.vadretapp.vadret.aurora.remote

import se.vadretapp.vadret.geohash.model.Geohash

internal interface AuroraRemoteDataSource {
    suspend fun getAuroraForecast(
        installationToken: String,
        geohashes: List<Geohash>
    ): AuroraForecastDto
}