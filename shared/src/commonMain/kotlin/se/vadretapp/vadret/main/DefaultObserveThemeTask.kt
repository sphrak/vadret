package se.vadretapp.vadret.main

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.setting.SettingRepository
import se.vadretapp.vadret.setting.model.Theme

internal class DefaultObserveThemeTask(
    private val settingRepository: SettingRepository
) : ObserveThemeTask {

    override fun invoke(): Flow<Either<Failure, Theme>> = settingRepository.observeTheme()
}