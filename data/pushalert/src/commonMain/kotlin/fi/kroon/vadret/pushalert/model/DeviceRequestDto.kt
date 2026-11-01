package fi.kroon.vadret.pushalert.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class DeviceRequestDto(
    @SerialName(value = "token")
    val token: String? = null,
    @SerialName(value = "public_key")
    val publicKey: String? = null,
    @SerialName(value = "replaces_token")
    val replacesToken: String? = null
)