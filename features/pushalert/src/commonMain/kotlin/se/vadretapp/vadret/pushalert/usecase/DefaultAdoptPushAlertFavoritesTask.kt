package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.flatMap
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.pushalert.PushAlertRepository
import se.vadretapp.vadret.pushalert.model.PushAlertChoices

// Push alerts chose their favorites before favorites were followed for every feature, so those are followed from now on.
internal class DefaultAdoptPushAlertFavoritesTask(
    private val pushAlertRepository: PushAlertRepository,
    private val locationRepository: LocationRepository
) : AdoptPushAlertFavoritesTask {
    override suspend fun invoke(): Either<Failure, Unit> =
        pushAlertRepository.choices().flatMap { pushAlertChoices: PushAlertChoices ->
            locationRepository.adoptFollowedFavoriteIds(favoriteIds = pushAlertChoices.favoriteIds)
        }
}