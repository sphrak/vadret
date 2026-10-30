package se.vadretapp.vadret.settings.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.display
import se.vadretapp.vadret.core.resources.enable_hourly_forecast
import se.vadretapp.vadret.core.resources.gps_is_being_used
import se.vadretapp.vadret.core.resources.gps_is_not_being_used
import se.vadretapp.vadret.core.resources.hourly_forecast_is_disabled
import se.vadretapp.vadret.core.resources.hourly_forecast_is_enabled
import se.vadretapp.vadret.core.resources.ic_chevron_right
import se.vadretapp.vadret.core.resources.pollen
import se.vadretapp.vadret.core.resources.pollen_allergens_selected
import se.vadretapp.vadret.core.resources.pollen_allergies
import se.vadretapp.vadret.core.resources.pollen_is_hidden
import se.vadretapp.vadret.core.resources.pollen_is_shown
import se.vadretapp.vadret.core.resources.pollen_no_allergies
import se.vadretapp.vadret.core.resources.select_a_language
import se.vadretapp.vadret.core.resources.select_a_theme
import se.vadretapp.vadret.core.resources.setting_current_location
import se.vadretapp.vadret.core.resources.setting_location_title
import se.vadretapp.vadret.core.resources.show_pollen
import se.vadretapp.vadret.core.resources.use_gps_by_default
import se.vadretapp.vadret.settings.Settings
import se.vadretapp.vadret.settings.SettingsEvent
import se.vadretapp.vadret.settings.SettingsState
import se.vadretapp.vadret.settings.extension.labelResource
import se.vadretapp.vadret.settings.model.LocationRow
import se.vadretapp.vadret.ui.SettingsRowDivider
import se.vadretapp.vadret.ui.SettingsSection
import se.vadretapp.vadret.ui.SettingsSectionHeader
import se.vadretapp.vadret.ui.SettingsValueRow
import se.vadretapp.vadret.ui.SwitchRow
import se.vadretapp.vadret.ui.VadretCard
import se.vadretapp.vadret.ui.VadretTheme

private const val FULL_ALPHA = 1f
private const val DISABLED_ALPHA = 0.38f
private const val LOCATION_HEADER_KEY = "location-header"
private const val GPS_KEY = "gps"
private const val DISPLAY_HEADER_KEY = "display-header"
private const val DISPLAY_KEY = "display"
private const val POLLEN_HEADER_KEY = "pollen-header"
private const val POLLEN_KEY = "pollen"
private const val CURRENT_LOCATION_HEADER_KEY = "current-location-header"
private const val CURRENT_LOCATION_KEY = "current-location"

private enum class PreferenceContentType {
    HEADER,
    CARD,
    SECTION
}

