package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.setting.SettingRepository
import se.vadretapp.vadret.setting.model.AppLanguage
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.setting.model.HourFormatChoice
import se.vadretapp.vadret.setting.model.Theme

// The phone is Swedish on a 24 hour clock, so a choice to follow it shows as Swedish and 24 hours.
internal class FakeSettingRepository(
    language: Either<Failure, AppLanguage> = AppLanguage.SYSTEM.asRight(),
    hourFormat: Either<Failure, HourFormat> = HourFormat.TWENTY_FOUR_HOUR.asRight()
) : SettingRepository {

    val language: MutableStateFlow<Either<Failure, AppLanguage>> = MutableStateFlow(value = language)
    val hourFormat: MutableStateFlow<Either<Failure, HourFormat>> = MutableStateFlow(value = hourFormat)

    override suspend fun getTheme(): Either<Failure, Theme> = error("Push alerts never read the theme")
    override fun observeTheme(): Flow<Either<Failure, Theme>> = error("Push alerts never read the theme")
    override suspend fun setTheme(theme: Theme): Either<Failure, Unit> = error("Push alerts never change the theme")
    override suspend fun getLanguage(): Either<Failure, AppLanguage> = language.value
    override fun observeLanguage(): Flow<Either<Failure, AppLanguage>> = language
    override suspend fun setLanguage(language: AppLanguage): Either<Failure, Unit> = error("Push alerts never change the language")
    override suspend fun getShownLanguage(): Either<Failure, AppLanguage> =
        language.value.map { chosenLanguage: AppLanguage ->
            if (chosenLanguage == AppLanguage.SYSTEM) {
                AppLanguage.SWEDISH
            } else {
                chosenLanguage
            }
        }
    override suspend fun getHourFormatChoice(): Either<Failure, HourFormatChoice> = error("Push alerts only read the clock to follow")
    override suspend fun getHourFormat(): Either<Failure, HourFormat> = hourFormat.value
    override fun observeHourFormat(): Flow<Either<Failure, HourFormat>> = hourFormat.map { value: Either<Failure, HourFormat> -> value }
    override suspend fun setHourFormatChoice(hourFormatChoice: HourFormatChoice): Either<Failure, Unit> = error("Push alerts never change the clock")
}