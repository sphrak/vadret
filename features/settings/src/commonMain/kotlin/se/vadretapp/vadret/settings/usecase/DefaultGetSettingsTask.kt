package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.flatMap
import io.github.sphrak.either.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.forecastpreference.ForecastPreferenceRepository
import se.vadretapp.vadret.forecastpreference.model.ForecastPreferences
import se.vadretapp.vadret.pollenpreference.PollenPreferenceRepository
import se.vadretapp.vadret.pollenpreference.model.PollenPreferences
import se.vadretapp.vadret.setting.SettingRepository
import se.vadretapp.vadret.setting.model.AppLanguage
import se.vadretapp.vadret.setting.model.HourFormatChoice
import se.vadretapp.vadret.setting.model.Theme
import se.vadretapp.vadret.settings.Settings

internal class DefaultGetSettingsTask(
    private val settingRepository: SettingRepository,
    private val forecastPreferenceRepository: ForecastPreferenceRepository,
    private val pollenPreferenceRepository: PollenPreferenceRepository
) : GetSettingsTask {

    override suspend fun invoke(): Either<Failure, Settings> =
        settingRepository
            .getTheme()
            .flatMap { theme: Theme ->
                settingRepository
                    .getLanguage()
                    .flatMap { language: AppLanguage ->
                        settingRepository
                            .getHourFormatChoice()
                            .flatMap { hourFormatChoice: HourFormatChoice ->
                                forecastPreferenceRepository
                                    .getForecastPreferences()
                                    .flatMap { forecastPreferences: ForecastPreferences ->
                                        pollenPreferenceRepository
                                            .getPollenPreferences()
                                            .map { pollenPreferences: PollenPreferences ->
                                                Settings(
                                                    theme = theme,
                                                    language = language,
                                                    hourFormatChoice = hourFormatChoice,
                                                    isHourlyForecastEnabled = forecastPreferences.isHourlyForecastEnabled,
                                                    isExpandOnceEnabled = forecastPreferences.isExpandOnceEnabled,
                                                    isPollenEnabled = pollenPreferences.isPollenEnabled,
                                                    allergenPollenIds = pollenPreferences.allergenPollenIds,
                                                    isEndOfSeasonSimulated = pollenPreferences.isEndOfSeasonSimulated
                                                )
                                            }
                                    }
                            }
                    }
            }
}