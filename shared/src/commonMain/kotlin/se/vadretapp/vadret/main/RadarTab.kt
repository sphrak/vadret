package se.vadretapp.vadret.main

import androidx.compose.runtime.Composable

class RadarTab(
    val host: @Composable (content: @Composable () -> Unit) -> Unit,
    val content: @Composable () -> Unit
)