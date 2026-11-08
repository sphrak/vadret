package se.vadretapp.vadret.pushalert.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Betas before one pollen level stored a level per pollen type, and only that level is still read.
@Serializable
internal data class PollenChoiceEntity(
    @SerialName(value = "lowest_level")
    val lowestLevel: Int
)