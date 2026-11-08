package se.vadretapp.vadret.setting.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.setting.model.HourFormat

interface GetHourFormatTask {
    suspend operator fun invoke(): Either<Failure, HourFormat>
}