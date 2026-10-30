package se.vadretapp.vadret.pollen.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PollenPageMetaDto(
    @SerialName(value = "totalRecords")
    val totalRecords: Int
)