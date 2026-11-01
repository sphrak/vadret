package se.vadretapp.vadret.pushalert.model

internal data class FavoriteRow(
    val favoriteId: String,
    val locality: String,
    val isChecked: Boolean,
    val isInSweden: Boolean
)