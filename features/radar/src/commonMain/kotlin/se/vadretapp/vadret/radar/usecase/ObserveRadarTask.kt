package se.vadretapp.vadret.radar.usecase

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.radar.model.Radar

internal interface ObserveRadarTask {
    operator fun invoke(imageUrl: String?): Flow<Either<Failure, Radar>>
}