package se.vadretapp.vadret.weatherforecast

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.place.model.Place

internal data class PlaceSearchState(
    val query: String = "",
    val suggestions: ImmutableList<Place> = persistentListOf(),
    val isPlaceChosen: Boolean = false,
    val errorMessageResource: StringResource? = null
)