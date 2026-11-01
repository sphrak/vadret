package se.vadretapp.vadret.pollen.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PollenPageDto<T>(
    @SerialName(value = "items")
    val items: List<T>
)