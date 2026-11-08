package se.vadretapp.vadret.aurora.model

import se.vadretapp.vadret.geohash.model.Geohash

data class AuroraCellForecast(
    val geohash: Geohash,
    val hours: List<AuroraHour>
)