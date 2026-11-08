package se.vadretapp.vadret.main.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.ic_partly_cloudy_day
import se.vadretapp.vadret.ui.resources.ic_partly_cloudy_day_filled
import se.vadretapp.vadret.ui.resources.ic_radar
import se.vadretapp.vadret.ui.resources.ic_radar_filled
import se.vadretapp.vadret.ui.resources.ic_settings
import se.vadretapp.vadret.ui.resources.ic_settings_filled
import se.vadretapp.vadret.ui.resources.ic_warning
import se.vadretapp.vadret.ui.resources.ic_warning_filled
import se.vadretapp.vadret.ui.resources.radar
import se.vadretapp.vadret.ui.resources.settings
import se.vadretapp.vadret.ui.resources.warnings
import se.vadretapp.vadret.ui.resources.weather

@Serializable
internal data object WeatherKey : NavKey

@Serializable
internal data object PlaceSearchKey : NavKey

@Serializable
internal data object WarningKey : NavKey

@Serializable
internal data class WarningDetailKey(val warningIdentifier: String) : NavKey

@Serializable
internal data object WarningPlacesKey : NavKey

@Serializable
internal data class WarningStoryKey(val warningIdentifiers: List<String>) : NavKey

@Serializable
internal data class SimulatedWarningStoryKey(val warningIdentifiers: List<String>) : NavKey

@Serializable
internal data object RadarKey : NavKey

@Serializable
internal data class PlaceForecastKey(
    val latitude: Double,
    val longitude: Double
) : NavKey

@Serializable
internal data object SettingsKey : NavKey

@Serializable
internal data object SettingsPlacesKey : NavKey

@Serializable
internal data class SettingsSectionKey(val sectionKey: String) : NavKey

@Serializable
internal data object SettingsPollenKey : NavKey

@Serializable
internal data object SettingsLabsKey : NavKey

@Serializable
internal data object SettingsAboutKey : NavKey

@Serializable
internal data object SettingsLicensesKey : NavKey

@Serializable
internal data object SettingsDeveloperKey : NavKey

@Serializable
internal data class SettingsDeveloperSectionKey(val sectionKey: String) : NavKey

internal enum class TopLevelDestination(
    val key: NavKey,
    val iconResource: DrawableResource,
    val selectedIconResource: DrawableResource,
    val labelResource: StringResource
) {
    WEATHER(
        key = WeatherKey,
        iconResource = Res.drawable.ic_partly_cloudy_day,
        selectedIconResource = Res.drawable.ic_partly_cloudy_day_filled,
        labelResource = Res.string.weather
    ),
    WARNINGS(
        key = WarningKey,
        iconResource = Res.drawable.ic_warning,
        selectedIconResource = Res.drawable.ic_warning_filled,
        labelResource = Res.string.warnings
    ),
    RADAR(
        key = RadarKey,
        iconResource = Res.drawable.ic_radar,
        selectedIconResource = Res.drawable.ic_radar_filled,
        labelResource = Res.string.radar
    ),
    SETTINGS(
        key = SettingsKey,
        iconResource = Res.drawable.ic_settings,
        selectedIconResource = Res.drawable.ic_settings_filled,
        labelResource = Res.string.settings
    )
}