package se.vadretapp.vadret.forecastwarning

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.forecastwarning.ui.WarningStorySimulationRoute
import se.vadretapp.vadret.ui.DeveloperSettingsSection
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.simulated_stories

// The stories open where the app opens every story, so whoever shows the developer settings says how to open one.
fun warningStorySimulationSection(onStoryClick: (warningIdentifiers: List<String>) -> Unit): DeveloperSettingsSection = WarningStorySimulationSection(onStoryClick = onStoryClick)

internal class WarningStorySimulationSection(
    private val onStoryClick: (warningIdentifiers: List<String>) -> Unit
) : DeveloperSettingsSection {
    override val key: String = WARNING_STORY_SIMULATION_SECTION_KEY
    override val titleResource: StringResource = Res.string.simulated_stories

    @Composable
    override fun Content(
        onBack: () -> Unit,
        modifier: Modifier
    ) {
        WarningStorySimulationRoute(
            onBack = onBack,
            onStoryClick = onStoryClick,
            modifier = modifier
        )
    }
}

private const val WARNING_STORY_SIMULATION_SECTION_KEY = "warning-story-simulation"