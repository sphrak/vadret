package se.vadretapp.vadret.radar.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RadarCacheEntity(
    @SerialName(value = "fetched_at_epoch_millis")
    val fetchedAtEpochMillis: Long,
    @SerialName(value = "date")
    val date: String,
    @SerialName(value = "images")
    val images: List<RadarImageEntity>
)