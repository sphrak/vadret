package se.vadretapp.vadret.settings

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import se.vadretapp.vadret.settings.model.PollenGroupRows

internal data class PollenSettingsState(
    val isPollenEnabled: Boolean? = null,
    val isOffSeason: Boolean = false,
    val pollenGroupRows: ImmutableList<PollenGroupRows> = persistentListOf(),
    val arePollenTypesUnavailable: Boolean = false
)