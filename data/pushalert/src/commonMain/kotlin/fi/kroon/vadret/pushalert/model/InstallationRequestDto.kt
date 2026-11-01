package fi.kroon.vadret.pushalert.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class InstallationRequestDto(
    @SerialName(value = "challenge")
    val challenge: String? = null,
    @SerialName(value = "android")
    val android: AndroidAttestationDto? = null,
    @SerialName(value = "ios")
    val ios: IosAttestationDto? = null
)