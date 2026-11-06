package se.vadretapp.vadret.radar.usecase

import io.github.sphrak.either.Either
import kotlinx.datetime.LocalDate
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.radar.model.PollenMap

internal interface GetPollenMapTask {
    suspend operator fun invoke(date: LocalDate): Either<Failure, PollenMap>
}