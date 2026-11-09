package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.setting.SettingRepository

internal class DefaultSetNativeNavigationTask(
    private val settingRepository: SettingRepository
) : SetNativeNavigationTask {

    override suspend fun invoke(isNativeNavigationEnabled: Boolean): Either<Failure, Unit> = settingRepository.setNativeNavigationEnabled(isEnabled = isNativeNavigationEnabled)
}