package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import io.github.sphrak.either.flatMap
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.followedplace.extension.toStoredLocation
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.place.model.Place

internal class DefaultSelectPlaceTask(
    private val favoriteRepository: FavoriteRepository,
    private val locationRepository: LocationRepository
) : SelectPlaceTask {

    // The new place becomes the chosen one, so it fills a room of the limit instead of the place chosen before.
    override suspend fun invoke(place: Place): Either<Failure, Unit> =
        favoriteRepository
            .addFavorite(
                locality = place.locality,
                municipality = place.municipality,
                county = place.county,
                latitude = place.latitude,
                longitude = place.longitude,
                maximumFavorites = MAXIMUM_FOLLOWED_PLACES
            ).flatMap { favorite: Favorite ->
                locationRepository.selectStoredLocation(storedLocation = favorite.toStoredLocation())
            }
}