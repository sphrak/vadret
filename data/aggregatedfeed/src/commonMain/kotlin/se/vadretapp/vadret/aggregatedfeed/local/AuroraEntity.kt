package se.vadretapp.vadret.aggregatedfeed.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AuroraEntity(
    @SerialName(value = "chance")
    val chance: String,
    @SerialName(value = "score")
    val score: Int,
    @SerialName(value = "peak_at_epoch_millis")
    val peakAtEpochMillis: Long? = null,
    @SerialName(value = "geohash")
    val geohash: String,
    @SerialName(value = "aurora_night")
    val auroraNight: String
)