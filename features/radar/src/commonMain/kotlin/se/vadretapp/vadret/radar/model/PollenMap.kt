package se.vadretapp.vadret.radar.model

import kotlinx.datetime.LocalDate
import se.vadretapp.vadret.pollen.model.PollenType

internal data class PollenMap(
    val date: LocalDate,
    val pollenTypes: List<PollenType>,
    val stations: List<PollenStation>
)