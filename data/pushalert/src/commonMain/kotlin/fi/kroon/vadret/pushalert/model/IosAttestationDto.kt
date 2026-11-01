package fi.kroon.vadret.pushalert.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class IosAttestationDto(
    @SerialName(value = "key_id")
    val keyId: String? = null,
    @SerialName(value = "attestation")
    val attestation: String? = null
)