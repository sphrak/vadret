package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.extension.onResult
import io.github.sphrak.either.flatMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.followedplace.model.FollowableFavorite
import se.vadretapp.vadret.followedplace.model.FollowablePlaces
import se.vadretapp.vadret.followedplace.model.FollowedPlace
import se.vadretapp.vadret.geohash.model.geohashOf
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.location.model.LocationPreference

internal class DefaultObserveFollowablePlacesTask(
    private val locationRepository: LocationRepository,
    private val favoriteRepository: FavoriteRepository,
    private val chosenPlaceLookup: ChosenPlaceLookup
) : ObserveFollowablePlacesTask {

    override fun invoke(): Flow<Either<Failure, FollowablePlaces>> =
        combine(
            flow = locationRepository.observeLocationPreference(),
            flow2 = favoriteRepository.observeFavorites()
        ) { locationPreference: Either<Failure, LocationPreference>, favorites: Either<Failure, List<Favorite>> ->
            locationPreference.flatMap { preference: LocationPreference ->
                favorites.flatMap { favoriteList: List<Favorite> ->
                    followablePlaces(
                        locationPreference = preference,
                        favorites = favoriteList
                    )
                }
            }
        }.distinctUntilChanged()

    // A chosen place that cannot be found is left unnamed, since the favorites can still be followed without it.
    private suspend fun followablePlaces(
        locationPreference: LocationPreference,
        favorites: List<Favorite>
    ): Either<Failure, FollowablePlaces> {
        val chosenPlace: FollowedPlace? = chosenPlaceLookup
            .chosenPlace(
                locationPreference = locationPreference,
                favorites = favorites
            ).onResult(
                onError = { _: Failure -> null },
                onSuccess = { followedPlace: FollowedPlace -> followedPlace }
            )
        return FollowablePlaces(
            chosenPlace = chosenPlace,
            favorites = favorites.map { favorite: Favorite ->
                FollowableFavorite(
                    favoriteId = favorite.favoriteId,
                    name = favorite.locality,
                    isFollowed = favorite.favoriteId in locationPreference.followedFavoriteIds,
                    isChosen = favorite.favoriteId == chosenPlace?.favoriteId,
                    isInSweden = geohashOf(
                        latitude = favorite.latitude,
                        longitude = favorite.longitude
                    )?.isInSwedishWaters() == true
                )
            }
        ).asRight()
    }
}