package fi.kroon.vadret.pollen.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PollenTypeDto(
    @SerialName(value = "id")
    val id: String,
    @SerialName(value = "name")
    val name: String
)