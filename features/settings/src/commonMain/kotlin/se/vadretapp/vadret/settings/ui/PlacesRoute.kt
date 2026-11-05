package se.vadretapp.vadret.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.di.koinViewModel
import se.vadretapp.vadret.followedplace.model.FavoriteChoice
import se.vadretapp.vadret.place.model.Place
import se.vadretapp.vadret.settings.PlacesEvent
import se.vadretapp.vadret.settings.PlacesState
import se.vadretapp.vadret.settings.PlacesViewModel
import se.vadretapp.vadret.settings.model.FailureDialog
import se.vadretapp.vadret.theme.model.Theme
import se.vadretapp.vadret.ui.InfoDialog
import se.vadretapp.vadret.ui.LocationPermissionEffect
import se.vadretapp.vadret.ui.PlaceSearchBar
import se.vadretapp.vadret.ui.SettingsNote
import se.vadretapp.vadret.ui.SettingsRow
import se.vadretapp.vadret.ui.SettingsScaffold
import se.vadretapp.vadret.ui.SettingsSectionHeader
import se.vadretapp.vadret.ui.SwitchRow
import se.vadretapp.vadret.ui.ThemePreviewParameterProvider
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.ic_check
import se.vadretapp.vadret.ui.resources.ic_delete
import se.vadretapp.vadret.ui.resources.ic_more_vert
import se.vadretapp.vadret.ui.resources.ic_star
import se.vadretapp.vadret.ui.resources.places_follow_position
import se.vadretapp.vadret.ui.resources.places_follow_position_summary
import se.vadretapp.vadret.ui.resources.places_make_default
import se.vadretapp.vadret.ui.resources.places_mine
import se.vadretapp.vadret.ui.resources.places_not_followed
import se.vadretapp.vadret.ui.resources.places_note
import se.vadretapp.vadret.ui.resources.places_options
import se.vadretapp.vadret.ui.resources.places_remove
import se.vadretapp.vadret.ui.resources.places_search
import se.vadretapp.vadret.ui.resources.selected
import se.vadretapp.vadret.ui.resources.settings_places

private const val FOLLOW_POSITION_KEY = "follow-position"
private const val SEARCH_KEY = "search"
private const val MINE_HEADER_KEY = "mine-header"
private const val NOTE_KEY = "note"

private enum class PlacesContentType {
    ROW,
    SEARCH,
    HEADER,
    PLACE,
    NOTE
}

@Composable
fun PlacesRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlacesViewModel = koinViewModel()
) {
    val state: PlacesState by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.onEvent(event = PlacesEvent.OnStart)
    }
    LocationPermissionEffect(
        isRequested = state.isLocationPermissionRequested,
        onResult = { isGranted: Boolean -> viewModel.onEvent(event = PlacesEvent.OnLocationPermissionResult(isGranted = isGranted)) }
    )
    PlacesScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
