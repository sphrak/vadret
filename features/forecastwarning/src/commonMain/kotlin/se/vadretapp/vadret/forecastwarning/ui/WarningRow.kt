package se.vadretapp.vadret.forecastwarning.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.forecastwarning.model.WarningItem
import se.vadretapp.vadret.theme.model.Theme
import se.vadretapp.vadret.ui.ThemePreviewParameterProvider
import se.vadretapp.vadret.ui.VadretCard
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.WarningColors
import se.vadretapp.vadret.ui.elapsedTimeText
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.ic_chevron_right
import se.vadretapp.vadret.ui.resources.warning_affects_places
import se.vadretapp.vadret.ui.resources.warning_until
import se.vadretapp.vadret.ui.warningTag

private val rowStartPadding = 21.dp
private val rowEndPadding = 13.dp
private val rowVerticalPadding = 13.dp
private val rowIconSize = 21.dp
private val tagHeight = 21.dp
private val tagShape = RoundedCornerShape(size = 5.dp)
private val bylineSpacing = 13.dp

internal val rowDividerStartPadding = rowStartPadding
internal val rowDividerEndPadding = rowEndPadding

@Composable
internal fun WarningRow(
    warning: WarningItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    edgeTopPadding: Dp = 0.dp,
    edgeBottomPadding: Dp = 0.dp
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape = shape)
            .clickable(onClick = onClick)
            .padding(
                start = rowStartPadding,
                top = rowVerticalPadding + edgeTopPadding,
                end = rowEndPadding,
                bottom = rowVerticalPadding + edgeBottomPadding
            )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = VadretTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(weight = 1f),
                verticalArrangement = Arrangement.spacedBy(space = VadretTheme.spacing.extraSmall)
            ) {
                Text(
                    text = warning.headline,
                    style = VadretTheme.textStyles.value,
                    color = VadretTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(space = bylineSpacing)
                ) {
                    Text(
                        text = warning.byline,
                        style = VadretTheme.textStyles.detailLabel,
                        color = VadretTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(weight = 1f)
                    )
                    Text(
                        text = rowTimeText(warning = warning),
                        style = VadretTheme.textStyles.detailLabel,
                        color = VadretTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                val followedPlaceNames: String? = warning.followedPlaceNames
                if (followedPlaceNames != null) {
                    Text(
                        text = stringResource(
                            resource = Res.string.warning_affects_places,
                            followedPlaceNames
                        ),
                        style = VadretTheme.textStyles.detailLabel,
                        color = VadretTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                painter = painterResource(resource = Res.drawable.ic_chevron_right),
                contentDescription = null,
                tint = VadretTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size = rowIconSize)
            )
        }
        val tag: String? = warningTag(
            warningLevel = warning.warningLevel,
            isPublicAlert = warning.isPublicAlert
        )
        if (tag != null) {
            WarningTagText(
                text = tag,
                colors = warning.colors(),
                modifier = Modifier.padding(top = VadretTheme.spacing.small)
            )
        }
    }
}

@Composable
private fun WarningTagText(
    text: String,
    colors: WarningColors,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = VadretTheme.textStyles.labelStrong,
        color = colors.content,
        maxLines = 1,
        modifier = modifier
            .heightIn(min = tagHeight)
            .background(
                color = colors.container,
                shape = tagShape
            ).wrapContentHeight(align = Alignment.CenterVertically)
            .padding(horizontal = VadretTheme.spacing.small)
    )
}

@Composable
private fun rowTimeText(warning: WarningItem): String {
    val endsAtText: String? = warning.endsAtText
    val timeText: String = if (endsAtText != null) {
        stringResource(
            resource = Res.string.warning_until,
            endsAtText
        )
    } else {
        elapsedTimeText(elapsedTime = warning.elapsedTime)
    }
    return timeText.replaceFirstChar { character: Char -> character.titlecase() }
}

@Preview
@Composable
private fun WarningRowPreview(
    @PreviewParameter(provider = WarningPreviewParameterProvider::class) warning: WarningItem
) {
    VadretTheme(theme = Theme.LIGHT) {
        VadretCard {
            WarningRow(
                warning = warning,
                onClick = {}
            )
        }
    }
}

@Preview
@Composable
private fun WarningRowThemePreview(
    @PreviewParameter(provider = ThemePreviewParameterProvider::class) theme: Theme
) {
    VadretTheme(theme = theme) {
        VadretCard {
            WarningPreviewParameterProvider().values.forEach { warning: WarningItem ->
                WarningRow(
                    warning = warning,
                    onClick = {}
                )
            }
        }
    }
}