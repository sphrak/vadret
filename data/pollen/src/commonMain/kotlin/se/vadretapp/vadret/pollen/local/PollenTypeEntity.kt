package se.vadretapp.vadret.pollen.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PollenTypeEntity(
    @SerialName(value = "pollen_id")
    val pollenId: String,
    @SerialName(value = "name")
    val name: String,
    @SerialName(value = "has_forecasts")
    val hasForecasts: Boolean
)