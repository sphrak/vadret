package se.vadretapp.vadret.main

import se.vadretapp.vadret.forecastwarning.warningStorySimulationSection
import se.vadretapp.vadret.main.navigation.AppNavigator
import se.vadretapp.vadret.main.navigation.SimulatedWarningStoryKey
import se.vadretapp.vadret.ui.DeveloperSettingsSection

internal fun List<DeveloperSettingsSection>.withWarningStorySimulation(appNavigator: AppNavigator): List<DeveloperSettingsSection> =
    this + warningStorySimulationSection(
        onStoryClick = { warningIdentifiers: List<String> ->
            appNavigator.open(key = SimulatedWarningStoryKey(warningIdentifiers = warningIdentifiers))
        }
    )