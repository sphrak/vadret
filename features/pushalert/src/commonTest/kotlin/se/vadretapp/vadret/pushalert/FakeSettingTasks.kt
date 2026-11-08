package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.setting.model.Language
import se.vadretapp.vadret.setting.usecase.GetHourFormatTask
import se.vadretapp.vadret.setting.usecase.GetShownLanguageTask
import se.vadretapp.vadret.setting.usecase.ObserveHourFormatTask
import se.vadretapp.vadret.setting.usecase.ObserveShownLanguageTask

internal class FakeGetHourFormatTask(
    private val result: Either<Failure, HourFormat> = HourFormat.TWENTY_FOUR_HOUR.asRight()
) : GetHourFormatTask {
    override suspend fun invoke(): Either<Failure, HourFormat> = result
}

internal class FakeGetShownLanguageTask(
    private val result: Either<Failure, Language> = Language.SWEDISH.asRight()
) : GetShownLanguageTask {
    override suspend fun invoke(): Either<Failure, Language> = result
}

// A shared flow passes on a value equal to the last one, as the setting store does on an unrelated write.
internal class FakeObserveHourFormatTask : ObserveHourFormatTask {
    val hourFormats: MutableSharedFlow<Either<Failure, HourFormat>> = MutableSharedFlow(replay = 1)

    init {
        hourFormats.tryEmit(value = HourFormat.TWENTY_FOUR_HOUR.asRight())
    }

    override fun invoke(): Flow<Either<Failure, HourFormat>> = hourFormats
}

internal class FakeObserveShownLanguageTask : ObserveShownLanguageTask {
    val languages: MutableSharedFlow<Either<Failure, Language>> = MutableSharedFlow(replay = 1)

    init {
        languages.tryEmit(value = Language.SWEDISH.asRight())
    }

    override fun invoke(): Flow<Either<Failure, Language>> = languages
}