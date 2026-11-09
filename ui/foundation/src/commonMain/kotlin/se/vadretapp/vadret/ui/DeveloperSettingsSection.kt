package se.vadretapp.vadret.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.StringResource

// A feature only some builds have adds its own developer screen, which the developer settings open from a row.
interface DeveloperSettingsSection {
    val key: String
    val titleResource: StringResource

    @Composable
    fun Content(
        onBack: () -> Unit,
        modifier: Modifier = Modifier
    )
}