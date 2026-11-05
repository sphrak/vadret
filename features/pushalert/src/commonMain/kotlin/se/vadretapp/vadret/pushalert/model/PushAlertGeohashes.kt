package se.vadretapp.vadret.pushalert.model

import se.vadretapp.vadret.geohash.model.Geohash

internal data class PushAlertGeohashes(
    val geohashes: List<Geohash>,
    val forecastGeohashes: List<Geohash>
)