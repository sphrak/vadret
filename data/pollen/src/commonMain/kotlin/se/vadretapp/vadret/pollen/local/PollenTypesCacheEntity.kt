package se.vadretapp.vadret.pollen.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PollenTypesCacheEntity(
    @SerialName(value = "fetched_at_epoch_millis")
    val fetchedAtEpochMillis: Long,
    @SerialName(value = "pollen_types")
    val pollenTypes: List<PollenTypeEntity>
)