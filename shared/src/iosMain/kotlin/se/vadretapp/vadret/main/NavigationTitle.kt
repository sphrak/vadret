package se.vadretapp.vadret.main

import org.jetbrains.compose.resources.StringResource
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
import se.vadretapp.vadret.ui.DeveloperSettingsSection
import se.vadretapp.vadret.ui.SettingsSection
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.developer_settings_title
import se.vadretapp.vadret.ui.resources.labs
import se.vadretapp.vadret.ui.resources.licenses_title
import se.vadretapp.vadret.ui.resources.pollen
import se.vadretapp.vadret.ui.resources.search
import se.vadretapp.vadret.ui.resources.settings_about
import se.vadretapp.vadret.ui.resources.settings_places

internal fun VadretKey.navigationTitleResource(
    settingsSections: List<SettingsSection>,
    developerSettingsSections: List<DeveloperSettingsSection>
): StringResource? =
    when (this) {
        WeatherKey -> null
        PlaceSearchKey -> Res.string.search
        WarningKey -> null
        RadarKey -> null
        SettingsKey -> null
        is PlaceForecastKey -> null
        is WarningDetailKey -> null
        is WarningStoryKey -> null
        is SimulatedWarningStoryKey -> null
        WarningPlacesKey -> Res.string.settings_places
        SettingsPlacesKey -> Res.string.settings_places
        is SettingsSectionKey ->
            settingsSections
                .firstOrNull { settingsSection: SettingsSection -> settingsSection.key == sectionKey }
                ?.titleResource
        SettingsPollenKey -> Res.string.pollen
        SettingsLabsKey -> Res.string.labs
        SettingsAboutKey -> Res.string.settings_about
        SettingsLicensesKey -> Res.string.licenses_title
        SettingsDeveloperKey -> Res.string.developer_settings_title
        is SettingsDeveloperSectionKey ->
            developerSettingsSections
                .firstOrNull { developerSettingsSection: DeveloperSettingsSection -> developerSettingsSection.key == sectionKey }
                ?.titleResource
    }