package se.vadretapp.vadret.forecastwarning

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import se.vadretapp.vadret.forecastwarning.model.WarningItem

@Immutable
internal data class WarningStoryState(
    val warnings: ImmutableList<WarningItem> = persistentListOf(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = true,
    val isFinished: Boolean = false
)