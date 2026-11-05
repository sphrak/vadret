package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.flatMap
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.first
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.followedplace.extension.maximumFavorites
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.location.model.LocationPreference

internal class DefaultAddFavoriteTask(
    private val favoriteRepository: FavoriteRepository,
    private val locationRepository: LocationRepository
) : AddFavoriteTask {

    // Favorites are unique by name, so a name a favorite elsewhere already has leaves this place unsaved.
    override suspend fun invoke(
        locality: String,
        municipality: String,
        county: String,
        latitude: Double,
        longitude: Double
    ): Either<Failure, Favorite> =
        maximumFavorites(
            locality = locality,
            municipality = municipality,
            county = county
        ).flatMap { maximumFavorites: Int ->
            favoriteRepository.addFavorite(
                locality = locality,
                municipality = municipality,
                county = county,
                latitude = latitude,
                longitude = longitude,
                maximumFavorites = maximumFavorites
            )
        }.flatMap { favorite: Favorite ->
            if (favorite.latitude == latitude && favorite.longitude == longitude) {
                favorite.asRight()
            } else {
                Failure(errorCode = ErrorCode.FAVORITE_NAME_TAKEN).asLeft()
            }
        }

    // The chosen place stays chosen, so it keeps its room whether or not it is a saved place.
    private suspend fun maximumFavorites(
        locality: String,
        municipality: String,
        county: String
    ): Either<Failure, Int> =
        locationRepository
            .observeLocationPreference()
            .first()
            .flatMap { locationPreference: LocationPreference ->
                favoriteRepository
                    .observeFavorites()
                    .first()
                    .map { favorites: List<Favorite> ->
                        locationPreference.maximumFavorites(
                            favorites = favorites,
                            locality = locality,
                            municipality = municipality,
                            county = county
                        )
                    }
            }
}