package se.vadretapp.vadret.weatherforecast.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.di.koinViewModel
import se.vadretapp.vadret.place.model.Place
import se.vadretapp.vadret.setting.model.Theme
import se.vadretapp.vadret.ui.PlaceSearchPage
import se.vadretapp.vadret.ui.ThemePreviewParameterProvider
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.places_search
import se.vadretapp.vadret.weatherforecast.PlaceSearchEvent
import se.vadretapp.vadret.weatherforecast.PlaceSearchState
import se.vadretapp.vadret.weatherforecast.PlaceSearchViewModel

@Composable
fun PlaceSearchRoute(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaceSearchViewModel = koinViewModel()
) {
    val state: PlaceSearchState by viewModel.state.collectAsStateWithLifecycle()
    PlaceSearchScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onClose = onClose,
        modifier = modifier
    )
}

@Composable
internal fun PlaceSearchScreen(
    state: PlaceSearchState,
    onEvent: (PlaceSearchEvent) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnClose: () -> Unit by rememberUpdatedState(newValue = onClose)
    if (state.isPlaceChosen) {
        LaunchedEffect(Unit) {
            currentOnClose()
        }
    }
    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    val errorMessageResource: StringResource? = state.errorMessageResource
    if (errorMessageResource != null) {
        val message: String = stringResource(resource = errorMessageResource)
        LaunchedEffect(errorMessageResource) {
            snackbarHostState.showSnackbar(message = message)
            onEvent(PlaceSearchEvent.OnErrorShown)
        }
    }
    Box(modifier = modifier) {
        PlaceSearchPage(
            placeholder = stringResource(resource = Res.string.places_search),
            suggestions = state.suggestions,
            onQueryChange = { query: String -> onEvent(PlaceSearchEvent.OnQueryChanged(query = query)) },
            onSearch = { query: String -> onEvent(PlaceSearchEvent.OnSearchSubmitted(query = query)) },
            onSuggestionClick = { place: Place -> onEvent(PlaceSearchEvent.OnSuggestionClicked(place = place)) },
            onClose = onClose
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(alignment = Alignment.BottomCenter)
                .imePadding()
        )
    }
}

@Preview(device = Devices.PHONE)
@Composable
private fun PlaceSearchScreenPreview(
    @PreviewParameter(provider = ThemePreviewParameterProvider::class) theme: Theme
) {
    VadretTheme(theme = theme) {
        PlaceSearchScreen(
            state = PlaceSearchState(
                query = "Kir",
                suggestions = persistentListOf(
                    Place(
                        locality = "Kiruna",
                        municipality = "Kiruna",
                        county = "Norrbottens län",
                        latitude = 67.8558,
                        longitude = 20.2253
                    )
                )
            ),
            onEvent = {},
            onClose = {}
        )
    }
}