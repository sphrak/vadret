package se.vadretapp.vadret.radar.model

import androidx.compose.runtime.Immutable
import se.vadretapp.vadret.pollen.model.PollenLevel

@Immutable
internal data class PollenMarker(
    val latitude: Double,
    val longitude: Double,
    val pollenLevel: PollenLevel
)