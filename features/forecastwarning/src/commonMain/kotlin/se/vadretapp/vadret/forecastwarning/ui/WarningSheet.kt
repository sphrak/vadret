package se.vadretapp.vadret.forecastwarning.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.forecastwarning.model.AuroraHourItem
import se.vadretapp.vadret.forecastwarning.model.WarningDetail
import se.vadretapp.vadret.forecastwarning.model.WarningItem
import se.vadretapp.vadret.icons.VadretIcon
import se.vadretapp.vadret.icons.VadretIconGraphic
import se.vadretapp.vadret.ui.HourChartSlice
import se.vadretapp.vadret.ui.HourColumn
import se.vadretapp.vadret.ui.HourLabel
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.WarningColors
import se.vadretapp.vadret.ui.elapsedTimeText
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.aurora_hour_score
import se.vadretapp.vadret.ui.resources.source_and_time
import se.vadretapp.vadret.ui.resources.warning_from
import se.vadretapp.vadret.ui.resources.warning_period
import se.vadretapp.vadret.ui.resources.warning_until
import se.vadretapp.vadret.ui.resources.warning_when
import se.vadretapp.vadret.ui.warningColors
import se.vadretapp.vadret.ui.warningLabel

internal const val GOLDEN_RATIO = 1.618f

internal val sheetHorizontalPadding = 35.dp
private val sheetTopPadding = 21.dp
private val sheetBottomPadding = 34.dp
private val headlineTopPadding = 5.dp
private val areaTopPadding = 13.dp
private val factSpacing = 21.dp
private val factLabelSpacing = 3.dp
private val dividerWidth = 1.dp
private val iconSpacing = 16.dp
private val levelIconSize = 48.dp
private val hourLabelBottomPadding = 4.dp
private val auroraChartHeight = 64.dp
private val auroraLineTop = 20.dp
private val auroraLineRange = 36.dp
private const val AURORA_HOUR_CONTENT_TYPE = "aurora-hour"
private const val DIVIDER_ALPHA = 0.2f
private const val OPAQUE = 1f
private const val EXPANDED = 0f

@Composable
internal fun WarningLevelBlock(
    warning: WarningItem,
    colors: WarningColors,
    modifier: Modifier = Modifier,
    collapsedColor: Color = colors.container,
    collapsedFraction: () -> Float = { EXPANDED },
    contentAlpha: () -> Float = { OPAQUE }
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    color = lerp(
                        start = colors.container,
                        stop = collapsedColor,
                        fraction = collapsedFraction()
                    )
                )
            }
    ) {
        HorizontalDivider(
            thickness = dividerWidth,
            color = colors.content.copy(alpha = DIVIDER_ALPHA)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = iconSpacing),
            modifier = Modifier
                .graphicsLayer { alpha = contentAlpha() }
                .padding(
                    start = sheetHorizontalPadding,
                    top = sheetTopPadding,
                    end = sheetHorizontalPadding,
                    bottom = sheetBottomPadding
                )
        ) {
            WarningLevelText(
                warning = warning,
                colors = colors,
                modifier = Modifier.weight(weight = 1f)
            )
            val icon: VadretIconGraphic? = warning.icon
            if (icon != null) {
                VadretIcon(
                    icon = icon,
                    contentDescription = null,
                    tint = colors.content,
                    modifier = Modifier.size(size = levelIconSize)
                )
            }
        }
    }
}

@Composable
private fun WarningLevelText(
    warning: WarningItem,
    colors: WarningColors,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = warningLabel(
                source = warning.source,
                warningLevel = warning.warningLevel,
                isPublicAlert = warning.isPublicAlert,
                slipperinessRisk = warning.slipperinessRisk,
                auroraChance = warning.auroraChance
            ),
            style = VadretTheme.textStyles.value,
            color = colors.content
        )
        Text(
            text = warning.headlineText(),
            style = VadretTheme.textStyles.headline,
            color = colors.content,
            modifier = Modifier.padding(top = headlineTopPadding)
        )
        val areaName: String? = warning.areaName
        if (areaName != null) {
            Text(
                text = areaName,
                style = VadretTheme.textStyles.listItem,
                color = colors.content,
                modifier = Modifier.padding(top = areaTopPadding)
            )
        }
    }
}

@Composable
internal fun WarningFacts(
    warning: WarningItem,
    detailMaxLines: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        val periodText: String? = periodText(warning = warning)
        if (periodText != null) {
            WarningFact(
                label = stringResource(resource = Res.string.warning_when),
                text = periodText,
                maxLines = detailMaxLines
            )
        }
        warning.details.forEach { warningDetail: WarningDetail ->
            WarningDetailFact(
                warningDetail = warningDetail,
                maxLines = detailMaxLines
            )
        }
    }
}

