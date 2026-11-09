package se.vadretapp.vadret.weatherforecast.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.icons.VadretIcon
import se.vadretapp.vadret.setting.model.Theme
import se.vadretapp.vadret.ui.ThemePreviewParameterProvider
import se.vadretapp.vadret.ui.VadretCard
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.collapsed
import se.vadretapp.vadret.ui.resources.coming_days_title
import se.vadretapp.vadret.ui.resources.degree
import se.vadretapp.vadret.ui.resources.expanded
import se.vadretapp.vadret.ui.resources.ic_expand_more
import se.vadretapp.vadret.ui.resources.millimeter
import se.vadretapp.vadret.ui.resources.today
import se.vadretapp.vadret.weatherforecast.model.ComingDay

private const val COLLAPSED_DEGREES = 0f
private const val EXPANDED_DEGREES = 180f
private const val SINGLE_LINE = 1
private const val PRECIPITATION_MAX_LINES = 2
private const val FIRST_DAY_INDEX = 0
private const val EXPANDED_STATE_LAYER_ALPHA = 0.04f
private const val NO_STATE_LAYER_ALPHA = 0f
private const val NO_WIDTH = 0f

private val headerInset = 4.dp
private val cardShape = RoundedCornerShape(size = 21.dp)
private val cardVerticalPadding = 10.5.dp
private val dayRowMinHeight = 55.dp
private val dayRowHorizontalPadding = 21.dp
private val dayRowSpacing = 8.dp
private val dayNameWidth = 55.dp
private val dayNameSpacing = 2.dp
private val dayIconSize = 34.dp
private val dayPrecipitationWidth = 42.dp
private val dayTemperatureWidth = 34.dp
private val chevronSize = 21.dp
private val dividerHorizontalPadding = 21.dp
private val hoursBottomPadding = 2.dp
private val noEdgePadding = 0.dp
private const val RANGE_BAR_WEIGHT = 1f
private val dayNameAutoSize = TextAutoSize.StepBased(
    minFontSize = 11.sp,
    maxFontSize = 16.sp
)

@Composable
internal fun ComingDaysHeader(
    dayCount: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = pluralStringResource(
            resource = Res.plurals.coming_days_title,
            quantity = dayCount,
            dayCount
        ),
        style = VadretTheme.textStyles.heading,
        color = VadretTheme.colorScheme.onBackground,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = headerInset
            )
    )
}

@Composable
internal fun ComingDaysCard(
    comingDays: ImmutableList<ComingDay>,
    dayKeyToShow: String?,
    onDayToggled: (String) -> Unit,
    onDayShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    VadretCard(
        modifier = modifier,
        shape = cardShape
    ) {
        Column {
            comingDays.forEachIndexed { index: Int, comingDay: ComingDay ->
                if (index > 0) {
                    HorizontalDivider(
                        color = VadretTheme.colors.divider,
                        modifier = Modifier.padding(horizontal = dividerHorizontalPadding)
                    )
                }
                ComingDayItem(
                    comingDay = comingDay,
                    isShown = comingDay.key == dayKeyToShow,
                    onDayToggled = onDayToggled,
                    onShown = onDayShown,
                    edgeTopPadding = cardVerticalPadding.onlyOnEdge(isEdge = index == FIRST_DAY_INDEX),
                    edgeBottomPadding = cardVerticalPadding.onlyOnEdge(isEdge = index == comingDays.lastIndex)
                )
            }
        }
    }
}

@Composable
private fun ComingDayItem(
    comingDay: ComingDay,
    isShown: Boolean,
    onDayToggled: (String) -> Unit,
    onShown: () -> Unit,
    edgeTopPadding: Dp,
    edgeBottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    val bringIntoViewRequester: BringIntoViewRequester = remember { BringIntoViewRequester() }
    val expansion: Transition<Boolean> = updateTransition(targetState = comingDay.isExpanded)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester = bringIntoViewRequester)
            .padding(bottom = edgeBottomPadding)
    ) {
        ComingDayRow(
            comingDay = comingDay,
            edgeTopPadding = edgeTopPadding,
            onClick = { onDayToggled(comingDay.key) }
        )
        expansion.AnimatedVisibility(visible = { isExpanded: Boolean -> isExpanded }) {
            ComingHourStrip(
                hours = comingDay.hours,
                firstShownHourIndex = comingDay.firstShownHourIndex,
                modifier = Modifier.padding(bottom = hoursBottomPadding)
            )
        }
    }
    // The day is scrolled to once its hours are open, so the scroll can reach a last day that only fits after it grew.
    if (isShown && expansion.currentState && !expansion.isRunning) {
        ShowDayEffect(
            bringIntoViewRequester = bringIntoViewRequester,
            onShown = onShown
        )
    }
}

