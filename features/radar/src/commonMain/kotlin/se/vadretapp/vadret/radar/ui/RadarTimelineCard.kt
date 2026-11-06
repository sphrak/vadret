package se.vadretapp.vadret.radar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.radar.model.RadarLayer
import se.vadretapp.vadret.radar.pollen.PollenColorScale
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.ic_pause_filled
import se.vadretapp.vadret.ui.resources.ic_play_arrow_filled
import se.vadretapp.vadret.ui.resources.pause
import se.vadretapp.vadret.ui.resources.play
import se.vadretapp.vadret.ui.resources.pollen_level_high
import se.vadretapp.vadret.ui.resources.pollen_level_low
import se.vadretapp.vadret.ui.resources.pollen_level_moderate
import se.vadretapp.vadret.ui.resources.pollen_level_very_high
import kotlin.math.roundToInt

private val cardElevation = 6.dp
private val cardTopPadding = 14.dp
private val cardSectionSpacing = 14.dp
private val playButtonSize = 48.dp
private val playIconSize = 28.dp
private val timelineSpacing = 6.dp
private val legendHeight = 6.dp
private val legendScaleSpacing = 4.dp

private val precipitationLegend: List<Color> = listOf(
    Color(color = 0xFF9CC9FF),
    Color(color = 0xFF2F6FE0),
    Color(color = 0xFF2DB34A),
    Color(color = 0xFFF2CF2E),
    Color(color = 0xFFEC5A2A)
)

private val precipitationLegendRainRates: ImmutableList<String> = persistentListOf(
    "0.1 mm/h",
    "0.3 mm/h",
    "1.4 mm/h",
    "6.3 mm/h",
    "29+ mm/h"
)

private val windLegendSpeeds: ImmutableList<String> = persistentListOf(
    "0 m/s",
    "4 m/s",
    "8 m/s",
    "14 m/s",
    "20+ m/s"
)

private val dayWindLegend: List<Color> = listOf(
    Color(color = 0xFF3B46A0),
    Color(color = 0xFF3E6EBE),
    Color(color = 0xFF3496AF),
    Color(color = 0xFF3CAA78),
    Color(color = 0xFF50B446),
    Color(color = 0xFFBEAF46),
    Color(color = 0xFFD28246),
    Color(color = 0xFFC34B78),
    Color(color = 0xFF963CAA)
)

private val nightWindLegend: List<Color> = listOf(
    Color(color = 0xFF0E1430),
    Color(color = 0xFF122050),
    Color(color = 0xFF163070),
    Color(color = 0xFF1A428C),
    Color(color = 0xFF1E56A2),
    Color(color = 0xFF266EB8),
    Color(color = 0xFF3284CA),
    Color(color = 0xFF4298D8),
    Color(color = 0xFF58ACE4)
)

@Composable
internal fun RadarTimelineCard(
    layer: RadarLayer,
    timeline: RadarTimeline,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    TimelineCardSurface(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = cardSectionSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayButton(
                isPlaying = isPlaying,
                onClick = onPlayClick
            )
            TimelineColumn(
                timeline = timeline,
                onIndexChange = onIndexChange,
                modifier = Modifier.weight(weight = 1f)
            )
        }
        Legend(layer = layer)
    }
}

@Composable
internal fun TimelineCardSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = VadretTheme.shapes.large.copy(
            bottomStart = ZeroCornerSize,
            bottomEnd = ZeroCornerSize
        ),
        color = VadretTheme.colorScheme.surface,
        shadowElevation = cardElevation,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(space = cardSectionSpacing),
            modifier = Modifier.padding(
                start = VadretTheme.spacing.large,
                end = VadretTheme.spacing.large,
                top = cardTopPadding,
                bottom = VadretTheme.spacing.large
            ),
            content = content
        )
    }
}

@Composable
private fun PlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconResource: DrawableResource = if (isPlaying) {
        Res.drawable.ic_pause_filled
    } else {
        Res.drawable.ic_play_arrow_filled
    }
    val contentDescriptionResource: StringResource = if (isPlaying) {
        Res.string.pause
    } else {
        Res.string.play
    }
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = VadretTheme.colorScheme.primaryContainer,
        contentColor = VadretTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.size(size = playButtonSize)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(resource = iconResource),
                contentDescription = stringResource(resource = contentDescriptionResource),
                modifier = Modifier.size(size = playIconSize)
            )
        }
    }
}

@Composable
private fun TimelineColumn(
    timeline: RadarTimeline,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(space = timelineSpacing),
        modifier = modifier
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = timeline.timeText,
                style = VadretTheme.textStyles.heading,
                color = VadretTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(weight = 1f)
                    .alignByBaseline()
            )
            Text(
                text = timeline.caption,
                style = VadretTheme.textStyles.label,
                color = VadretTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alignByBaseline()
            )
        }
        RadarSlider(
            value = timeline.index.toFloat(),
            onValueChange = { value: Float -> onIndexChange(value.roundToInt()) },
            valueRange = 0f..timeline.lastIndex.coerceAtLeast(minimumValue = 1).toFloat(),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            TimelineLabel(text = timeline.startLabel)
            TimelineLabel(text = timeline.middleLabel)
            TimelineLabel(text = timeline.endLabel)
        }
    }
}

@Composable
private fun TimelineLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = VadretTheme.textStyles.label,
        color = VadretTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
internal fun Legend(
    layer: RadarLayer,
    modifier: Modifier = Modifier
) {
    when (layer) {
        RadarLayer.PRECIPITATION -> ScaleLegend(
            colors = precipitationLegend,
            labels = precipitationLegendRainRates,
            modifier = modifier
        )
        RadarLayer.WIND -> ScaleLegend(
            colors = if (VadretTheme.isDark) {
                nightWindLegend
            } else {
                dayWindLegend
            },
            labels = windLegendSpeeds,
            modifier = modifier
        )

        RadarLayer.POLLEN -> ScaleLegend(
            colors = PollenColorScale.legendColors,
            labels = persistentListOf(
                stringResource(resource = Res.string.pollen_level_low),
                stringResource(resource = Res.string.pollen_level_moderate),
                stringResource(resource = Res.string.pollen_level_high),
                stringResource(resource = Res.string.pollen_level_very_high)
            ),
            modifier = modifier
        )
    }
}

@Composable
private fun ScaleLegend(
    colors: List<Color>,
    labels: ImmutableList<String>,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(space = legendScaleSpacing),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height = legendHeight)
                .background(
                    brush = Brush.horizontalGradient(colors = colors),
                    shape = CircleShape
                )
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            labels.forEach { label: String ->
                TimelineLabel(text = label)
            }
        }
    }
}