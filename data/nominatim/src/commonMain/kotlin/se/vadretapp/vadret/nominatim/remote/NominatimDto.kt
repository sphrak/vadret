package se.vadretapp.vadret.nominatim.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class NominatimDto(
    @SerialName(value = "address")
    val address: AddressDto? = null
)