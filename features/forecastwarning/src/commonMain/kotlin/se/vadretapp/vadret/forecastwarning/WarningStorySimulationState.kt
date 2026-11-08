package se.vadretapp.vadret.forecastwarning

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import se.vadretapp.vadret.forecastwarning.model.WarningItem

@Immutable
internal data class WarningStorySimulationState(
    val warnings: ImmutableList<WarningItem> = persistentListOf()
) {

    val warningIdentifiers: List<String> = warnings.map { warningItem: WarningItem -> warningItem.identifier }
}