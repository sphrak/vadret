package se.vadretapp.vadret.forecastwarning.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.koin.core.qualifier.named
import se.vadretapp.vadret.di.koinViewModel
import se.vadretapp.vadret.forecastwarning.SIMULATED_WARNING_STORY_QUALIFIER

// The story of a simulated warning is the real story, aurora sky and all, told from the simulated warnings.
@Composable
fun SimulatedWarningStoryRoute(
    warningIdentifiers: List<String>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    WarningStoryRoute(
        warningIdentifiers = warningIdentifiers,
        onDismiss = onDismiss,
        modifier = modifier,
        viewModel = koinViewModel(qualifier = named(name = SIMULATED_WARNING_STORY_QUALIFIER))
    )
}