package se.vadretapp.vadret.pollen.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PollenRegionDto(
    @SerialName(value = "id")
    val id: String,
    @SerialName(value = "name")
    val name: String,
    @SerialName(value = "latitude")
    val latitude: String? = null,
    @SerialName(value = "longitude")
    val longitude: String? = null
)