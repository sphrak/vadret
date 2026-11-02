package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure

internal interface AdoptPushAlertFavoritesTask {
    suspend operator fun invoke(): Either<Failure, Unit>
}