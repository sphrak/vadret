package fi.kroon.vadret.pushalert.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SubscriptionErrorDto(
    @SerialName(value = "code")
    val code: String? = null
)