package se.vadretapp.vadret.main

import se.vadretapp.vadret.main.navigation.TopLevelDestination

enum class NativeTab(
    internal val destination: TopLevelDestination,
    val systemImageName: String
) {
    WEATHER(
        destination = TopLevelDestination.WEATHER,
        systemImageName = "cloud.sun"
    ),
    WARNINGS(
        destination = TopLevelDestination.WARNINGS,
        systemImageName = "exclamationmark.triangle"
    ),
    RADAR(
        destination = TopLevelDestination.RADAR,
        systemImageName = "scope"
    ),
    SETTINGS(
        destination = TopLevelDestination.SETTINGS,
        systemImageName = "gearshape"
    )
}