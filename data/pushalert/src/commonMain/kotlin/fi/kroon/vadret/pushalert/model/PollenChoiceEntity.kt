package fi.kroon.vadret.pushalert.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PollenChoiceEntity(
    @SerialName(value = "pollen_type_id")
    val pollenTypeId: String,
    @SerialName(value = "lowest_level")
    val lowestLevel: Int
)