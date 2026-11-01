package se.vadretapp.vadret.main.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.ic_partly_cloudy_day
import se.vadretapp.vadret.core.resources.ic_partly_cloudy_day_filled
import se.vadretapp.vadret.core.resources.ic_radar
import se.vadretapp.vadret.core.resources.ic_radar_filled
import se.vadretapp.vadret.core.resources.ic_settings
import se.vadretapp.vadret.core.resources.ic_settings_filled
import se.vadretapp.vadret.core.resources.ic_warning
import se.vadretapp.vadret.core.resources.ic_warning_filled
import se.vadretapp.vadret.core.resources.radar
import se.vadretapp.vadret.core.resources.settings
import se.vadretapp.vadret.core.resources.warnings
import se.vadretapp.vadret.core.resources.weather

@Serializable
internal data object WeatherKey : NavKey

@Serializable
internal data object WarningKey : NavKey

@Serializable
internal data class WarningDetailKey(val warningIdentifier: String) : NavKey

@Serializable
internal data class WarningStoryKey(val warningIdentifiers: List<String>) : NavKey

@Serializable
internal data object RadarKey : NavKey

@Serializable
internal data object SettingsKey : NavKey

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