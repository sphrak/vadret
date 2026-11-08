package se.vadretapp.vadret.weatherforecast.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.aggregatedfeed.model.FeedSource
import se.vadretapp.vadret.aggregatedfeed.model.WarningLevel
import se.vadretapp.vadret.icons.VadretIcon
import se.vadretapp.vadret.icons.VadretIconGraphic
import se.vadretapp.vadret.setting.model.Theme
import se.vadretapp.vadret.ui.ThemePreviewParameterProvider
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.WarningColors
import se.vadretapp.vadret.ui.extension.iconResource
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.warning_until
import se.vadretapp.vadret.ui.warningColors
import se.vadretapp.vadret.ui.warningLabel
import se.vadretapp.vadret.weatherforecast.model.LatestWarning

private val pillCornerRadius = 21.dp
private val pillPadding = 13.dp
private val pillIconSize = 21.dp
private val pillBorderWidth = 1.dp
private const val PILL_BORDER_ALPHA = 0.2f
private const val LABEL_SEPARATOR = " "

@Composable
internal fun WarningPill(
    latestWarning: LatestWarning,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors: WarningColors = warningColors(
        warningLevel = latestWarning.warningLevel,
        isPublicAlert = latestWarning.isPublicAlert
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(size = pillCornerRadius),
        color = colors.container,
        contentColor = colors.content,
        border = BorderStroke(
            width = pillBorderWidth,
            color = colors.content.copy(alpha = PILL_BORDER_ALPHA)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = VadretTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(all = pillPadding)
        ) {
            val icon: VadretIconGraphic? = latestWarning.icon
            if (icon != null) {
                VadretIcon(
                    icon = icon,
                    contentDescription = null,
                    tint = colors.content,
                    modifier = Modifier.size(size = pillIconSize)
                )
            } else {
                Icon(
                    painter = painterResource(resource = latestWarning.source.iconResource(warningLevel = latestWarning.warningLevel)),
                    contentDescription = null,
                    modifier = Modifier.size(size = pillIconSize)
                )
            }
            Text(
                text = pillText(latestWarning = latestWarning),
                style = VadretTheme.textStyles.listItem,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun pillText(latestWarning: LatestWarning): AnnotatedString {
    val label: String = warningLabel(
        source = latestWarning.source,
        warningLevel = latestWarning.warningLevel,
        isPublicAlert = latestWarning.isPublicAlert,
        slipperinessRisk = latestWarning.slipperinessRisk
    )
    val endsAtText: String? = latestWarning.endsAtText
    val detail: String = if (endsAtText != null) {
        stringResource(
            resource = Res.string.warning_until,
            endsAtText
        )
    } else {
        latestWarning.headline
    }
    return buildAnnotatedString {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Medium)) {
            append(text = label)
        }
        append(text = LABEL_SEPARATOR)
        append(text = detail)
    }
}

@Preview
@Composable
private fun WarningPillPreview(
    @PreviewParameter(provider = ThemePreviewParameterProvider::class) theme: Theme
) {
    VadretTheme(theme = theme) {
        WarningPill(
            latestWarning = LatestWarning(
                warningLevel = WarningLevel.YELLOW,
                slipperinessRisk = null,
                icon = null,
                source = FeedSource.SMHI,
                isPublicAlert = false,
                headline = "Kuling 14-17 m/s",
                endsAtText = "13:00",
                warningIdentifiers = persistentListOf("smhi-3376-10720")
            ),
            onClick = {}
        )
    }
}