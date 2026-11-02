package se.vadretapp.vadret.main.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals

class TabNavigationTest {

    private val detailKey = WarningDetailKey(warningIdentifier = "smhi-1-1")
    private val storyKey = WarningStoryKey(warningIdentifiers = listOf("smhi-1-1"))
    private val placeForecastKey = PlaceForecastKey(
        latitude = 61.8286,
        longitude = 16.0912
    )

    private val tabNavigation = TabNavigation(
        tabHistory = NavBackStack(WeatherKey),
        tabBackStacks = listOf(WeatherKey, WarningKey, RadarKey, SettingsKey).associateWith { tabKey: NavKey -> NavBackStack(tabKey) }
    )

    private fun tabBackStack(tabKey: NavKey): List<NavKey> = tabNavigation.tabBackStacks.getValue(key = tabKey).toList()

    @Test
    fun `a tab keeps what it showed while another tab is open`() {
        tabNavigation.selectTab(tabKey = WarningKey)
        tabNavigation.open(key = detailKey)
        tabNavigation.selectTab(tabKey = RadarKey)
        tabNavigation.selectTab(tabKey = WarningKey)

        assertEquals(expected = listOf(WeatherKey, WarningKey), actual = tabNavigation.tabHistory.toList())
        assertEquals(expected = listOf(WarningKey, detailKey), actual = tabBackStack(tabKey = WarningKey))
    }

    @Test
    fun `selecting the open tab again goes back to its first screen`() {
        tabNavigation.selectTab(tabKey = WarningKey)
        tabNavigation.open(key = detailKey)
        tabNavigation.selectTab(tabKey = WarningKey)

        assertEquals(expected = listOf(WarningKey), actual = tabBackStack(tabKey = WarningKey))
    }

    @Test
    fun `back leaves a screen inside the tab before it leaves the tab for the weather`() {
        tabNavigation.selectTab(tabKey = WarningKey)
        tabNavigation.open(key = detailKey)

        tabNavigation.goBack()
        assertEquals(expected = listOf(WarningKey), actual = tabBackStack(tabKey = WarningKey))
        assertEquals(expected = WarningKey, actual = tabNavigation.currentTabKey)

        tabNavigation.goBack()
        assertEquals(expected = WeatherKey, actual = tabNavigation.currentTabKey)

        tabNavigation.goBack()
        assertEquals(expected = listOf(WeatherKey), actual = tabNavigation.tabHistory.toList())
    }

    @Test
    fun `selecting the weather tab closes the other tab without forgetting it`() {
        tabNavigation.selectTab(tabKey = SettingsKey)
        tabNavigation.selectTab(tabKey = RadarKey)
        tabNavigation.selectTab(tabKey = WeatherKey)

        assertEquals(expected = listOf(WeatherKey), actual = tabNavigation.tabHistory.toList())
        assertEquals(expected = listOf(RadarKey), actual = tabBackStack(tabKey = RadarKey))
    }

    @Test
    fun `closing removes only the screen on top of the open tab`() {
        tabNavigation.open(key = storyKey)
        tabNavigation.close(key = detailKey)
        assertEquals(expected = listOf(WeatherKey, storyKey), actual = tabBackStack(tabKey = WeatherKey))

        tabNavigation.close(key = storyKey)
        tabNavigation.close(key = WeatherKey)
        assertEquals(expected = listOf(WeatherKey), actual = tabBackStack(tabKey = WeatherKey))
    }

    @Test
    fun `a place forecast opens in the weather tab on top of its first screen`() {
        tabNavigation.open(key = storyKey)
        tabNavigation.selectTab(tabKey = RadarKey)

        tabNavigation.openPlaceForecast(placeForecastKey = placeForecastKey)

        assertEquals(expected = WeatherKey, actual = tabNavigation.currentTabKey)
        assertEquals(expected = listOf(WeatherKey, placeForecastKey), actual = tabBackStack(tabKey = WeatherKey))
    }

    @Test
    fun `back from a place forecast returns to the map`() {
        tabNavigation.selectTab(tabKey = RadarKey)
        tabNavigation.openPlaceForecast(placeForecastKey = placeForecastKey)

        tabNavigation.goBack()

        assertEquals(expected = listOf(WeatherKey, RadarKey), actual = tabNavigation.tabHistory.toList())
        assertEquals(expected = listOf(WeatherKey), actual = tabBackStack(tabKey = WeatherKey))
    }

    @Test
    fun `closing a place forecast returns to the map`() {
        tabNavigation.selectTab(tabKey = RadarKey)
        tabNavigation.openPlaceForecast(placeForecastKey = placeForecastKey)

        tabNavigation.closePlaceForecast(placeForecastKey = placeForecastKey)

        assertEquals(expected = RadarKey, actual = tabNavigation.currentTabKey)
        assertEquals(expected = listOf(WeatherKey), actual = tabBackStack(tabKey = WeatherKey))
    }

    @Test
    fun `a place saved from the map shows the weather tab from its first screen`() {
        tabNavigation.openPlaceForecast(placeForecastKey = placeForecastKey)
        tabNavigation.selectTab(tabKey = RadarKey)

        tabNavigation.openWeatherTab()

        assertEquals(expected = WeatherKey, actual = tabNavigation.currentTabKey)
        assertEquals(expected = listOf(WeatherKey), actual = tabBackStack(tabKey = WeatherKey))
    }
}