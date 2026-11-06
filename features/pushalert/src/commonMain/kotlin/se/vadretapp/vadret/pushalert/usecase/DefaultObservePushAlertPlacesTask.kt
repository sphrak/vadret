package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.flatMap
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.location.model.LocationPreference
import se.vadretapp.vadret.pushalert.extension.toPushAlertPlaces
import se.vadretapp.vadret.pushalert.model.PushAlertPlace

internal class DefaultObservePushAlertPlacesTask(
    private val locationRepository: LocationRepository,
    private val favoriteRepository: FavoriteRepository
) : ObservePushAlertPlacesTask {

    override fun invoke(): Flow<Either<Failure, List<PushAlertPlace>>> =
        combine(
            flow = locationRepository.observeLocationPreference(),
            flow2 = favoriteRepository.observeFavorites()
        ) { locationPreference: Either<Failure, LocationPreference>, favorites: Either<Failure, List<Favorite>> ->
            locationPreference.flatMap { preference: LocationPreference ->
                favorites.map { favoriteList: List<Favorite> -> preference.toPushAlertPlaces(favorites = favoriteList) }
            }
        }.distinctUntilChanged()
}