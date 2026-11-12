package se.vadretapp.vadret.weatherforecast.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.icons.VadretIcon
import se.vadretapp.vadret.setting.model.Theme
import se.vadretapp.vadret.ui.LocalScrollBottomInset
import se.vadretapp.vadret.ui.ThemePreviewParameterProvider
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
import se.vadretapp.vadret.weatherforecast.model.DayToShow
import kotlin.math.max

private const val COLLAPSED_DEGREES = 0f
private const val EXPANDED_DEGREES = 180f
private const val SINGLE_LINE = 1
private const val PRECIPITATION_MAX_LINES = 2
private const val FIRST_DAY_INDEX = 0
private const val EXPANDED_STATE_LAYER_ALPHA = 0.04f
private const val NO_STATE_LAYER_ALPHA = 0f
private const val NO_WIDTH = 0f
private const val NO_HEIGHT = 0
private const val FULL_SEPARATION = 1f
private const val NO_SEPARATION = 0f
private const val CLIPPED_STROKE_FACTOR = 2f
private const val NO_OFFSET = 0f

private val headerInset = 4.dp
private val cardBorderWidth = 1.dp
private val cardVerticalPadding = 10.5.dp
private val dayRowMinHeight = 55.dp
private val dayRowSpacing = 8.dp
private val dayNameWidth = 55.dp
private val dayNameSpacing = 2.dp
private val dayIconSize = 34.dp
private val dayPrecipitationWidth = 42.dp
private val dayTemperatureWidth = 34.dp
private val chevronSize = 21.dp
private val noEdgePadding = 0.dp
private const val RANGE_BAR_WEIGHT = 1f
private val dayNameAutoSize = TextAutoSize.StepBased(
    minFontSize = 11.sp,
    maxFontSize = 16.sp
)

internal object ComingDayCard {
    val OuterRadius = 28.dp
    val Inset = 16.dp
    private val InnerRadius = OuterRadius - Inset

    val InnerShape = RoundedCornerShape(InnerRadius)
}

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
    dayToShow: DayToShow?,
    isInPlace: Boolean,
    onDayToggled: (String) -> Unit,
    onDayScrolledTo: (String) -> Unit,
    onDayShown: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val expansions: List<Transition<Boolean>> = comingDays.map { comingDay: ComingDay ->
        key(comingDay.key) {
            updateTransition(targetState = comingDay.isExpanded)
        }
    }
    val isSettled: Boolean = isInPlace && expansions.all { expansion: Transition<Boolean> -> expansion.isSettled() }
    val separations: List<Float> = comingDays.mapIndexed { index: Int, comingDay: ComingDay ->
        key(comingDay.key) {
            val separation: Float by expansions[index].animateFloat { isExpanded: Boolean ->
                if (isExpanded) {
                    FULL_SEPARATION
                } else {
                    NO_SEPARATION
                }
            }
            separation
        }
    }
    Column(modifier = modifier.fillMaxWidth()) {
        comingDays.forEachIndexed { index: Int, comingDay: ComingDay ->
            val separationAbove: Float = separations.separationAbove(index = index)
            val separationBelow: Float = separations.separationBelow(index = index)
            ComingDayItem(
                modifier = Modifier.padding(
                    top = dayGap(
                        index = index,
                        separationAbove = separationAbove
                    )
                ),
                comingDay = comingDay,
                expansion = expansions[index],
                isFirst = index == FIRST_DAY_INDEX,
                separationAbove = separationAbove,
                separationBelow = separationBelow,
                dayToShow = dayToShow,
                isSettled = isSettled,
                onDayToggled = onDayToggled,
                onDayScrolledTo = onDayScrolledTo,
                onDayShown = onDayShown,
                edgeTopPadding = cardVerticalPadding * separationAbove,
                edgeBottomPadding = cardVerticalPadding * separationBelow * (FULL_SEPARATION - separations[index])
            )
        }
    }
}

private fun List<Float>.separationAbove(index: Int): Float =
    if (index == FIRST_DAY_INDEX) {
        FULL_SEPARATION
    } else {
        max(
            a = this[index],
            b = this[index - 1]
        )
    }

private fun List<Float>.separationBelow(index: Int): Float =
    if (index == lastIndex) {
        FULL_SEPARATION
    } else {
        max(
            a = this[index],
            b = this[index + 1]
        )
    }

private fun dayGap(
    index: Int,
    separationAbove: Float
): Dp =
    if (index == FIRST_DAY_INDEX) {
        noEdgePadding
    } else {
        ForecastCards.Spacing * separationAbove
    }

