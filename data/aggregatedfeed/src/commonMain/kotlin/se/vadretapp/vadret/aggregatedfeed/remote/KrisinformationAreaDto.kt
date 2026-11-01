package se.vadretapp.vadret.aggregatedfeed.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class KrisinformationAreaDto(
    @SerialName(value = "type")
    val type: String? = null,
    @SerialName(value = "description")
    val description: String? = null
)