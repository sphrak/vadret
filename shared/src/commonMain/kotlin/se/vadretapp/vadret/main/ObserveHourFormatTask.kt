package se.vadretapp.vadret.main

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.setting.model.HourFormat

internal interface ObserveHourFormatTask {

    operator fun invoke(): Flow<Either<Failure, HourFormat>>
}