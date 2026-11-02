package se.vadretapp.vadret.main.navigation

import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

private const val ROOT_SIZE = 1

@Stable
internal class TabNavigation(
    val tabHistory: NavBackStack<NavKey>,
    val tabBackStacks: Map<NavKey, NavBackStack<NavKey>>
)

internal val TabNavigation.currentTabKey: NavKey
    get() = tabHistory.last()

internal fun TabNavigation.selectTab(tabKey: NavKey) {
    if (tabKey == currentTabKey) {
        tabBackStacks.getValue(key = tabKey).popToRoot()
        return
    }
    tabHistory.popToRoot()
    if (tabKey != tabHistory.first()) {
        tabHistory.add(element = tabKey)
    }
}

internal fun TabNavigation.open(key: NavKey) {
    tabBackStacks.getValue(key = currentTabKey).add(element = key)
}

internal fun TabNavigation.openPlaceForecast(placeForecastKey: PlaceForecastKey) {
    selectTab(tabKey = WeatherKey)
    val weatherBackStack: NavBackStack<NavKey> = tabBackStacks.getValue(key = WeatherKey)
    weatherBackStack.popToRoot()
    weatherBackStack.add(element = placeForecastKey)
}

internal fun TabNavigation.openWeatherTab() {
    selectTab(tabKey = WeatherKey)
    tabBackStacks.getValue(key = WeatherKey).popToRoot()
}

// A place forecast is only opened from the map, so leaving it returns there.
internal fun TabNavigation.closePlaceForecast(placeForecastKey: PlaceForecastKey) {
    close(key = placeForecastKey)
    selectTab(tabKey = RadarKey)
}

internal fun TabNavigation.close(key: NavKey) {
    val currentBackStack: NavBackStack<NavKey> = tabBackStacks.getValue(key = currentTabKey)
    if (currentBackStack.last() == key && currentBackStack.size > ROOT_SIZE) {
        currentBackStack.removeAt(index = currentBackStack.lastIndex)
    }
}

internal fun TabNavigation.goBack() {
    val currentBackStack: NavBackStack<NavKey> = tabBackStacks.getValue(key = currentTabKey)
    val shownKey: NavKey = currentBackStack.last()
    if (shownKey is PlaceForecastKey) {
        closePlaceForecast(placeForecastKey = shownKey)
    } else if (currentBackStack.size > ROOT_SIZE) {
        currentBackStack.removeAt(index = currentBackStack.lastIndex)
    } else if (tabHistory.size > ROOT_SIZE) {
        tabHistory.removeAt(index = tabHistory.lastIndex)
    }
}

private fun NavBackStack<NavKey>.popToRoot() {
    while (size > ROOT_SIZE) {
        removeAt(index = lastIndex)
    }
}