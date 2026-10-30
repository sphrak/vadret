package se.vadretapp.vadret.settings

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.flatMap
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pollenpreference.PollenPreferenceRepository

internal class DefaultSetEndOfSeasonSimulatedTask(
    private val pollenPreferenceRepository: PollenPreferenceRepository
) : SetEndOfSeasonSimulatedTask {

    override suspend fun invoke(isEndOfSeasonSimulated: Boolean): Either<Failure, Unit> =
        pollenPreferenceRepository
            .setEndOfSeasonSimulated(isSimulated = isEndOfSeasonSimulated)
            .flatMap { _: Unit ->
                if (isEndOfSeasonSimulated) {
                    pollenPreferenceRepository.setHibernationNoticeShown(isShown = false)
                } else {
                    Unit.asRight()
                }
            }
}