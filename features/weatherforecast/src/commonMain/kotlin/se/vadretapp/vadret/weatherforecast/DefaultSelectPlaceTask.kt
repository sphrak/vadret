package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import io.github.sphrak.either.flatMap
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.place.model.Place
import se.vadretapp.vadret.weatherforecast.extension.toStoredLocation

internal class DefaultSelectPlaceTask(
    private val favoriteRepository: FavoriteRepository,
    private val locationRepository: LocationRepository
) : SelectPlaceTask {

    override suspend fun invoke(place: Place): Either<Failure, Unit> =
        favoriteRepository
            .addFavorite(
                locality = place.locality,
                municipality = place.municipality,
                county = place.county,
                latitude = place.latitude,
                longitude = place.longitude
            ).flatMap { favorite: Favorite -> locationRepository.selectStoredLocation(storedLocation = favorite.toStoredLocation()) }
}