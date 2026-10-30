package se.vadretapp.vadret.radar.model

import se.vadretapp.vadret.pollen.model.PollenLevel

internal data class PollenStation(
    val regionId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val pollenLevels: Map<String, PollenLevel>
)