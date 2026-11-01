package se.vadretapp.vadret.aggregatedfeed.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SmhiTextDto(
    @SerialName(value = "sv")
    val sv: String,
    @SerialName(value = "code")
    val code: String? = null
)