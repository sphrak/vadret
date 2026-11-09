package se.vadretapp.vadret.main

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import se.vadretapp.vadret.main.navigation.AppNavigator
import se.vadretapp.vadret.main.navigation.PlaceForecastKey
import se.vadretapp.vadret.main.navigation.VadretKey
import se.vadretapp.vadret.main.navigation.screenKind
import se.vadretapp.vadret.ui.DeveloperSettingsSection
import se.vadretapp.vadret.ui.SettingsSection

private const val NO_TITLE = ""

internal class NativeAppNavigator(
    private val resourceEnvironment: ResourceEnvironment,
    private val coroutineScope: CoroutineScope,
    private val settingsSections: List<SettingsSection>,
    developerSettingsSections: List<DeveloperSettingsSection>,
    private val onNavigate: (NativeRoute) -> Unit,
    private val onGoBack: () -> Unit,
    private val onSelectTab: (NativeTab) -> Unit
) : AppNavigator {

    val developerSettingsSections: List<DeveloperSettingsSection> = developerSettingsSections.withWarningStorySimulation(appNavigator = this)

    override fun open(key: VadretKey) {
        coroutineScope.launch {
            onNavigate(
                NativeRoute(
                    key = key,
                    title = navigationTitle(key = key),
                    presentation = key.screenKind.toNativePresentation()
                )
            )
        }
    }

    override fun close(key: VadretKey) {
        onGoBack()
    }

    override fun openPlaceForecast(placeForecastKey: PlaceForecastKey) {
        open(key = placeForecastKey)
    }

    override fun closePlaceForecast(placeForecastKey: PlaceForecastKey) {
        onGoBack()
    }

    override fun openWeatherTab() {
        onSelectTab(NativeTab.WEATHER)
    }

    private suspend fun navigationTitle(key: VadretKey): String {
        val titleResource: StringResource? = key.navigationTitleResource(
            settingsSections = settingsSections,
            developerSettingsSections = developerSettingsSections
        )
        return if (titleResource != null) {
            getString(
                environment = resourceEnvironment,
                resource = titleResource
            )
        } else {
            NO_TITLE
        }
    }
}