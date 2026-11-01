package se.vadretapp.vadret.forecastwarning

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.forecastwarning.model.WarningItem

@Immutable
internal data class WarningDetailState(
    val warning: WarningItem? = null,
    val isLoading: Boolean = true,
    val messageResource: StringResource? = null
)