private fun Modifier.daySegment(
    separationAbove: Float,
    separationBelow: Float,
    color: Color,
    borderColor: Color
): Modifier {
    val shape = RoundedCornerShape(
        topStart = ComingDayCard.OuterRadius * separationAbove,
        topEnd = ComingDayCard.OuterRadius * separationAbove,
        bottomStart = ComingDayCard.OuterRadius * separationBelow,
        bottomEnd = ComingDayCard.OuterRadius * separationBelow
    )
    return clip(shape = shape).drawBehind {
        val outline: Outline = shape.createOutline(
            size = size,
            layoutDirection = layoutDirection,
            density = this
        )
        val borderWidth: Float = cardBorderWidth.toPx()
        val border = Stroke(width = borderWidth * CLIPPED_STROKE_FACTOR)
        drawOutline(
            outline = outline,
            color = color
        )
        clipRect(
            left = borderWidth,
            top = NO_OFFSET,
            right = size.width - borderWidth,
            bottom = borderWidth,
            clipOp = ClipOp.Difference
        ) {
            clipRect(
                left = borderWidth,
                top = size.height - borderWidth,
                right = size.width - borderWidth,
                bottom = size.height,
                clipOp = ClipOp.Difference
            ) {
                drawOutline(
                    outline = outline,
                    color = borderColor,
                    style = border
                )
            }
        }
        clipRect(
            left = borderWidth,
            top = NO_OFFSET,
            right = size.width - borderWidth,
            bottom = borderWidth
        ) {
            drawOutline(
                outline = outline,
                color = borderColor,
                alpha = separationAbove,
                style = border
            )
        }
        clipRect(
            left = borderWidth,
            top = size.height - borderWidth,
            right = size.width - borderWidth,
            bottom = size.height
        ) {
            drawOutline(
                outline = outline,
                color = borderColor,
                alpha = separationBelow,
                style = border
            )
        }
    }
}

@Composable
private fun ComingDayItem(
    comingDay: ComingDay,
    expansion: Transition<Boolean>,
    isFirst: Boolean,
    separationAbove: Float,
    separationBelow: Float,
    dayToShow: DayToShow?,
    isSettled: Boolean,
    onDayToggled: (String) -> Unit,
    onDayScrolledTo: (String) -> Unit,
    onDayShown: (String) -> Unit,
    edgeTopPadding: Dp,
    edgeBottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    val bringIntoViewRequester: BringIntoViewRequester = remember { BringIntoViewRequester() }
    var dayHeight: Int by remember { mutableIntStateOf(value = NO_HEIGHT) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .daySegment(
                separationAbove = separationAbove,
                separationBelow = separationBelow,
                color = VadretTheme.colorScheme.surface,
                borderColor = VadretTheme.colorScheme.outlineVariant
            )
            .bringIntoViewRequester(bringIntoViewRequester = bringIntoViewRequester)
            .onSizeChanged { size: IntSize -> dayHeight = size.height }
            .padding(bottom = edgeBottomPadding)
    ) {
        if (!isFirst) {
            HorizontalDivider(
                modifier = Modifier
                    .padding(horizontal = ComingDayCard.Inset)
                    .graphicsLayer { alpha = FULL_SEPARATION - separationAbove },
                color = VadretTheme.colors.divider
            )
        }
        ComingDayRow(
            comingDay = comingDay,
            edgeTopPadding = edgeTopPadding,
            onClick = { onDayToggled(comingDay.key) }
        )
        expansion.AnimatedVisibility(
            visible = { isExpanded: Boolean -> isExpanded },
            enter = expandVertically(expandFrom = Alignment.Top),
            exit = shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            ComingHourStrip(
                hours = comingDay.hours,
                firstShownHourIndex = comingDay.firstShownHourIndex
            )
        }
    }
    if (isSettled && dayToShow != null && dayToShow.dayKey == comingDay.key) {
        if (dayToShow.isScrolledTo) {
            RevealDayEffect(
                bringIntoViewRequester = bringIntoViewRequester,
                dayHeight = { dayHeight },
                onRevealed = { onDayShown(comingDay.key) }
            )
        } else {
            ScrollToDayEffect(
                bringIntoViewRequester = bringIntoViewRequester,
                onScrolled = { onDayScrolledTo(comingDay.key) }
            )
        }
    }
}

private fun Transition<Boolean>.isSettled(): Boolean = currentState == targetState && !isRunning

@Composable
private fun ScrollToDayEffect(
    bringIntoViewRequester: BringIntoViewRequester,
    onScrolled: () -> Unit
) {
    val windowHeight: Int = LocalWindowInfo.current.containerSize.height
    val currentOnScrolled: () -> Unit by rememberUpdatedState(newValue = onScrolled)
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
            currentOnScrolled()
        }
    }
}

@Composable
private fun RevealDayEffect(
    bringIntoViewRequester: BringIntoViewRequester,
    dayHeight: () -> Int,
    onRevealed: () -> Unit
) {
    val bottomInset: Float = with(receiver = LocalDensity.current) {
        LocalScrollBottomInset.current.toPx()
    }
    val currentOnRevealed: () -> Unit by rememberUpdatedState(newValue = onRevealed)
    LaunchedEffect(bringIntoViewRequester) {
        try {
            bringIntoViewRequester.bringIntoView(
                rect = Rect(
                    offset = Offset.Zero,
                    size = Size(
                        width = NO_WIDTH,
                        height = dayHeight() + bottomInset
                    )
                )
            )
        } finally {
            currentOnRevealed()
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
        },
        animationSpec = tween(200)
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
            .padding(horizontal = ComingDayCard.Inset),
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
            dayToShow = null,
            isInPlace = true,
            onDayToggled = {},
            onDayScrolledTo = {},
            onDayShown = {},
            modifier = Modifier
                .background(color = VadretTheme.colorScheme.background)
                .padding(all = VadretTheme.spacing.medium)
        )
    }
}