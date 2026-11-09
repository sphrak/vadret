package se.vadretapp.vadret.setting.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.setting.SettingRepository
import se.vadretapp.vadret.setting.model.AppLanguage
import se.vadretapp.vadret.setting.model.HourFormatChoice
import se.vadretapp.vadret.setting.model.SystemSettings
import se.vadretapp.vadret.setting.model.Theme

internal class FakeSettingRepository(
    language: Either<Failure, AppLanguage> = AppLanguage.SYSTEM.asRight(),
    hourFormatChoice: Either<Failure, HourFormatChoice> = HourFormatChoice.SYSTEM.asRight(),
    systemSettings: Either<Failure, SystemSettings> = SystemSettings(
        isTwentyFourHour = true,
        languageTag = "sv-SE"
    ).asRight()
) : SettingRepository {

    val language: MutableStateFlow<Either<Failure, AppLanguage>> = MutableStateFlow(value = language)
    val hourFormatChoice: MutableStateFlow<Either<Failure, HourFormatChoice>> = MutableStateFlow(value = hourFormatChoice)
    val systemSettings: MutableStateFlow<Either<Failure, SystemSettings>> = MutableStateFlow(value = systemSettings)

    override suspend fun getTheme(): Either<Failure, Theme> = error("The tasks never read the theme")
    override fun observeTheme(): Flow<Either<Failure, Theme>> = error("The tasks never read the theme")
    override suspend fun setTheme(theme: Theme): Either<Failure, Unit> = error("The tasks never change the theme")
    override suspend fun getLanguage(): Either<Failure, AppLanguage> = language.value
    override fun observeLanguage(): Flow<Either<Failure, AppLanguage>> = language
    override suspend fun setLanguage(language: AppLanguage): Either<Failure, Unit> = error("The tasks never change the language")
    override suspend fun getHourFormatChoice(): Either<Failure, HourFormatChoice> = hourFormatChoice.value
    override fun observeHourFormatChoice(): Flow<Either<Failure, HourFormatChoice>> = hourFormatChoice
    override suspend fun setHourFormatChoice(hourFormatChoice: HourFormatChoice): Either<Failure, Unit> = error("The tasks never change the clock")
    override suspend fun isNativeNavigationEnabled(): Either<Failure, Boolean> = error("The tasks never read the navigation")
    override fun observeNativeNavigationEnabled(): Flow<Either<Failure, Boolean>> = error("The tasks never read the navigation")
    override suspend fun setNativeNavigationEnabled(isEnabled: Boolean): Either<Failure, Unit> = error("The tasks never change the navigation")
    override fun getSystemSettings(): Either<Failure, SystemSettings> = systemSettings.value
    override fun observeSystemSettings(): Flow<Either<Failure, SystemSettings>> = systemSettings
}