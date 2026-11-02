package se.vadretapp.vadret.forecastwarning

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.forecastwarning.model.WarningItem

@Immutable
internal data class WarningState(
    val warnings: ImmutableList<WarningItem> = persistentListOf(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isEmpty: Boolean = false,
    val followedPlaceNames: String? = null,
    val errorMessageResource: StringResource? = null
)