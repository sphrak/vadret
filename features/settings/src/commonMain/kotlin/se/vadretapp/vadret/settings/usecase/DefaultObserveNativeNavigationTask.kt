package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.setting.SettingRepository

internal class DefaultObserveNativeNavigationTask(
    private val settingRepository: SettingRepository
) : ObserveNativeNavigationTask {

    override fun invoke(): Flow<Either<Failure, Boolean>> = settingRepository.observeNativeNavigationEnabled()
}