@Composable
private fun WarningDetailFact(
    warningDetail: WarningDetail,
    maxLines: Int,
    modifier: Modifier = Modifier
) {
    when (warningDetail) {
        is WarningDetail.Labelled -> WarningFact(
            label = stringResource(resource = warningDetail.labelResource),
            text = warningDetail.text,
            maxLines = maxLines,
            modifier = modifier
        )
        is WarningDetail.Titled -> WarningFact(
            label = warningDetail.title,
            text = warningDetail.text,
            maxLines = maxLines,
            modifier = modifier
        )
        is WarningDetail.Paragraph -> WarningFact(
            label = null,
            text = warningDetail.text,
            maxLines = maxLines,
            modifier = modifier
        )
        is WarningDetail.Explained -> WarningFact(
            label = stringResource(resource = warningDetail.labelResource),
            text = stringResource(resource = warningDetail.textResource),
            maxLines = maxLines,
            modifier = modifier
        )
        is WarningDetail.Hourly -> HourlyFact(
            label = stringResource(resource = warningDetail.labelResource),
            auroraHourItems = warningDetail.auroraHourItems,
            firstShownHourIndex = warningDetail.firstShownHourIndex,
            modifier = modifier
        )
    }
}

@Composable
private fun HourlyFact(
    label: String,
    auroraHourItems: ImmutableList<AuroraHourItem>,
    firstShownHourIndex: Int,
    modifier: Modifier = Modifier
) {
    val textMeasurer: TextMeasurer = rememberTextMeasurer()
    Column(
        verticalArrangement = Arrangement.spacedBy(space = factLabelSpacing),
        modifier = modifier.padding(bottom = factSpacing)
    ) {
        Text(
            text = label,
            style = VadretTheme.textStyles.listItem.copy(fontWeight = FontWeight.Bold),
            color = VadretTheme.colorScheme.onSurface
        )
        LazyRow(
            state = rememberLazyListState(initialFirstVisibleItemIndex = firstShownHourIndex),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(
                items = auroraHourItems,
                key = { _: Int, auroraHourItem: AuroraHourItem -> auroraHourItem.key },
                contentType = { _: Int, _: AuroraHourItem -> AURORA_HOUR_CONTENT_TYPE }
            ) { index: Int, _: AuroraHourItem ->
                AuroraHourColumn(
                    auroraHourItems = auroraHourItems,
                    index = index,
                    textMeasurer = textMeasurer
                )
            }
        }
    }
}

@Composable
private fun AuroraHourColumn(
    auroraHourItems: ImmutableList<AuroraHourItem>,
    index: Int,
    textMeasurer: TextMeasurer,
    modifier: Modifier = Modifier
) {
    val auroraHourItem: AuroraHourItem = auroraHourItems[index]
    val scoreText: String = stringResource(
        resource = Res.string.aurora_hour_score,
        auroraHourItem.score
    )
    HourColumn(
        isNow = auroraHourItem.isNow,
        modifier = modifier
    ) {
        HourLabel(
            timeText = auroraHourItem.hourText,
            isNow = auroraHourItem.isNow,
            modifier = Modifier.padding(bottom = hourLabelBottomPadding)
        )
        HourChartSlice(
            hours = auroraHourItems,
            index = index,
            level = { shownHour: AuroraHourItem -> shownHour.level },
            lineColor = VadretTheme.colorScheme.primary,
            lineTop = auroraLineTop,
            lineRange = auroraLineRange,
            labelText = scoreText,
            labelStyle = VadretTheme.textStyles.detailLabelStrong,
            labelColor = VadretTheme.colorScheme.onSurface,
            textMeasurer = textMeasurer,
            modifier = Modifier
                .height(height = auroraChartHeight)
                .semantics { contentDescription = scoreText }
        )
    }
}

@Composable
private fun WarningFact(
    label: String?,
    text: String,
    maxLines: Int,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(space = factLabelSpacing),
        modifier = modifier.padding(bottom = factSpacing)
    ) {
        if (label != null) {
            Text(
                text = label,
                style = VadretTheme.textStyles.listItem.copy(fontWeight = FontWeight.Bold),
                color = VadretTheme.colorScheme.onSurface
            )
        }
        Text(
            text = text,
            style = VadretTheme.textStyles.listItem,
            color = VadretTheme.colorScheme.onSurface,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun WarningFooter(
    warning: WarningItem,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(
            resource = Res.string.source_and_time,
            warning.publisher,
            elapsedTimeText(elapsedTime = warning.elapsedTime)
        ),
        style = VadretTheme.textStyles.listItem,
        color = VadretTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
@ReadOnlyComposable
internal fun WarningItem.colors(): WarningColors =
    warningColors(
        warningLevel = warningLevel,
        isPublicAlert = isPublicAlert
    )

@Composable
internal fun periodText(warning: WarningItem): String? {
    val startsAtText: String? = warning.startsAtText
    val endsAtText: String? = warning.endsAtText
    return when {
        startsAtText != null && endsAtText != null -> stringResource(
            resource = Res.string.warning_period,
            startsAtText,
            endsAtText
        )
        endsAtText != null -> stringResource(
            resource = Res.string.warning_until,
            endsAtText
        ).capitalized()
        startsAtText != null -> stringResource(
            resource = Res.string.warning_from,
            startsAtText
        ).capitalized()
        else -> null
    }
}

@Composable
internal fun WarningItem.headlineText(): String {
    val resource: StringResource? = headlineResource
    return if (resource != null) {
        stringResource(
            resource = resource,
            *headlineArguments.toTypedArray()
        )
    } else {
        headline
    }
}

private fun String.capitalized(): String = replaceFirstChar { character: Char -> character.titlecase() }