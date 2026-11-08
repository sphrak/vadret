package se.vadretapp.vadret.aggregatedfeed.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SlipperinessEntity(
    @SerialName(value = "risk")
    val risk: String,
    @SerialName(value = "causes")
    val causes: List<String>
)