@Composable
internal fun PreferencesPage(
    state: SettingsState,
    settingsSections: List<SettingsSection>,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings: Settings = state.settings ?: return

    LazyColumn(
        contentPadding = PaddingValues(
            start = VadretTheme.spacing.large,
            end = VadretTheme.spacing.large,
            bottom = VadretTheme.spacing.large
        ),
        modifier = modifier.fillMaxSize()
    ) {
        item(key = LOCATION_HEADER_KEY, contentType = PreferenceContentType.HEADER) {
            SettingsSectionHeader(text = stringResource(resource = Res.string.setting_location_title))
        }
        item(key = GPS_KEY, contentType = PreferenceContentType.CARD) {
            VadretCard {
                SwitchRow(
                    title = stringResource(resource = Res.string.use_gps_by_default),
                    checkedSummary = stringResource(resource = Res.string.gps_is_being_used),
                    uncheckedSummary = stringResource(resource = Res.string.gps_is_not_being_used),
                    isChecked = settings.isAutomaticLocation,
                    onCheckedChange = { isChecked: Boolean ->
                        onEvent(SettingsEvent.OnLocationModeToggled(isAutomaticLocation = isChecked))
                    }
                )
            }
        }
        item(key = DISPLAY_HEADER_KEY, contentType = PreferenceContentType.HEADER) {
            SettingsSectionHeader(text = stringResource(resource = Res.string.display))
        }
        item(key = DISPLAY_KEY, contentType = PreferenceContentType.CARD) {
            VadretCard {
                SettingsValueRow(
                    title = stringResource(resource = Res.string.select_a_theme),
                    value = stringResource(resource = settings.theme.labelResource()),
                    trailingIconResource = Res.drawable.ic_chevron_right,
                    onClick = { onEvent(SettingsEvent.OnThemeRowClicked) }
                )
                SettingsRowDivider()
                SettingsValueRow(
                    title = stringResource(resource = Res.string.select_a_language),
                    value = stringResource(resource = settings.language.labelResource()),
                    trailingIconResource = Res.drawable.ic_chevron_right,
                    onClick = { onEvent(SettingsEvent.OnLanguageRowClicked) }
                )
                SettingsRowDivider()
                SwitchRow(
                    title = stringResource(resource = Res.string.enable_hourly_forecast),
                    checkedSummary = stringResource(resource = Res.string.hourly_forecast_is_enabled),
                    uncheckedSummary = stringResource(resource = Res.string.hourly_forecast_is_disabled),
                    isChecked = settings.isHourlyForecastEnabled,
                    onCheckedChange = { isChecked: Boolean ->
                        onEvent(SettingsEvent.OnHourlyForecastToggled(isHourlyForecastEnabled = isChecked))
                    }
                )
            }
        }
        item(key = POLLEN_HEADER_KEY, contentType = PreferenceContentType.HEADER) {
            SettingsSectionHeader(text = stringResource(resource = Res.string.pollen))
        }
        item(key = POLLEN_KEY, contentType = PreferenceContentType.CARD) {
            PollenCard(
                isPollenEnabled = settings.isPollenEnabled,
                pollenAllergenCount = state.pollenAllergenCount,
                onEvent = onEvent
            )
        }
        item(key = CURRENT_LOCATION_HEADER_KEY, contentType = PreferenceContentType.HEADER) {
            SettingsSectionHeader(text = stringResource(resource = Res.string.setting_current_location))
        }
        item(key = CURRENT_LOCATION_KEY, contentType = PreferenceContentType.CARD) {
            LocationCard(
                locationRows = state.locationRows,
                isAutomaticLocation = settings.isAutomaticLocation
            )
        }
        items(
            items = settingsSections,
            key = { settingsSection: SettingsSection -> settingsSection.key },
            contentType = { PreferenceContentType.SECTION }
        ) { settingsSection: SettingsSection ->
            settingsSection.Content(modifier = Modifier)
        }
    }
}

@Composable
private fun PollenCard(
    isPollenEnabled: Boolean,
    pollenAllergenCount: Int,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    VadretCard(modifier = modifier) {
        SwitchRow(
            title = stringResource(resource = Res.string.show_pollen),
            checkedSummary = stringResource(resource = Res.string.pollen_is_shown),
            uncheckedSummary = stringResource(resource = Res.string.pollen_is_hidden),
            isChecked = isPollenEnabled,
            onCheckedChange = { isChecked: Boolean ->
                onEvent(SettingsEvent.OnPollenToggled(isPollenEnabled = isChecked))
            }
        )
        if (isPollenEnabled) {
            SettingsRowDivider()
            SettingsValueRow(
                title = stringResource(resource = Res.string.pollen_allergies),
                value = pollenAllergenSummaryText(pollenAllergenCount = pollenAllergenCount),
                trailingIconResource = Res.drawable.ic_chevron_right,
                onClick = { onEvent(SettingsEvent.OnPollenAllergensRowClicked) }
            )
        }
    }
}

@Composable
private fun pollenAllergenSummaryText(pollenAllergenCount: Int): String =
    if (pollenAllergenCount == 0) {
        stringResource(resource = Res.string.pollen_no_allergies)
    } else {
        pluralStringResource(
            resource = Res.plurals.pollen_allergens_selected,
            quantity = pollenAllergenCount,
            pollenAllergenCount
        )
    }

@Composable
private fun LocationCard(
    locationRows: ImmutableList<LocationRow>,
    isAutomaticLocation: Boolean,
    modifier: Modifier = Modifier
) {
    VadretCard(modifier = modifier.alpha(alpha = locationAlpha(isAutomaticLocation = isAutomaticLocation))) {
        locationRows.forEachIndexed { index: Int, locationRow: LocationRow ->
            if (index > 0) {
                SettingsRowDivider()
            }
            SettingsValueRow(
                title = stringResource(resource = locationRow.titleResource),
                value = locationRow.value
            )
        }
    }
}

private fun locationAlpha(isAutomaticLocation: Boolean): Float =
    if (isAutomaticLocation) {
        DISABLED_ALPHA
    } else {
        FULL_ALPHA
    }