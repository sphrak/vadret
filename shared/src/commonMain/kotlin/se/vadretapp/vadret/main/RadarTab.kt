package se.vadretapp.vadret.main

import androidx.compose.runtime.Composable

// The app supplies the place island, since the radar can neither depend on the weather forecast nor switch tabs.
class RadarTab(
    val host: @Composable (content: @Composable () -> Unit) -> Unit,
    val content: @Composable (placeIsland: @Composable (latitude: Double, longitude: Double, onDismiss: () -> Unit) -> Unit) -> Unit
)