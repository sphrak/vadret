package se.vadretapp.vadret.pushalert.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SealedContentDto(
    @SerialName(value = "title")
    val title: String? = null,
    @SerialName(value = "body")
    val body: String? = null,
    @SerialName(value = "data")
    val data: Map<String, String>? = null
)