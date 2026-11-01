package se.vadretapp.vadret.pushalert.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class InstallationDto(
    @SerialName(value = "installation_token")
    val installationToken: String? = null
)