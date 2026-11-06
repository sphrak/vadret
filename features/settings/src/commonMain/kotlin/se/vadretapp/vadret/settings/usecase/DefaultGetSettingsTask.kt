package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.flatMap
import io.github.sphrak.either.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.forecastpreference.ForecastPreferenceRepository
import se.vadretapp.vadret.forecastpreference.model.ForecastPreferences
import se.vadretapp.vadret.language.LanguageRepository
import se.vadretapp.vadret.language.model.AppLanguage
import se.vadretapp.vadret.pollenpreference.PollenPreferenceRepository
import se.vadretapp.vadret.pollenpreference.model.PollenPreferences
import se.vadretapp.vadret.settings.Settings
import se.vadretapp.vadret.theme.ThemeRepository
import se.vadretapp.vadret.theme.model.Theme

internal class DefaultGetSettingsTask(
    private val themeRepository: ThemeRepository,
    private val languageRepository: LanguageRepository,
    private val forecastPreferenceRepository: ForecastPreferenceRepository,
    private val pollenPreferenceRepository: PollenPreferenceRepository
) : GetSettingsTask {

    override suspend fun invoke(): Either<Failure, Settings> =
        themeRepository
            .getTheme()
            .flatMap { theme: Theme ->
                languageRepository
                    .getLanguage()
                    .flatMap { language: AppLanguage ->
                        forecastPreferenceRepository
                            .getForecastPreferences()
                            .flatMap { forecastPreferences: ForecastPreferences ->
                                pollenPreferenceRepository
                                    .getPollenPreferences()
                                    .map { pollenPreferences: PollenPreferences ->
                                        Settings(
                                            theme = theme,
                                            language = language,
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