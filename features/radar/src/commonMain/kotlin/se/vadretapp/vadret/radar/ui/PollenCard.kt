package se.vadretapp.vadret.radar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.radar.model.RadarLayer
import se.vadretapp.vadret.ui.VadretTheme

private const val POLLEN_TYPE_CONTENT_TYPE = "pollen_type"

@Composable
internal fun PollenCard(
    pollenTypes: ImmutableList<PollenType>,
    selectedPollenId: String?,
    dayText: String?,
    dateText: String?,
    onPollenTypeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TimelineCardSurface(modifier = modifier) {
        if (dayText != null && dateText != null) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = dayText,
                    style = VadretTheme.textStyles.heading,
                    color = VadretTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(weight = 1f)
                        .alignByBaseline()
                )
                Text(
                    text = dateText,
                    style = VadretTheme.textStyles.label,
                    color = VadretTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline()
                )
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(space = VadretTheme.spacing.small),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(
                items = pollenTypes,
                key = { pollenType: PollenType -> pollenType.pollenId },
                contentType = { POLLEN_TYPE_CONTENT_TYPE }
            ) { pollenType: PollenType ->
                FilterChip(
                    selected = pollenType.pollenId == selectedPollenId,
                    onClick = { onPollenTypeSelected(pollenType.pollenId) },
                    label = { Text(text = pollenType.name) }
                )
            }
        }
        Legend(layer = RadarLayer.POLLEN)
    }
}