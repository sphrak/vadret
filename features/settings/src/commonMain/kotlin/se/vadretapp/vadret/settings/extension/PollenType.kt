package se.vadretapp.vadret.settings.extension

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.settings.model.PollenAllergenRow

internal fun List<PollenType>.toPollenAllergenRows(allergenPollenIds: Set<String>): ImmutableList<PollenAllergenRow> =
    map { pollenType: PollenType ->
        PollenAllergenRow(
            pollenId = pollenType.pollenId,
            name = pollenType.name,
            isAllergen = pollenType.pollenId in allergenPollenIds
        )
    }.toImmutableList()