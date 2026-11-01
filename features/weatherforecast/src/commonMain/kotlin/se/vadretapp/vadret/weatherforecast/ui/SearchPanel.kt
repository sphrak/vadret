package se.vadretapp.vadret.weatherforecast.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.add_place
import se.vadretapp.vadret.core.resources.close_search
import se.vadretapp.vadret.core.resources.ic_close
import se.vadretapp.vadret.place.model.Place
import se.vadretapp.vadret.ui.VadretCard
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.weatherforecast.WeatherForecastEvent
import se.vadretapp.vadret.weatherforecast.model.SearchState

@Composable
internal fun SearchPanel(
    search: SearchState,
    onEvent: (WeatherForecastEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester: FocusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
    VadretCard(modifier = modifier) {
        TextField(
            value = search.query,
            onValueChange = { query: String -> onEvent(WeatherForecastEvent.OnSearchQueryChanged(query = query)) },
            placeholder = { Text(text = stringResource(resource = Res.string.add_place)) },
            textStyle = VadretTheme.textStyles.body,
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { onEvent(WeatherForecastEvent.OnSearchDismissed) }) {
                    Icon(
                        painter = painterResource(resource = Res.drawable.ic_close),
                        contentDescription = stringResource(resource = Res.string.close_search)
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onEvent(WeatherForecastEvent.OnSearchSubmitted) }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester = focusRequester)
        )
        search.suggestions.forEachIndexed { index: Int, place: Place ->
            if (index > 0) {
                HorizontalDivider(color = VadretTheme.colors.divider)
            }
            SuggestionRow(
                place = place,
                onClick = { onEvent(WeatherForecastEvent.OnSuggestionClicked(place = place)) }
            )
        }
    }
}

@Composable
private fun SuggestionRow(
    place: Place,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = VadretTheme.spacing.large,
                vertical = VadretTheme.spacing.medium
            )
    ) {
        Text(
            text = place.locality,
            style = VadretTheme.textStyles.value,
            color = VadretTheme.colorScheme.onSurface
        )
        Text(
            text = "${place.municipality}, ${place.county}",
            style = VadretTheme.textStyles.detail,
            color = VadretTheme.colorScheme.onSurfaceVariant
        )
    }
}