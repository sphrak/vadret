package se.vadretapp.vadret.main

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.setting.SettingRepository
import se.vadretapp.vadret.setting.model.AppLanguage

internal class DefaultObserveLanguageTask(
    private val settingRepository: SettingRepository
) : ObserveLanguageTask {

    override fun invoke(): Flow<Either<Failure, AppLanguage>> = settingRepository.observeLanguage()
}