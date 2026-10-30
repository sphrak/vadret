package se.vadretapp.vadret.radar

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pollenpreference.model.PollenPreferences

internal interface ObservePollenPreferencesTask {
    operator fun invoke(): Flow<Either<Failure, PollenPreferences>>
}