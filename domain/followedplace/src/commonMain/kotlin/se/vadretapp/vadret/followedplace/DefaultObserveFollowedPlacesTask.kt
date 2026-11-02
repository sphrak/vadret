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
import se.vadretapp.vadret.followedplace.extension.toFollowedPlace
import se.vadretapp.vadret.followedplace.model.FollowedPlace
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.location.model.LocationPreference
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory

internal class DefaultObserveFollowedPlacesTask(
    private val locationRepository: LocationRepository,
    private val favoriteRepository: FavoriteRepository,
    private val chosenPlaceLookup: ChosenPlaceLookup
) : ObserveFollowedPlacesTask {

    override fun invoke(): Flow<Either<Failure, List<FollowedPlace>>> =
        combine(
            flow = locationRepository.observeLocationPreference(),
            flow2 = favoriteRepository.observeFavorites()
        ) { locationPreference: Either<Failure, LocationPreference>, favorites: Either<Failure, List<Favorite>> ->
            locationPreference.flatMap { preference: LocationPreference ->
                favorites.flatMap { favoriteList: List<Favorite> ->
                    followedPlaces(
                        locationPreference = preference,
                        favorites = favoriteList
                    )
                }
            }
        }.distinctUntilChanged()

    // A chosen place that cannot be found leaves the followed favorites, which are known without it.
    private suspend fun followedPlaces(
        locationPreference: LocationPreference,
        favorites: List<Favorite>
    ): Either<Failure, List<FollowedPlace>> {
        val chosenPlace: FollowedPlace? = chosenPlaceLookup
            .chosenPlace(
                locationPreference = locationPreference,
                favorites = favorites
            ).onResult(
                onError = { failure: Failure ->
                    logger.warn { "${failure.errorCode}: the chosen place is left out of the followed places" }
                    null
                },
                onSuccess = { followedPlace: FollowedPlace -> followedPlace }
            )
        val followedFavorites: List<FollowedPlace> = favorites
            .filter { favorite: Favorite ->
                favorite.favoriteId in locationPreference.followedFavoriteIds && favorite.favoriteId != chosenPlace?.favoriteId
            }.map { favorite: Favorite -> favorite.toFollowedPlace() }
        return (listOfNotNull(chosenPlace) + followedFavorites)
            .take(n = MAXIMUM_FOLLOWED_PLACES)
            .asRight()
    }
}

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.followedplace")