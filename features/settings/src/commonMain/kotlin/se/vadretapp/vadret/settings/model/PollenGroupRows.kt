package se.vadretapp.vadret.settings.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class PollenGroupRows(
    val pollenGroup: PollenGroup,
    val pollenAllergenRows: ImmutableList<PollenAllergenRow>
)