package se.vadretapp.vadret.weatherforecast.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.followedplace.model.FavoriteChoice
import se.vadretapp.vadret.followedplace.model.LocationChoices
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.add_place
import se.vadretapp.vadret.ui.resources.automatic_location
import se.vadretapp.vadret.ui.resources.choose_location
import se.vadretapp.vadret.ui.resources.ic_add
import se.vadretapp.vadret.ui.resources.ic_check
import se.vadretapp.vadret.ui.resources.ic_close
import se.vadretapp.vadret.ui.resources.ic_my_location
import se.vadretapp.vadret.ui.resources.remove_favorite
import se.vadretapp.vadret.ui.resources.selected
import se.vadretapp.vadret.weatherforecast.WeatherForecastEvent

private val locationTextMaxWidth = 180.dp
private val menuIconSize = 20.dp
private val menuShape = RoundedCornerShape(size = 16.dp)
private val menuMinWidth = 220.dp

@Composable
internal fun LocationPicker(
    localityName: String?,
    locationChoices: LocationChoices,
    onEvent: (WeatherForecastEvent) -> Unit,
    onAddPlaceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded: Boolean by remember { mutableStateOf(value = false) }
    Box(modifier = modifier) {
        Text(
            text = localityName ?: stringResource(resource = Res.string.automatic_location),
            style = VadretTheme.textStyles.value,
            color = VadretTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clip(shape = CircleShape)
                .clickable(
                    onClickLabel = stringResource(resource = Res.string.choose_location),
                    role = Role.Button,
                    onClick = { isExpanded = true }
                )
                .padding(all = VadretTheme.spacing.small)
                .widthIn(max = locationTextMaxWidth)
        )
        LocationMenu(
            isExpanded = isExpanded,
            locationChoices = locationChoices,
            onDismiss = { isExpanded = false },
            onEvent = { event: WeatherForecastEvent ->
                if (event !is WeatherForecastEvent.OnFavoriteRemoveClicked) {
                    isExpanded = false
                }
                onEvent(event)
            },
            onAddPlaceClick = {
                isExpanded = false
                onAddPlaceClick()
            }
        )
    }
}

@Composable
private fun LocationMenu(
    isExpanded: Boolean,
    locationChoices: LocationChoices,
    onDismiss: () -> Unit,
    onEvent: (WeatherForecastEvent) -> Unit,
    onAddPlaceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = isExpanded,
        onDismissRequest = onDismiss,
        shape = menuShape,
        containerColor = VadretTheme.colorScheme.surface,
        modifier = modifier.widthIn(min = menuMinWidth)
    ) {
        DropdownMenuItem(
            text = { MenuText(text = stringResource(resource = Res.string.automatic_location)) },
            leadingIcon = { SelectionMark(isSelected = locationChoices.isAutomatic) },
            trailingIcon = {
                MenuIcon(
                    iconResource = Res.drawable.ic_my_location,
                    modifier = Modifier.minimumInteractiveComponentSize()
                )
            },
            onClick = { onEvent(WeatherForecastEvent.OnAutomaticLocationClicked) }
        )
        if (locationChoices.favorites.isNotEmpty()) {
            HorizontalDivider(color = VadretTheme.colors.divider)
        }
        locationChoices.favorites.forEach { favoriteChoice: FavoriteChoice ->
            DropdownMenuItem(
                text = { MenuText(text = favoriteChoice.favorite.locality) },
                leadingIcon = { SelectionMark(isSelected = favoriteChoice.isSelected) },
                trailingIcon = {
                    IconButton(onClick = { onEvent(WeatherForecastEvent.OnFavoriteRemoveClicked(favoriteId = favoriteChoice.favorite.favoriteId)) }) {
                        MenuIcon(
                            iconResource = Res.drawable.ic_close,
                            contentDescription = stringResource(
                                resource = Res.string.remove_favorite,
                                favoriteChoice.favorite.locality
                            )
                        )
                    }
                },
                onClick = { onEvent(WeatherForecastEvent.OnFavoriteClicked(favorite = favoriteChoice.favorite)) }
            )
        }
        HorizontalDivider(color = VadretTheme.colors.divider)
        DropdownMenuItem(
            text = { MenuText(text = stringResource(resource = Res.string.add_place)) },
            leadingIcon = { MenuIcon(iconResource = Res.drawable.ic_add) },
            onClick = onAddPlaceClick
        )
    }
}

@Composable
private fun MenuText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = VadretTheme.textStyles.body,
        color = VadretTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun SelectionMark(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    if (isSelected) {
        Icon(
            painter = painterResource(resource = Res.drawable.ic_check),
            contentDescription = stringResource(resource = Res.string.selected),
            tint = VadretTheme.colorScheme.primary,
            modifier = modifier.size(size = menuIconSize)
        )
    } else {
        Spacer(modifier = modifier.size(size = menuIconSize))
    }
}

@Composable
private fun MenuIcon(
    iconResource: DrawableResource,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    Icon(
        painter = painterResource(resource = iconResource),
        contentDescription = contentDescription,
        tint = VadretTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.size(size = menuIconSize)
    )
}