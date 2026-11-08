package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.setting.model.HourFormatChoice

internal interface SetHourFormatChoiceTask {
    suspend operator fun invoke(hourFormatChoice: HourFormatChoice): Either<Failure, Unit>
}