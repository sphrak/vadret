package se.vadretapp.vadret.settings

import io.github.sphrak.either.Either
import io.github.sphrak.either.flatMap
import io.github.sphrak.either.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.forecastpreference.ForecastPreferenceRepository
import se.vadretapp.vadret.forecastpreference.model.ForecastPreferences
import se.vadretapp.vadret.language.LanguageRepository
import se.vadretapp.vadret.language.model.AppLanguage
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.location.model.StoredLocation
import se.vadretapp.vadret.theme.ThemeRepository
import se.vadretapp.vadret.theme.model.Theme

internal class DefaultGetSettingsTask(
    private val locationRepository: LocationRepository,
    private val themeRepository: ThemeRepository,
    private val languageRepository: LanguageRepository,
    private val forecastPreferenceRepository: ForecastPreferenceRepository
) : GetSettingsTask {

    override suspend fun invoke(): Either<Failure, Settings> =
        locationRepository
            .isAutomatic()
            .flatMap { isAutomaticLocation: Boolean ->
                themeRepository
                    .getTheme()
                    .flatMap { theme: Theme ->
                        languageRepository
                            .getLanguage()
                            .flatMap { language: AppLanguage ->
                                forecastPreferenceRepository
                                    .getForecastPreferences()
                                    .flatMap { forecastPreferences: ForecastPreferences ->
                                        locationRepository
                                            .getStoredLocation()
                                            .map { storedLocation: StoredLocation ->
                                                Settings(
                                                    isAutomaticLocation = isAutomaticLocation,
                                                    theme = theme,
                                                    language = language,
                                                    isHourlyForecastEnabled = forecastPreferences.isHourlyForecastEnabled,
                                                    isExpandOnceEnabled = forecastPreferences.isExpandOnceEnabled,
                                                    storedLocation = storedLocation
                                                )
                                            }
                                    }
                            }
                    }
            }
}