package fi.kroon.vadret.forecastwarning.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fi.kroon.vadret.core.resources.Res
import fi.kroon.vadret.core.resources.source_and_time
import fi.kroon.vadret.core.resources.warning_from
import fi.kroon.vadret.core.resources.warning_period
import fi.kroon.vadret.core.resources.warning_until
import fi.kroon.vadret.core.resources.warning_when
import fi.kroon.vadret.forecastwarning.model.WarningDetail
import fi.kroon.vadret.forecastwarning.model.WarningItem
import fi.kroon.vadret.ui.VadretTheme
import fi.kroon.vadret.ui.WarningColors
import fi.kroon.vadret.ui.elapsedTimeText
import fi.kroon.vadret.ui.warningColors
import fi.kroon.vadret.ui.warningLabel
import org.jetbrains.compose.resources.stringResource

internal const val GOLDEN_RATIO = 1.618f

internal val sheetHorizontalPadding = 35.dp
private val sheetTopPadding = 21.dp
private val sheetBottomPadding = 34.dp
private val headlineTopPadding = 5.dp
private val areaTopPadding = 13.dp
private val factSpacing = 21.dp
private val factLabelSpacing = 3.dp
private val dividerWidth = 1.dp
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
        Column(
            modifier = Modifier
                .graphicsLayer { alpha = contentAlpha() }
                .padding(
                    start = sheetHorizontalPadding,
                    top = sheetTopPadding,
                    end = sheetHorizontalPadding,
                    bottom = sheetBottomPadding
                )
        ) {
            Text(
                text = warningLabel(
                    source = warning.source,
                    warningLevel = warning.warningLevel,
                    isPublicAlert = warning.isPublicAlert
                ),
                style = VadretTheme.textStyles.value,
                color = colors.content
            )
            Text(
                text = warning.headline,
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
            WarningFact(
                label = warningDetail.label(),
                text = warningDetail.text,
                maxLines = detailMaxLines
            )
        }
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
private fun WarningDetail.label(): String? =
    when (this) {
        is WarningDetail.Labelled -> stringResource(resource = labelResource)
        is WarningDetail.Titled -> title
        is WarningDetail.Paragraph -> null
    }

private fun String.capitalized(): String = replaceFirstChar { character: Char -> character.titlecase() }