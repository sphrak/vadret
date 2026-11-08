package se.vadretapp.vadret.aurora.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AuroraForecastDto(
    @SerialName(value = "cells")
    val cells: List<AuroraCellDto>? = null
)