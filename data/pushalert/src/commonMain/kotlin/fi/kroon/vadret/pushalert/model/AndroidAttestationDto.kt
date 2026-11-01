package fi.kroon.vadret.pushalert.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AndroidAttestationDto(
    @SerialName(value = "certificate_chain")
    val certificateChain: List<String>? = null
)