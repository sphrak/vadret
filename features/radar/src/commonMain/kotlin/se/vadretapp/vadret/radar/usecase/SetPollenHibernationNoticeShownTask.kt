package se.vadretapp.vadret.radar.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure

internal interface SetPollenHibernationNoticeShownTask {
    suspend operator fun invoke(isShown: Boolean): Either<Failure, Unit>
}