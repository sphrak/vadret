package se.vadretapp.vadret.followedplace.model

import se.vadretapp.vadret.favorite.model.Favorite

data class FavoriteChoice(
    val favorite: Favorite,
    val isSelected: Boolean,
    val isFollowed: Boolean
)