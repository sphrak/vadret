package se.vadretapp.vadret.aggregatedfeed.model

import se.vadretapp.vadret.geohash.model.Geohash

data class ForecastWarningRequest(
    val installationToken: String,
    val geohashes: List<Geohash>
)