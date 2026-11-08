package se.vadretapp.vadret.aggregatedfeed.model

import se.vadretapp.vadret.geohash.model.Geohash
import kotlin.time.Instant

data class Aurora(
    val chance: AuroraChance,
    val score: Int,
    val peakAt: Instant?,
    val geohash: Geohash,
    val auroraNight: String
)