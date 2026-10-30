package se.vadretapp.vadret.settings.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class PollenAllergenRow(
    val pollenId: String,
    val name: String,
    val isAllergen: Boolean
)