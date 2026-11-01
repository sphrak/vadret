package se.vadretapp.vadret.nominatim.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AddressDto(
    @SerialName(value = "city")
    val city: String? = null,
    @SerialName(value = "hamlet")
    val hamlet: String? = null,
    @SerialName(value = "village")
    val village: String? = null,
    @SerialName(value = "municipality")
    val municipality: String? = null,
    @SerialName(value = "county")
    val county: String? = null
)