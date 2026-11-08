package se.vadretapp.vadret.forecastwarning.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class AuroraHourItem(
    val hourText: String,
    val score: Int
)