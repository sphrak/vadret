package se.vadretapp.vadret.radar

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.radar.model.Radar

internal interface GetRadarTask {
    suspend operator fun invoke(imageUrl: String?): Either<Failure, Radar>
}