// An area as tall as the window from the day's top never fits below it, so the list lines the day up with its top.
@Composable
private fun ShowDayEffect(
    bringIntoViewRequester: BringIntoViewRequester,
    onShown: () -> Unit
) {
    val windowHeight: Int = LocalWindowInfo.current.containerSize.height
    val currentOnShown: () -> Unit by rememberUpdatedState(newValue = onShown)
    LaunchedEffect(bringIntoViewRequester) {
        try {
            bringIntoViewRequester.bringIntoView(
                rect = Rect(
                    offset = Offset.Zero,
                    size = Size(
                        width = NO_WIDTH,
                        height = windowHeight.toFloat()
                    )
                )
            )
        } finally {
            currentOnShown()
        }
    }
}

@Composable
private fun ComingDayRow(
    comingDay: ComingDay,
    edgeTopPadding: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val degree: String = stringResource(resource = Res.string.degree)
    val expansionDescription: String = if (comingDay.isExpanded) {
        stringResource(resource = Res.string.expanded)
    } else {
        stringResource(resource = Res.string.collapsed)
    }
    val chevronRotation: Float by animateFloatAsState(
        targetValue = if (comingDay.isExpanded) {
            EXPANDED_DEGREES
        } else {
            COLLAPSED_DEGREES
        }
    )
    val stateLayerColor: Color = VadretTheme.colorScheme.onSurface
    val stateLayerAlpha: Float by animateFloatAsState(
        targetValue = if (comingDay.isExpanded) {
            EXPANDED_STATE_LAYER_ALPHA
        } else {
            NO_STATE_LAYER_ALPHA
        }
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    color = stateLayerColor,
                    alpha = stateLayerAlpha
                )
            }.clickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { stateDescription = expansionDescription }
            .padding(top = edgeTopPadding)
            .heightIn(min = dayRowMinHeight)
            .padding(horizontal = dayRowHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(space = dayRowSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(space = dayNameSpacing),
            modifier = Modifier.width(width = dayNameWidth)
        ) {
            Text(
                text = if (comingDay.isToday) {
                    stringResource(resource = Res.string.today)
                } else {
                    comingDay.weekdayText
                },
                style = VadretTheme.textStyles.value,
                color = VadretTheme.colorScheme.onSurface,
                autoSize = dayNameAutoSize,
                maxLines = SINGLE_LINE
            )
            Text(
                text = comingDay.dateText,
                style = VadretTheme.textStyles.detailLabel,
                color = VadretTheme.colorScheme.onSurfaceVariant,
                maxLines = SINGLE_LINE
            )
        }
        VadretIcon(
            icon = comingDay.icon,
            contentDescription = stringResource(resource = comingDay.descriptionResource),
            tint = VadretTheme.colors.icon,
            modifier = Modifier.size(size = dayIconSize)
        )
        Text(
            text = comingDay.precipitationText.withMillimeters(),
            style = VadretTheme.textStyles.detailLabel,
            color = VadretTheme.colors.precipitation,
            textAlign = TextAlign.End,
            maxLines = PRECIPITATION_MAX_LINES,
            modifier = Modifier.width(width = dayPrecipitationWidth)
        )
        Text(
            text = "${comingDay.lowTemperatureText}$degree",
            style = VadretTheme.textStyles.listItem,
            color = VadretTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            maxLines = SINGLE_LINE,
            modifier = Modifier.width(width = dayTemperatureWidth)
        )
        TemperatureRangeBar(
            rangeStart = comingDay.temperatureRangeStart,
            rangeEnd = comingDay.temperatureRangeEnd,
            modifier = Modifier.weight(weight = RANGE_BAR_WEIGHT)
        )
        Text(
            text = "${comingDay.highTemperatureText}$degree",
            style = VadretTheme.textStyles.value,
            color = VadretTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = SINGLE_LINE,
            modifier = Modifier.width(width = dayTemperatureWidth)
        )
        Icon(
            painter = painterResource(resource = Res.drawable.ic_expand_more),
            contentDescription = null,
            tint = VadretTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(size = chevronSize)
                .rotate(degrees = chevronRotation)
        )
    }
}

@Composable
private fun String?.withMillimeters(): String {
    val precipitationText: String = this ?: return ""
    return "$precipitationText ${stringResource(resource = Res.string.millimeter)}"
}

@Preview
@Composable
private fun ComingDaysCardPreview(
    @PreviewParameter(provider = ThemePreviewParameterProvider::class) theme: Theme
) {
    VadretTheme(theme = theme) {
        ComingDaysCard(
            comingDays = comingDaysPreview(),
            dayKeyToShow = null,
            onDayToggled = {},
            onDayShown = {},
            modifier = Modifier
                .background(color = VadretTheme.colorScheme.background)
                .padding(all = VadretTheme.spacing.medium)
        )
    }
}

private fun Dp.onlyOnEdge(isEdge: Boolean): Dp =
    if (isEdge) {
        this
    } else {
        noEdgePadding
    }