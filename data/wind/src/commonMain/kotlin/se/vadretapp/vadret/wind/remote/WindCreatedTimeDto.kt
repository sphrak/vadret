package se.vadretapp.vadret.wind.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class WindCreatedTimeDto(
    @SerialName(value = "createdTime")
    val createdTime: String,
    @SerialName(value = "referenceTime")
    val referenceTime: String
)