package se.vadretapp.vadret.setting.usecase

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.setting.model.HourFormat

interface ObserveHourFormatTask {
    operator fun invoke(): Flow<Either<Failure, HourFormat>>
}