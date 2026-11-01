package se.vadretapp.vadret.aggregatedfeed.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class KrisinformationFeedDto(
    @SerialName(value = "identifier")
    val identifier: String,
    @SerialName(value = "published")
    val published: String,
    @SerialName(value = "headline")
    val headline: String,
    @SerialName(value = "preamble")
    val preamble: String? = null,
    @SerialName(value = "bodyText")
    val bodyText: String? = null,
    @SerialName(value = "senderName")
    val senderName: String? = null,
    @SerialName(value = "area")
    val area: List<KrisinformationAreaDto>? = null
)