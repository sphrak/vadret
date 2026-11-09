package se.vadretapp.vadret.main

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import se.vadretapp.vadret.forecastwarning.ui.SimulatedWarningStoryRoute
import se.vadretapp.vadret.forecastwarning.ui.WarningDetailRoute
import se.vadretapp.vadret.forecastwarning.ui.WarningRoute
import se.vadretapp.vadret.forecastwarning.ui.WarningStoryRoute
import se.vadretapp.vadret.main.navigation.AppNavigator
import se.vadretapp.vadret.main.navigation.PlaceForecastKey
import se.vadretapp.vadret.main.navigation.PlaceSearchKey
import se.vadretapp.vadret.main.navigation.RadarKey
import se.vadretapp.vadret.main.navigation.SettingsAboutKey
import se.vadretapp.vadret.main.navigation.SettingsDeveloperKey
import se.vadretapp.vadret.main.navigation.SettingsDeveloperSectionKey
import se.vadretapp.vadret.main.navigation.SettingsKey
import se.vadretapp.vadret.main.navigation.SettingsLabsKey
import se.vadretapp.vadret.main.navigation.SettingsLicensesKey
import se.vadretapp.vadret.main.navigation.SettingsPlacesKey
import se.vadretapp.vadret.main.navigation.SettingsPollenKey
import se.vadretapp.vadret.main.navigation.SettingsSectionKey
import se.vadretapp.vadret.main.navigation.SimulatedWarningStoryKey
import se.vadretapp.vadret.main.navigation.VadretKey
import se.vadretapp.vadret.main.navigation.WarningDetailKey
import se.vadretapp.vadret.main.navigation.WarningKey
import se.vadretapp.vadret.main.navigation.WarningPlacesKey
import se.vadretapp.vadret.main.navigation.WarningStoryKey
import se.vadretapp.vadret.main.navigation.WeatherKey
import se.vadretapp.vadret.settings.ui.AboutRoute
import se.vadretapp.vadret.settings.ui.DeveloperRoute
import se.vadretapp.vadret.settings.ui.LabsRoute
import se.vadretapp.vadret.settings.ui.LicensesRoute
import se.vadretapp.vadret.settings.ui.PlacesRoute
import se.vadretapp.vadret.settings.ui.PollenSettingsRoute
import se.vadretapp.vadret.settings.ui.SettingsRoute
import se.vadretapp.vadret.ui.DeveloperSettingsSection
import se.vadretapp.vadret.ui.SettingsSection
import se.vadretapp.vadret.ui.model.NavigationIcon
import se.vadretapp.vadret.weatherforecast.ui.PlaceForecastRoute
import se.vadretapp.vadret.weatherforecast.ui.PlaceIslandRoute
import se.vadretapp.vadret.weatherforecast.ui.PlaceSearchRoute
import se.vadretapp.vadret.weatherforecast.ui.WeatherForecastRoute

