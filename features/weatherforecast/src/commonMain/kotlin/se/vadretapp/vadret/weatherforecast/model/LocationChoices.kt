package se.vadretapp.vadret.weatherforecast.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class LocationChoices(
    val isAutomatic: Boolean = true,
    val favorites: ImmutableList<FavoriteChoice> = persistentListOf()
)