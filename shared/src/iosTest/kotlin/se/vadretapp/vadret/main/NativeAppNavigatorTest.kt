package se.vadretapp.vadret.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import se.vadretapp.vadret.main.navigation.PlaceForecastKey
import se.vadretapp.vadret.main.navigation.SettingsAboutKey
import se.vadretapp.vadret.main.navigation.SettingsDeveloperSectionKey
import se.vadretapp.vadret.main.navigation.SettingsSectionKey
import se.vadretapp.vadret.main.navigation.WarningDetailKey
import se.vadretapp.vadret.main.navigation.WarningPlacesKey
import se.vadretapp.vadret.main.navigation.WarningStoryKey
import se.vadretapp.vadret.ui.DeveloperSettingsSection
import se.vadretapp.vadret.ui.SettingsSection
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.ic_settings
import se.vadretapp.vadret.ui.resources.labs
import se.vadretapp.vadret.ui.resources.pollen
import se.vadretapp.vadret.ui.resources.settings_about
import se.vadretapp.vadret.ui.resources.settings_places
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private const val SECTION_KEY = "notifications"
private const val DEVELOPER_SECTION_KEY = "simulation"
private const val MISSING_SECTION_KEY = "removed"
private const val NO_TITLE = ""

class NativeAppNavigatorTest {

    private val settingsSection = FakeSettingsSection(
        key = SECTION_KEY,
        titleResource = Res.string.pollen
    )

    private val developerSettingsSection = FakeDeveloperSettingsSection(
        key = DEVELOPER_SECTION_KEY,
        titleResource = Res.string.labs
    )

    @Test
    fun `a settings screen is pushed under a navigation bar showing its title`() = runTest {
        val navigatedRoute: NativeRoute = navigatedRoute { nativeAppNavigator: NativeAppNavigator ->
            nativeAppNavigator.open(key = SettingsAboutKey)
        }

        assertEquals(expected = SettingsAboutKey, actual = navigatedRoute.key)
        assertEquals(expected = NativePresentation.PUSH, actual = navigatedRoute.presentation)
        assertEquals(expected = title(titleResource = Res.string.settings_about), actual = navigatedRoute.title)
        assertTrue(actual = navigatedRoute.title.isNotEmpty())
    }

    @Test
    fun `a settings section is titled by the section it opens`() = runTest {
        val navigatedRoute: NativeRoute = navigatedRoute { nativeAppNavigator: NativeAppNavigator ->
            nativeAppNavigator.open(key = SettingsSectionKey(sectionKey = SECTION_KEY))
        }

        assertEquals(expected = title(titleResource = Res.string.pollen), actual = navigatedRoute.title)
    }

    @Test
    fun `a developer section is titled by the section it opens`() = runTest {
        val navigatedRoute: NativeRoute = navigatedRoute { nativeAppNavigator: NativeAppNavigator ->
            nativeAppNavigator.open(key = SettingsDeveloperSectionKey(sectionKey = DEVELOPER_SECTION_KEY))
        }

        assertEquals(expected = title(titleResource = Res.string.labs), actual = navigatedRoute.title)
    }

    @Test
    fun `a section the build no longer has opens without a title`() = runTest {
        val navigatedRoute: NativeRoute = navigatedRoute { nativeAppNavigator: NativeAppNavigator ->
            nativeAppNavigator.open(key = SettingsSectionKey(sectionKey = MISSING_SECTION_KEY))
        }

        assertEquals(expected = NO_TITLE, actual = navigatedRoute.title)
    }

    @Test
    fun `a place forecast is pushed without a navigation bar since it has its own header`() = runTest {
        val placeForecastKey = PlaceForecastKey(
            latitude = 59.33,
            longitude = 18.07
        )

        val navigatedRoute: NativeRoute = navigatedRoute { nativeAppNavigator: NativeAppNavigator ->
            nativeAppNavigator.openPlaceForecast(placeForecastKey = placeForecastKey)
        }

        assertEquals(expected = placeForecastKey, actual = navigatedRoute.key)
        assertEquals(expected = NativePresentation.PUSH_WITHOUT_NAVIGATION_BAR, actual = navigatedRoute.presentation)
        assertEquals(expected = NO_TITLE, actual = navigatedRoute.title)
    }

    @Test
    fun `a warning is pushed without a navigation bar since it has its own header`() = runTest {
        val navigatedRoute: NativeRoute = navigatedRoute { nativeAppNavigator: NativeAppNavigator ->
            nativeAppNavigator.open(key = WarningDetailKey(warningIdentifier = "smhi-1-1"))
        }

        assertEquals(expected = NativePresentation.PUSH_WITHOUT_NAVIGATION_BAR, actual = navigatedRoute.presentation)
    }

