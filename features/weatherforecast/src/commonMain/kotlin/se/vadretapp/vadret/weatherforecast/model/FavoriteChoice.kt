package se.vadretapp.vadret.weatherforecast.model

import se.vadretapp.vadret.favorite.model.Favorite

data class FavoriteChoice(
    val favorite: Favorite,
    val isSelected: Boolean
)