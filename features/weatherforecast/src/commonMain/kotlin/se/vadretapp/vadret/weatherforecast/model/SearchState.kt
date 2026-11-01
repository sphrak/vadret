package se.vadretapp.vadret.weatherforecast.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import se.vadretapp.vadret.place.model.Place

data class SearchState(
    val query: String = "",
    val suggestions: ImmutableList<Place> = persistentListOf()
)