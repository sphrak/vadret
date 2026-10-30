package fi.kroon.vadret.radar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import fi.kroon.vadret.core.resources.Res
import fi.kroon.vadret.core.resources.ic_air
import fi.kroon.vadret.core.resources.ic_rainy
import fi.kroon.vadret.core.resources.ic_rainy_filled
import fi.kroon.vadret.core.resources.precipitation
import fi.kroon.vadret.core.resources.wind
import fi.kroon.vadret.radar.model.RadarLayer
import fi.kroon.vadret.ui.VadretSlidingSelector
import fi.kroon.vadret.ui.VadretTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val FILLED_ICON_COVERAGE = 0.5f

private val toolbarPadding = PaddingValues(all = 4.dp)
private val toolbarHeight = 44.dp
private val toolbarElevation = 4.dp
private val layerIconSize = 18.dp
private val layerIconGap = 8.dp
private val layerPadding = PaddingValues(
    start = 10.dp,
    end = 14.dp
)

@Composable
internal fun RadarLayerToolbar(
    selectedLayer: RadarLayer,
    onLayerSelected: (RadarLayer) -> Unit,
    modifier: Modifier = Modifier
) {
    HorizontalFloatingToolbar(
        expanded = true,
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
            toolbarContainerColor = VadretTheme.colorScheme.surface,
            toolbarContentColor = VadretTheme.colorScheme.onSurface
        ),
        contentPadding = toolbarPadding,
        shape = CircleShape,
        expandedShadowElevation = toolbarElevation,
        modifier = modifier.height(height = toolbarHeight)
    ) {
        VadretSlidingSelector(
            optionCount = RadarLayer.entries.size,
            selectedIndex = selectedLayer.ordinal,
            onOptionSelected = { index: Int -> onLayerSelected(RadarLayer.entries[index]) },
            role = Role.RadioButton,
            modifier = Modifier.fillMaxHeight(),
            unselectedContentColor = VadretTheme.colorScheme.onSurface
        ) { index: Int, contentColor: Color, coverage: Float ->
            LayerLabel(
                layer = RadarLayer.entries[index],
                coverage = coverage,
                contentColor = contentColor
            )
        }
    }
}

@Composable
private fun LayerLabel(
    layer: RadarLayer,
    coverage: Float,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val iconTint: Color = lerp(
        start = VadretTheme.colorScheme.onSurfaceVariant,
        stop = VadretTheme.colorScheme.onPrimaryContainer,
        fraction = coverage
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(space = layerIconGap),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(paddingValues = layerPadding)
    ) {
        Icon(
            painter = painterResource(resource = layer.iconResource(isSelected = coverage >= FILLED_ICON_COVERAGE)),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(size = layerIconSize)
        )
        Text(
            text = stringResource(resource = layer.labelResource()),
            style = VadretTheme.textStyles.detailStrong,
            color = contentColor
        )
    }
}

private fun RadarLayer.iconResource(isSelected: Boolean): DrawableResource =
    when (this) {
        RadarLayer.PRECIPITATION -> if (isSelected) {
            Res.drawable.ic_rainy_filled
        } else {
            Res.drawable.ic_rainy
        }
        RadarLayer.WIND -> Res.drawable.ic_air
    }

private fun RadarLayer.labelResource(): StringResource =
    when (this) {
        RadarLayer.PRECIPITATION -> Res.string.precipitation
        RadarLayer.WIND -> Res.string.wind
    }