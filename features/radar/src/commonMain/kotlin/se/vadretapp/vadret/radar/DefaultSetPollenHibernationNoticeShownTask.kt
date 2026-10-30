package se.vadretapp.vadret.radar

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pollenpreference.PollenPreferenceRepository

internal class DefaultSetPollenHibernationNoticeShownTask(
    private val pollenPreferenceRepository: PollenPreferenceRepository
) : SetPollenHibernationNoticeShownTask {

    override suspend fun invoke(isShown: Boolean): Either<Failure, Unit> = pollenPreferenceRepository.setHibernationNoticeShown(isShown = isShown)
}