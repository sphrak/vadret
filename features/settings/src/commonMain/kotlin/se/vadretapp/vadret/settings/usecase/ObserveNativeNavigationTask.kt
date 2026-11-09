package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.setting.SettingRepository

internal interface ObserveNativeNavigationTask {
    operator fun invoke(): Flow<Either<Failure, Boolean>>
}

internal expect fun ObserveNativeNavigationTask(settingRepository: SettingRepository): ObserveNativeNavigationTask