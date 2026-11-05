package se.vadretapp.vadret.settings.extension

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.settings.model.PollenAllergenRow
import se.vadretapp.vadret.settings.model.PollenGroup
import se.vadretapp.vadret.settings.model.PollenGroupRows

// Pollenrapporten has no groups, so its pollen ids are sorted into the groups people know them by.
private val treePollenIds: Set<String> = setOf(
    "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323233",
    "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323236",
    "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323239",
    "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323330",
    "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323331",
    "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323332",
    "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323335",
    "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323337",
    "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323338"
)
private val grassAndHerbPollenIds: Set<String> = setOf(
    "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323433",
    "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323530",
    "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323533"
)
private val sporePollenIds: Set<String> = setOf(
    "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323833",
    "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323834",
    "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323839"
)

internal fun List<PollenType>.toPollenGroupRows(allergenPollenIds: Set<String>): ImmutableList<PollenGroupRows> {
    val pollenTypesByGroup: Map<PollenGroup, List<PollenType>> = groupBy { pollenType: PollenType -> pollenType.pollenGroup() }
    return PollenGroup.entries
        .mapNotNull { pollenGroup: PollenGroup ->
            val pollenTypes: List<PollenType> = pollenTypesByGroup[pollenGroup].orEmpty()
            if (pollenTypes.isEmpty()) {
                null
            } else {
                PollenGroupRows(
                    pollenGroup = pollenGroup,
                    pollenAllergenRows = pollenTypes.toPollenAllergenRows(allergenPollenIds = allergenPollenIds)
                )
            }
        }.toImmutableList()
}

private fun List<PollenType>.toPollenAllergenRows(allergenPollenIds: Set<String>): ImmutableList<PollenAllergenRow> =
    map { pollenType: PollenType ->
        PollenAllergenRow(
            pollenId = pollenType.pollenId,
            name = pollenType.name,
            isAllergen = pollenType.pollenId in allergenPollenIds
        )
    }.toImmutableList()

private fun PollenType.pollenGroup(): PollenGroup =
    when (pollenId) {
        in treePollenIds -> PollenGroup.TREES
        in grassAndHerbPollenIds -> PollenGroup.GRASSES_AND_HERBS
        in sporePollenIds -> PollenGroup.SPORES
        else -> PollenGroup.OTHER
    }