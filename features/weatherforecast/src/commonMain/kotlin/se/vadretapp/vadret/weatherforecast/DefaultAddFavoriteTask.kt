package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.flatMap
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.favorite.model.Favorite

internal class DefaultAddFavoriteTask(
    private val favoriteRepository: FavoriteRepository
) : AddFavoriteTask {

    // Favorites are unique by name, so a name a favorite elsewhere already has leaves this place unsaved.
    override suspend fun invoke(
        locality: String,
        municipality: String,
        county: String,
        latitude: Double,
        longitude: Double
    ): Either<Failure, Favorite> =
        favoriteRepository
            .addFavorite(
                locality = locality,
                municipality = municipality,
                county = county,
                latitude = latitude,
                longitude = longitude
            ).flatMap { favorite: Favorite ->
                if (favorite.latitude == latitude && favorite.longitude == longitude) {
                    favorite.asRight()
                } else {
                    Failure(errorCode = ErrorCode.FAVORITE_NAME_TAKEN).asLeft()
                }
            }
}