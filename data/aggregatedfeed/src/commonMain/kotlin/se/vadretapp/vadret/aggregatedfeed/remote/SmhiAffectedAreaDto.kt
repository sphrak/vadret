package se.vadretapp.vadret.aggregatedfeed.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SmhiAffectedAreaDto(
    @SerialName(value = "id")
    val id: Int,
    @SerialName(value = "sv")
    val sv: String? = null
)