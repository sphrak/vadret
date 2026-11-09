package se.vadretapp.vadret.forecastwarning.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class AuroraHourItem(
    val key: String,
    val isNow: Boolean,
    val hourText: String,
    val score: Int,
    val level: Float
)