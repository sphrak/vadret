package se.vadretapp.vadret.setting

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.setting.local.DefaultSettingLocalDataSource
import se.vadretapp.vadret.setting.local.SystemSettingLocalDataSource
import se.vadretapp.vadret.setting.model.AppLanguage
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.setting.model.HourFormatChoice
import se.vadretapp.vadret.setting.model.Theme

interface SettingRepository {
    suspend fun getTheme(): Either<Failure, Theme>
    fun observeTheme(): Flow<Either<Failure, Theme>>
    suspend fun setTheme(theme: Theme): Either<Failure, Unit>
    suspend fun getLanguage(): Either<Failure, AppLanguage>
    fun observeLanguage(): Flow<Either<Failure, AppLanguage>>
    suspend fun setLanguage(language: AppLanguage): Either<Failure, Unit>
    suspend fun getShownLanguage(): Either<Failure, AppLanguage>
    suspend fun getHourFormatChoice(): Either<Failure, HourFormatChoice>
    suspend fun getHourFormat(): Either<Failure, HourFormat>
    fun observeHourFormat(): Flow<Either<Failure, HourFormat>>
    suspend fun setHourFormatChoice(hourFormatChoice: HourFormatChoice): Either<Failure, Unit>
}

internal fun SettingRepository(
    dataStore: DataStore<Preferences>,
    systemSettingLocalDataSource: SystemSettingLocalDataSource
): SettingRepository =
    DefaultSettingRepository(
        settingLocalDataSource = DefaultSettingLocalDataSource(dataStore = dataStore),
        systemSettingLocalDataSource = systemSettingLocalDataSource
    )