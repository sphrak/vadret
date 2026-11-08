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

// A place forecast belongs to the map it was opened from, so going to the map leaves it.
internal fun TabNavigation.selectTab(tabKey: NavKey) {
    if (tabKey == currentTabKey) {
        tabBackStacks.getValue(key = tabKey).popToRoot()
        return
    }
    if (tabKey == RadarKey) {
        tabBackStacks.getValue(key = WeatherKey).popToPlaceForecast()
    }
    tabHistory.popToRoot()
    if (tabKey != tabHistory.first()) {
        tabHistory.add(element = tabKey)
    }
}

// A second tap while a screen slides in would stack it twice, so a screen already on top is not opened again.
internal fun TabNavigation.open(key: NavKey) {
    val currentBackStack: NavBackStack<NavKey> = tabBackStacks.getValue(key = currentTabKey)
    if (currentBackStack.last() != key) {
        currentBackStack.add(element = key)
    }
}

// A screen opened from another tab is shown right on top of its own tab, whatever that tab showed before.
private fun TabNavigation.openOnTab(
    tabKey: NavKey,
    key: NavKey
) {
    selectTab(tabKey = tabKey)
    val tabBackStack: NavBackStack<NavKey> = tabBackStacks.getValue(key = tabKey)
    tabBackStack.popToRoot()
    tabBackStack.add(element = key)
}

internal fun TabNavigation.openPlaceForecast(placeForecastKey: PlaceForecastKey) {
    openOnTab(
        tabKey = WeatherKey,
        key = placeForecastKey
    )
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

// A place forecast returns to the map it was opened from, so the map lies right beneath it, where a back gesture
// reveals it. Screens opened on top of the forecast stay on top, so closing them only uncovers what lies beneath.
internal fun <T> TabNavigation.shownEntries(tabEntries: Map<NavKey, List<T>>): List<T> =
    tabHistory.flatMap { tabKey: NavKey ->
        val entries: List<T> = tabEntries.getValue(key = tabKey)
        val placeForecastIndex: Int = tabBackStacks.getValue(key = tabKey).indexOfFirst { key: NavKey -> key is PlaceForecastKey }
        if (placeForecastIndex > 0 && RadarKey !in tabHistory) {
            entries.take(n = placeForecastIndex) + tabEntries[RadarKey].orEmpty() + entries.drop(n = placeForecastIndex)
        } else {
            entries
        }
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

private fun NavBackStack<NavKey>.popToPlaceForecast() {
    val placeForecastIndex: Int = indexOfFirst { key: NavKey -> key is PlaceForecastKey }
    if (placeForecastIndex > 0) {
        while (size > placeForecastIndex) {
            removeAt(index = lastIndex)
        }
    }
}

private fun NavBackStack<NavKey>.popToRoot() {
    while (size > ROOT_SIZE) {
        removeAt(index = lastIndex)
    }
}