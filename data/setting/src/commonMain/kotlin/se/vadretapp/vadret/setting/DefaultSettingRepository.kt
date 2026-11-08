package se.vadretapp.vadret.setting

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.catch
import se.vadretapp.vadret.common.either
import se.vadretapp.vadret.setting.extension.toAppLanguage
import se.vadretapp.vadret.setting.extension.toFailure
import se.vadretapp.vadret.setting.extension.toHourFormatChoice
import se.vadretapp.vadret.setting.extension.toPreferenceValue
import se.vadretapp.vadret.setting.extension.toSystemSettings
import se.vadretapp.vadret.setting.extension.toTheme
import se.vadretapp.vadret.setting.local.SettingLocalDataSource
import se.vadretapp.vadret.setting.local.SystemSettingLocalDataSource
import se.vadretapp.vadret.setting.model.AppLanguage
import se.vadretapp.vadret.setting.model.HourFormatChoice
import se.vadretapp.vadret.setting.model.SystemSettings
import se.vadretapp.vadret.setting.model.SystemSettingsEntity
import se.vadretapp.vadret.setting.model.Theme

internal class DefaultSettingRepository(
    private val settingLocalDataSource: SettingLocalDataSource,
    private val systemSettingLocalDataSource: SystemSettingLocalDataSource
) : SettingRepository {

    override suspend fun getTheme(): Either<Failure, Theme> =
        either { settingLocalDataSource.getTheme().toTheme() }
            .catch { exception: Exception -> exception.toFailure() }

    override fun observeTheme(): Flow<Either<Failure, Theme>> =
        settingLocalDataSource
            .observeTheme()
            .toSetting { value: String? -> value.toTheme() }

    override suspend fun setTheme(theme: Theme): Either<Failure, Unit> =
        either { settingLocalDataSource.setTheme(value = theme.toPreferenceValue()) }
            .catch { exception: Exception -> exception.toFailure() }

    override suspend fun getLanguage(): Either<Failure, AppLanguage> =
        either { settingLocalDataSource.getLanguage().toAppLanguage() }
            .catch { exception: Exception -> exception.toFailure() }

    override fun observeLanguage(): Flow<Either<Failure, AppLanguage>> =
        settingLocalDataSource
            .observeLanguage()
            .toSetting { value: String? -> value.toAppLanguage() }

    override suspend fun setLanguage(language: AppLanguage): Either<Failure, Unit> =
        either { settingLocalDataSource.setLanguage(value = language.toPreferenceValue()) }
            .catch { exception: Exception -> exception.toFailure() }

    override suspend fun getHourFormatChoice(): Either<Failure, HourFormatChoice> =
        either { settingLocalDataSource.getHourFormatChoice().toHourFormatChoice() }
            .catch { exception: Exception -> exception.toFailure() }

    override fun observeHourFormatChoice(): Flow<Either<Failure, HourFormatChoice>> =
        settingLocalDataSource
            .observeHourFormatChoice()
            .toSetting { value: String? -> value.toHourFormatChoice() }

    override suspend fun setHourFormatChoice(hourFormatChoice: HourFormatChoice): Either<Failure, Unit> =
        either { settingLocalDataSource.setHourFormatChoice(value = hourFormatChoice.toPreferenceValue()) }
            .catch { exception: Exception -> exception.toFailure() }

    override fun getSystemSettings(): Either<Failure, SystemSettings> =
        either { systemSettingLocalDataSource.getSystemSettings().toSystemSettings() }
            .catch { exception: Exception -> exception.toFailure() }

    override fun observeSystemSettings(): Flow<Either<Failure, SystemSettings>> =
        systemSettingLocalDataSource
            .observeSystemSettings()
            .map<SystemSettingsEntity, Either<Failure, SystemSettings>> { systemSettingsEntity: SystemSettingsEntity ->
                systemSettingsEntity.toSystemSettings().asRight()
            }.catchToFailure()

    private fun <T> Flow<String?>.toSetting(parse: (String?) -> T): Flow<Either<Failure, T>> =
        map<String?, Either<Failure, T>> { value: String? -> parse(value).asRight() }.catchToFailure()

    private fun <T> Flow<Either<Failure, T>>.catchToFailure(): Flow<Either<Failure, T>> =
        catch { throwable: Throwable ->
            if (throwable is Exception) {
                emit(throwable.toFailure().asLeft())
            } else {
                throw throwable
            }
        }
}