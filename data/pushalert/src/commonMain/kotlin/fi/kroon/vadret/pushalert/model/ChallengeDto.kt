package fi.kroon.vadret.pushalert.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ChallengeDto(
    @SerialName(value = "challenge")
    val challenge: String? = null
)