@Composable
internal fun ScreenContent(
    key: VadretKey,
    appNavigator: AppNavigator,
    radarTab: RadarTab?,
    settingsSections: List<SettingsSection>,
    developerSettingsSections: List<DeveloperSettingsSection>,
    modifier: Modifier = Modifier
) {
    when (key) {
        WeatherKey -> WeatherForecastRoute(
            onWarningClick = { warningIdentifiers: List<String> ->
                appNavigator.open(key = WarningStoryKey(warningIdentifiers = warningIdentifiers))
            },
            onAddPlaceClick = { appNavigator.open(key = PlaceSearchKey) },
            modifier = modifier
        )
        PlaceSearchKey -> PlaceSearchRoute(
            onClose = { appNavigator.close(key = PlaceSearchKey) },
            modifier = modifier
        )
        is PlaceForecastKey -> PlaceForecastRoute(
            latitude = key.latitude,
            longitude = key.longitude,
            onClose = { appNavigator.closePlaceForecast(placeForecastKey = key) },
            onWarningClick = { warningIdentifiers: List<String> ->
                appNavigator.open(key = WarningStoryKey(warningIdentifiers = warningIdentifiers))
            },
            modifier = modifier
        )
        WarningKey -> WarningRoute(
            onWarningClick = { warningIdentifier: String ->
                appNavigator.open(key = WarningDetailKey(warningIdentifier = warningIdentifier))
            },
            onFollowedPlacesClick = { appNavigator.open(key = WarningPlacesKey) },
            modifier = modifier
        )
        WarningPlacesKey -> PlacesRoute(
            onBack = { appNavigator.close(key = WarningPlacesKey) },
            navigationIcon = NavigationIcon.CLOSE,
            modifier = modifier
        )
        is WarningDetailKey -> WarningDetailRoute(
            warningIdentifier = key.warningIdentifier,
            onBack = { appNavigator.close(key = key) },
            modifier = modifier
        )
        is WarningStoryKey -> WarningStoryRoute(
            warningIdentifiers = key.warningIdentifiers,
            onDismiss = { appNavigator.close(key = key) },
            modifier = modifier
        )
        is SimulatedWarningStoryKey -> SimulatedWarningStoryRoute(
            warningIdentifiers = key.warningIdentifiers,
            onDismiss = { appNavigator.close(key = key) },
            modifier = modifier
        )
        RadarKey -> RadarContent(
            appNavigator = appNavigator,
            radarTab = radarTab,
            modifier = modifier
        )
        SettingsKey -> SettingsRoute(
            onPlacesClick = { appNavigator.open(key = SettingsPlacesKey) },
            onSectionClick = { sectionKey: String -> appNavigator.open(key = SettingsSectionKey(sectionKey = sectionKey)) },
            onPollenClick = { appNavigator.open(key = SettingsPollenKey) },
            onLabsClick = { appNavigator.open(key = SettingsLabsKey) },
            onDeveloperClick = { appNavigator.open(key = SettingsDeveloperKey) },
            onAboutClick = { appNavigator.open(key = SettingsAboutKey) },
            settingsSections = settingsSections,
            modifier = modifier
        )
        SettingsPlacesKey -> PlacesRoute(
            onBack = { appNavigator.close(key = SettingsPlacesKey) },
            modifier = modifier
        )
        is SettingsSectionKey ->
            settingsSections
                .firstOrNull { settingsSection: SettingsSection -> settingsSection.key == key.sectionKey }
                ?.Content(
                    onBack = { appNavigator.close(key = key) },
                    modifier = modifier
                )
        SettingsPollenKey -> PollenSettingsRoute(
            onBack = { appNavigator.close(key = SettingsPollenKey) },
            modifier = modifier
        )
        SettingsLabsKey -> LabsRoute(
            onBack = { appNavigator.close(key = SettingsLabsKey) },
            modifier = modifier
        )
        SettingsAboutKey -> AboutRoute(
            onLicensesClick = { appNavigator.open(key = SettingsLicensesKey) },
            onBack = { appNavigator.close(key = SettingsAboutKey) },
            modifier = modifier
        )
        SettingsLicensesKey -> LicensesRoute(
            onBack = { appNavigator.close(key = SettingsLicensesKey) },
            modifier = modifier
        )
        SettingsDeveloperKey -> DeveloperRoute(
            onSectionClick = { sectionKey: String -> appNavigator.open(key = SettingsDeveloperSectionKey(sectionKey = sectionKey)) },
            onBack = { appNavigator.close(key = SettingsDeveloperKey) },
            developerSettingsSections = developerSettingsSections,
            modifier = modifier
        )
        is SettingsDeveloperSectionKey ->
            developerSettingsSections
                .firstOrNull { developerSettingsSection: DeveloperSettingsSection -> developerSettingsSection.key == key.sectionKey }
                ?.Content(
                    onBack = { appNavigator.close(key = key) },
                    modifier = modifier
                )
    }
}

@Composable
private fun RadarContent(
    appNavigator: AppNavigator,
    radarTab: RadarTab?,
    modifier: Modifier = Modifier
) {
    if (radarTab != null) {
        Box(modifier = modifier) {
            RadarPlaceContent(
                appNavigator = appNavigator,
                radarTab = radarTab
            )
        }
    }
}

@Composable
private fun RadarPlaceContent(
    appNavigator: AppNavigator,
    radarTab: RadarTab
) {
    radarTab.content { latitude: Double, longitude: Double, onDismiss: () -> Unit ->
        PlaceIslandRoute(
            latitude = latitude,
            longitude = longitude,
            onDismiss = onDismiss,
            onOpenForecast = {
                appNavigator.openPlaceForecast(
                    placeForecastKey = PlaceForecastKey(
                        latitude = latitude,
                        longitude = longitude
                    )
                )
            },
            onOpenChosenLocation = {
                onDismiss()
                appNavigator.openWeatherTab()
            }
        )
    }
}