private fun PlacesScreen(
    state: PlacesState,
    onEvent: (PlacesEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsScaffold(
        title = stringResource(resource = Res.string.settings_places),
        onBack = onBack,
        modifier = modifier
    ) {
        item(key = FOLLOW_POSITION_KEY, contentType = PlacesContentType.ROW) {
            SwitchRow(
                title = stringResource(resource = Res.string.places_follow_position),
                summary = stringResource(resource = Res.string.places_follow_position_summary),
                isChecked = state.isFollowingPosition,
                onCheckedChange = { isChecked: Boolean -> onEvent(PlacesEvent.OnFollowPositionToggled(isFollowingPosition = isChecked)) }
            )
        }
        item(key = SEARCH_KEY, contentType = PlacesContentType.SEARCH) {
            PlaceSearchBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = VadretTheme.spacing.large,
                        vertical = VadretTheme.spacing.small
                    ),
                placeholder = stringResource(resource = Res.string.places_search),
                suggestions = state.suggestions,
                onQueryChange = { query: String -> onEvent(PlacesEvent.OnSearchQueryChanged(query = query)) },
                onSearch = { query: String -> onEvent(PlacesEvent.OnSearchSubmitted(query = query)) },
                onSuggestionClick = { place: Place -> onEvent(PlacesEvent.OnSuggestionClicked(place = place)) },
                onCollapse = { onEvent(PlacesEvent.OnSearchCleared) }
            )
        }
        item(key = MINE_HEADER_KEY, contentType = PlacesContentType.HEADER) {
            SettingsSectionHeader(text = stringResource(resource = Res.string.places_mine))
        }
        items(
            items = state.favoriteChoices,
            key = { favoriteChoice: FavoriteChoice -> favoriteChoice.favorite.favoriteId },
            contentType = { PlacesContentType.PLACE }
        ) { favoriteChoice: FavoriteChoice ->
            PlaceRow(
                favoriteChoice = favoriteChoice,
                onEvent = onEvent
            )
        }
        item(key = NOTE_KEY, contentType = PlacesContentType.NOTE) {
            SettingsNote(text = stringResource(resource = Res.string.places_note))
        }
    }
    val failureDialog: FailureDialog? = state.failureDialog
    if (failureDialog != null) {
        InfoDialog(
            title = stringResource(resource = failureDialog.titleResource),
            text = stringResource(resource = failureDialog.textResource),
            onDismiss = { onEvent(PlacesEvent.OnFailureDismissed) }
        )
    }
}

@Composable
private fun PlaceRow(
    favoriteChoice: FavoriteChoice,
    onEvent: (PlacesEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuExpanded: Boolean by remember { mutableStateOf(value = false) }
    SettingsRow(
        title = favoriteChoice.favorite.locality,
        summary = if (favoriteChoice.isFollowed) {
            null
        } else {
            stringResource(resource = Res.string.places_not_followed)
        },
        onClick = { onEvent(PlacesEvent.OnPlaceChosen(favorite = favoriteChoice.favorite)) },
        modifier = modifier
    ) {
        if (favoriteChoice.isSelected) {
            Icon(
                painter = painterResource(resource = Res.drawable.ic_check),
                contentDescription = stringResource(resource = Res.string.selected),
                tint = VadretTheme.colorScheme.primary
            )
        }
        Box {
            IconButton(onClick = { isMenuExpanded = true }) {
                Icon(
                    painter = painterResource(resource = Res.drawable.ic_more_vert),
                    contentDescription = stringResource(
                        resource = Res.string.places_options,
                        favoriteChoice.favorite.locality
                    )
                )
            }
            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false }
            ) {
                if (!favoriteChoice.isSelected) {
                    PlaceMenuItem(
                        text = stringResource(resource = Res.string.places_make_default),
                        iconResource = Res.drawable.ic_star,
                        onClick = {
                            isMenuExpanded = false
                            onEvent(PlacesEvent.OnPlaceChosen(favorite = favoriteChoice.favorite))
                        }
                    )
                }
                PlaceMenuItem(
                    text = stringResource(resource = Res.string.places_remove),
                    iconResource = Res.drawable.ic_delete,
                    onClick = {
                        isMenuExpanded = false
                        onEvent(PlacesEvent.OnPlaceRemoved(favoriteId = favoriteChoice.favorite.favoriteId))
                    }
                )
            }
        }
    }
}

@Composable
private fun PlaceMenuItem(
    text: String,
    iconResource: DrawableResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenuItem(
        text = { Text(text = text) },
        leadingIcon = {
            Icon(
                painter = painterResource(resource = iconResource),
                contentDescription = null
            )
        },
        onClick = onClick,
        modifier = modifier
    )
}

@Preview(device = Devices.PHONE)
@Composable
private fun PlacesScreenPreview(
    @PreviewParameter(provider = ThemePreviewParameterProvider::class) theme: Theme
) {
    VadretTheme(theme = theme) {
        PlacesScreen(
            state = placesPreviewState(),
            onEvent = {},
            onBack = {},
            modifier = Modifier.background(color = VadretTheme.colorScheme.background)
        )
    }
}