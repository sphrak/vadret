package se.vadretapp.vadret.radar.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RadarImageEntity(
    @SerialName(value = "image_url")
    val imageUrl: String,
    @SerialName(value = "valid_at_epoch_millis")
    val validAtEpochMillis: Long
)