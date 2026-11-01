package se.vadretapp.vadret.aggregatedfeed.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class FeedRegionEntity(
    @SerialName(value = "kind")
    val kind: String,
    @SerialName(value = "name")
    val name: String
)