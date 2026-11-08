package se.vadretapp.vadret.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.place.model.Place
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.back
import se.vadretapp.vadret.ui.resources.clear_search
import se.vadretapp.vadret.ui.resources.ic_arrow_back
import se.vadretapp.vadret.ui.resources.ic_close
import se.vadretapp.vadret.ui.resources.ic_search
import se.vadretapp.vadret.ui.resources.place_municipality_county
import se.vadretapp.vadret.ui.resources.search
import se.vadretapp.vadret.ui.resources.search_nothing_found

private const val SUGGESTION_CONTENT_TYPE = "suggestion"
private const val NOTE_CONTENT_TYPE = "note"
private const val NOTHING_FOUND_KEY = "nothing-found"

// Places are searched the same way wherever one is added, so the forecast and the settings share this search bar.
// An open search fills the screen in the screen colour, so the status bar above it looks like on every other screen.
// A collapsed search is a finished one, so collapsing empties it. Searching picks the first place found, so with none
// found the search stays open to be written again and says so until the query changes or places are found.
@Composable
fun PlaceSearchBar(
    placeholder: String,
    suggestions: ImmutableList<Place>,
    onQueryChange: (String) -> Unit,
    onSearch: (query: String) -> Unit,
    onSuggestionClick: (Place) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    isExpandedAtStart: Boolean = false
) {
    val textFieldState: TextFieldState = rememberTextFieldState()
    val searchBarState: SearchBarState = rememberSearchBarState(
        initialValue = if (isExpandedAtStart) {
            SearchBarValue.Expanded
        } else {
            SearchBarValue.Collapsed
        }
    )
    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    val currentOnQueryChange: (String) -> Unit by rememberUpdatedState(newValue = onQueryChange)
    val currentOnCollapse: () -> Unit by rememberUpdatedState(newValue = onCollapse)
    var isNothingFoundShown: Boolean by remember { mutableStateOf(value = false) }
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .drop(count = 1)
            .collect { query: String ->
                isNothingFoundShown = false
                currentOnQueryChange(query)
            }
    }
    LaunchedEffect(searchBarState) {
        snapshotFlow { searchBarState.currentValue }
            .drop(count = 1)
            .filter { searchBarValue: SearchBarValue -> searchBarValue == SearchBarValue.Collapsed }
            .collect {
                textFieldState.clearText()
                currentOnCollapse()
            }
    }
    val inputField: @Composable () -> Unit = {
        PlaceSearchInputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            placeholder = placeholder,
            onSearch = { query: String ->
                if (suggestions.isNotEmpty()) {
                    onSearch(query)
                    coroutineScope.launch { searchBarState.animateToCollapsed() }
                } else {
                    isNothingFoundShown = query.isNotBlank()
                }
            },
            onBack = { coroutineScope.launch { searchBarState.animateToCollapsed() } }
        )
    }
    SearchBar(
        modifier = modifier,
        state = searchBarState,
        inputField = inputField
    )
    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = inputField,
        colors = SearchBarDefaults.colors(containerColor = VadretTheme.colorScheme.background)
    ) {
        LazyColumn {
            if (isNothingFoundShown && suggestions.isEmpty()) {
                item(key = NOTHING_FOUND_KEY, contentType = NOTE_CONTENT_TYPE) {
                    SettingsNote(text = stringResource(resource = Res.string.search_nothing_found))
                }
            }
            items(
                items = suggestions,
                key = { place: Place -> place.locality + place.municipality + place.county },
                contentType = { SUGGESTION_CONTENT_TYPE }
            ) { place: Place ->
                PlaceSuggestionRow(
                    modifier = Modifier.animateItem(),
                    place = place,
                    onClick = {
                        onSuggestionClick(place)
                        coroutineScope.launch { searchBarState.animateToCollapsed() }
                    }
                )
            }
        }
    }
}

@Composable
private fun PlaceSearchInputField(
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    placeholder: String,
    onSearch: (query: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SearchBarDefaults.InputField(
        modifier = modifier,
        textFieldState = textFieldState,
        searchBarState = searchBarState,
        onSearch = onSearch,
        placeholder = { Text(text = placeholder) },
        leadingIcon = {
            if (searchBarState.currentValue == SearchBarValue.Expanded) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(resource = Res.drawable.ic_arrow_back),
                        contentDescription = stringResource(resource = Res.string.back)
                    )
                }
            } else {
                Icon(
                    painter = painterResource(resource = Res.drawable.ic_search),
                    contentDescription = null
                )
            }
        },
        trailingIcon = if (searchBarState.currentValue == SearchBarValue.Expanded) {
            {
                PlaceSearchActions(
                    isClearShown = textFieldState.text.isNotEmpty(),
                    onClear = { textFieldState.clearText() },
                    onSearch = { onSearch(textFieldState.text.toString()) }
                )
            }
        } else {
            null
        }
    )
}

// An open search keeps the magnifying glass at hand, so a place can be searched without the keyboard's own key.
@Composable
private fun PlaceSearchActions(
    isClearShown: Boolean,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isClearShown) {
            IconButton(onClick = onClear) {
                Icon(
                    painter = painterResource(resource = Res.drawable.ic_close),
                    contentDescription = stringResource(resource = Res.string.clear_search)
                )
            }
        }
        IconButton(onClick = onSearch) {
            Icon(
                painter = painterResource(resource = Res.drawable.ic_search),
                contentDescription = stringResource(resource = Res.string.search)
            )
        }
    }
}

@Composable
private fun PlaceSuggestionRow(
    place: Place,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsRow(
        title = place.locality,
        summary = stringResource(
            resource = Res.string.place_municipality_county,
            place.municipality,
            place.county
        ),
        onClick = onClick,
        modifier = modifier
    )
}