    @Test
    fun `a warning story covers the whole screen`() = runTest {
        val navigatedRoute: NativeRoute = navigatedRoute { nativeAppNavigator: NativeAppNavigator ->
            nativeAppNavigator.open(key = WarningStoryKey(warningIdentifiers = listOf("smhi-1-1")))
        }

        assertEquals(expected = NativePresentation.FULL_SCREEN_COVER, actual = navigatedRoute.presentation)
        assertEquals(expected = NO_TITLE, actual = navigatedRoute.title)
    }

    @Test
    fun `the followed places open as a titled sheet`() = runTest {
        val navigatedRoute: NativeRoute = navigatedRoute { nativeAppNavigator: NativeAppNavigator ->
            nativeAppNavigator.open(key = WarningPlacesKey)
        }

        assertEquals(expected = NativePresentation.SHEET, actual = navigatedRoute.presentation)
        assertEquals(expected = title(titleResource = Res.string.settings_places), actual = navigatedRoute.title)
    }

    @Test
    fun `closing a screen goes back`() = runTest {
        var goBackCount = 0
        val nativeAppNavigator: NativeAppNavigator = nativeAppNavigator(
            coroutineScope = this,
            onGoBack = { goBackCount += 1 }
        )

        nativeAppNavigator.close(key = SettingsAboutKey)

        assertEquals(expected = 1, actual = goBackCount)
    }

    @Test
    fun `closing a place forecast goes back to the map beneath it`() = runTest {
        var goBackCount = 0
        val nativeAppNavigator: NativeAppNavigator = nativeAppNavigator(
            coroutineScope = this,
            onGoBack = { goBackCount += 1 }
        )

        nativeAppNavigator.closePlaceForecast(
            placeForecastKey = PlaceForecastKey(
                latitude = 59.33,
                longitude = 18.07
            )
        )

        assertEquals(expected = 1, actual = goBackCount)
    }

    @Test
    fun `opening the chosen location selects the weather tab`() = runTest {
        val selectedTabs: MutableList<NativeTab> = mutableListOf()
        val nativeAppNavigator: NativeAppNavigator = nativeAppNavigator(
            coroutineScope = this,
            onSelectTab = { tab: NativeTab -> selectedTabs += tab }
        )

        nativeAppNavigator.openWeatherTab()

        assertEquals(expected = listOf(NativeTab.WEATHER), actual = selectedTabs)
    }

    @Test
    fun `routes to the same screen are equal whatever their title`() {
        val route = NativeRoute(
            key = SettingsAboutKey,
            title = "About",
            presentation = NativePresentation.PUSH
        )
        val sameScreen = NativeRoute(
            key = SettingsAboutKey,
            title = "Om",
            presentation = NativePresentation.PUSH
        )

        assertEquals(expected = route, actual = sameScreen)
        assertEquals(expected = route.hashCode(), actual = sameScreen.hashCode())
    }

    @Test
    fun `routes to different screens are not equal`() {
        val route = NativeRoute(
            key = SettingsAboutKey,
            title = NO_TITLE,
            presentation = NativePresentation.PUSH
        )
        val otherScreen = NativeRoute(
            key = WarningPlacesKey,
            title = NO_TITLE,
            presentation = NativePresentation.SHEET
        )

        assertNotEquals(illegal = route, actual = otherScreen)
    }

    private suspend fun CoroutineScope.navigatedRoute(navigate: (NativeAppNavigator) -> Unit): NativeRoute {
        val navigatedRoute: CompletableDeferred<NativeRoute> = CompletableDeferred()
        val nativeAppNavigator: NativeAppNavigator = nativeAppNavigator(
            coroutineScope = this,
            onNavigate = { route: NativeRoute -> navigatedRoute.complete(value = route) }
        )
        navigate(nativeAppNavigator)
        return navigatedRoute.await()
    }

    private fun nativeAppNavigator(
        coroutineScope: CoroutineScope,
        onNavigate: (NativeRoute) -> Unit = {},
        onGoBack: () -> Unit = {},
        onSelectTab: (NativeTab) -> Unit = {}
    ): NativeAppNavigator =
        NativeAppNavigator(
            resourceEnvironment = getSystemResourceEnvironment(),
            coroutineScope = coroutineScope,
            settingsSections = listOf(settingsSection),
            developerSettingsSections = listOf(developerSettingsSection),
            onNavigate = onNavigate,
            onGoBack = onGoBack,
            onSelectTab = onSelectTab
        )

    private suspend fun title(titleResource: StringResource): String =
        getString(
            environment = getSystemResourceEnvironment(),
            resource = titleResource
        )
}

private class FakeSettingsSection(
    override val key: String,
    override val titleResource: StringResource
) : SettingsSection {

    override val iconResource: DrawableResource = Res.drawable.ic_settings

    @Composable
    override fun Content(
        onBack: () -> Unit,
        modifier: Modifier
    ) {
    }
}

private class FakeDeveloperSettingsSection(
    override val key: String,
    override val titleResource: StringResource
) : DeveloperSettingsSection {

    @Composable
    override fun Content(
        onBack: () -> Unit,
        modifier: Modifier
    ) {
    }
}