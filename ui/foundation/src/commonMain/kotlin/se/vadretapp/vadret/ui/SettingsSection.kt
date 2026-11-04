package se.vadretapp.vadret.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface SettingsSection {
    val key: String

    @Composable
    fun Content(modifier: Modifier)
}