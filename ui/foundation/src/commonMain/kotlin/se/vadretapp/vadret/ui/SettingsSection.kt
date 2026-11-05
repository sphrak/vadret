package se.vadretapp.vadret.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

// A feature only some builds have adds its own settings screen, which the settings list opens from a row.
interface SettingsSection {
    val key: String
    val titleResource: StringResource
    val iconResource: DrawableResource

    @Composable
    fun Content(
        onBack: () -> Unit,
        modifier: Modifier = Modifier
    )
}