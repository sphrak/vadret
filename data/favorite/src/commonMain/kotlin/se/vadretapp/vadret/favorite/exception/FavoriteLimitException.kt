package se.vadretapp.vadret.favorite.exception

private const val FAVORITE_LIMIT_REACHED = "As many favorites as allowed are already saved"

internal class FavoriteLimitException internal constructor() : Exception(FAVORITE_LIMIT_REACHED)