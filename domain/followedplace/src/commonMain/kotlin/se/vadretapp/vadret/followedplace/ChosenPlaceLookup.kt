package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.extension.onResult
import io.github.sphrak.either.flatMap
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.followedplace.extension.chosenFavorite
import se.vadretapp.vadret.followedplace.extension.toChosenPlace
import se.vadretapp.vadret.followedplace.model.FollowedPlace
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.location.model.LocationPreference
import se.vadretapp.vadret.nominatim.NominatimRepository
import se.vadretapp.vadret.nominatim.model.Locality

internal class ChosenPlaceLookup(
    private val locationRepository: LocationRepository,
    private val nominatimRepository: NominatimRepository
) {

    suspend fun chosenPlace(
        locationPreference: LocationPreference,
        favorites: List<Favorite>
    ): Either<Failure, FollowedPlace> =
        if (locationPreference.isAutomatic) {
            devicePlace()
        } else {
            locationPreference.storedLocation.toChosenPlace(chosenFavorite = locationPreference.chosenFavorite(favorites = favorites)).asRight()
        }

    // A device position Nominatim cannot name is still the chosen place, known by its coordinates.
    private suspend fun devicePlace(): Either<Failure, FollowedPlace> =
        locationRepository
            .getDeviceLocation()
            .flatMap { location: Location ->
                val locality: Locality? = nominatimRepository
                    .getLocality(
                        latitude = location.latitude,
                        longitude = location.longitude
                    ).onResult(
                        onError = { _: Failure -> null },
                        onSuccess = { locality: Locality -> locality }
                    )
                FollowedPlace(
                    favoriteId = null,
                    name = locality?.name,
                    municipality = locality?.municipality,
                    county = locality?.county,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    isChosen = true
                ).asRight()
            }
}