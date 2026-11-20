package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.flatMap
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.pushalert.PushAlertRepository
import se.vadretapp.vadret.pushalert.model.PushAlertChoices

internal class DefaultMigrateNotifiedFavoritesTask(
    private val pushAlertRepository: PushAlertRepository,
    private val locationRepository: LocationRepository
) : MigrateNotifiedFavoritesTask {

    override suspend fun invoke(): Either<Failure, Unit> =
        pushAlertRepository.choices().flatMap { pushAlertChoices: PushAlertChoices ->
            if (pushAlertChoices.notifiedFavoriteIds.isEmpty()) {
                Unit.asRight()
            } else {
                follow(favoriteIds = pushAlertChoices.notifiedFavoriteIds).flatMap {
                    pushAlertRepository.saveChoices(pushAlertChoices = pushAlertChoices.copy(notifiedFavoriteIds = emptySet()))
                }
            }
        }

    private suspend fun follow(favoriteIds: Set<String>): Either<Failure, Unit> {
        var followed: Either<Failure, Unit> = Unit.asRight()
        for (favoriteId in favoriteIds) {
            if (followed is Either.Right) {
                followed = locationRepository.setFavoriteFollowed(
                    favoriteId = favoriteId,
                    isFollowed = true
                )
            }
        }
        return followed
    }
}