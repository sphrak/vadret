package se.vadretapp.vadret.aggregatedfeed.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class FeedDetailEntity(
    @SerialName(value = "kind")
    val kind: String,
    @SerialName(value = "title")
    val title: String,
    @SerialName(value = "text")
    val text: String
)