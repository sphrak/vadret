package se.vadretapp.vadret.followedplace.model

import kotlinx.collections.immutable.ImmutableList

data class LocationChoices(
    val isAutomatic: Boolean,
    val chosenPlaceName: String?,
    val favorites: ImmutableList<FavoriteChoice>
)