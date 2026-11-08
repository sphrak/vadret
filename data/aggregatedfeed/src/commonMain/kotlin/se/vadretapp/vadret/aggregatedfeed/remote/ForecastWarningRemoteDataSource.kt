package se.vadretapp.vadret.aggregatedfeed.remote

import se.vadretapp.vadret.geohash.model.Geohash

internal interface ForecastWarningRemoteDataSource {
    suspend fun getWarnings(
        installationToken: String,
        geohashes: List<Geohash>
    ): ForecastWarningsDto
}