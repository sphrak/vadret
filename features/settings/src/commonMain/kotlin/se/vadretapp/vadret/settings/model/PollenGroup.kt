package se.vadretapp.vadret.settings.model

import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.pollen_group_grasses_and_herbs
import se.vadretapp.vadret.ui.resources.pollen_group_other
import se.vadretapp.vadret.ui.resources.pollen_group_spores
import se.vadretapp.vadret.ui.resources.pollen_group_trees

internal enum class PollenGroup(
    val titleResource: StringResource
) {
    TREES(titleResource = Res.string.pollen_group_trees),
    GRASSES_AND_HERBS(titleResource = Res.string.pollen_group_grasses_and_herbs),
    SPORES(titleResource = Res.string.pollen_group_spores),
    OTHER(titleResource = Res.string.pollen_group_other)
}