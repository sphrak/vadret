package fi.kroon.vadret.pushalert.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PollenSubscriptionDto(
    @SerialName(value = "pollen_type_id")
    val pollenTypeId: String? = null,
    @SerialName(value = "lowest_level")
    val lowestLevel: Int? = null
)