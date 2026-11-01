package se.vadretapp.vadret.forecastwarning.ui

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.back
import se.vadretapp.vadret.core.resources.ic_arrow_back
import se.vadretapp.vadret.di.koinViewModel
import se.vadretapp.vadret.forecastwarning.WarningDetailEvent
import se.vadretapp.vadret.forecastwarning.WarningDetailState
import se.vadretapp.vadret.forecastwarning.WarningDetailViewModel
import se.vadretapp.vadret.forecastwarning.model.WarningItem
import se.vadretapp.vadret.theme.model.Theme
import se.vadretapp.vadret.ui.LocalLanguageTag
import se.vadretapp.vadret.ui.VadretProgressBar
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.WarningColors
import kotlin.math.roundToInt

private const val EXPANDED = 0f
private const val COLLAPSED = 1f
private const val HALF = 0.5f
private const val MINIMUM_RANGE_PX = 1f
private const val TEXT_FADE_END = 0.5f
private const val TITLE_FADE_START = 0.7f

private val detailEdgePadding = 13.dp
private val detailVerticalPadding = 21.dp
private val detailFooterTopPadding = 13.dp
private val detailTopBarHeight = TopAppBarDefaults.TopAppBarExpandedHeight

@Composable
fun WarningDetailRoute(
    warningIdentifier: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WarningDetailViewModel = koinViewModel()
) {
    val state: WarningDetailState by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(warningIdentifier) {
        viewModel.onEvent(event = WarningDetailEvent.OnStart(warningIdentifier = warningIdentifier))
    }
    WarningDetailScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
internal fun WarningDetailScreen(
    state: WarningDetailState,
    onEvent: (WarningDetailEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val languageTag: String = LocalLanguageTag.current
    LaunchedEffect(languageTag) {
        onEvent(WarningDetailEvent.OnLocaleChanged(languageTag = languageTag))
    }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(color = VadretTheme.colorScheme.surface)
    ) {
        val warning: WarningItem? = state.warning
        if (warning != null) {
            WarningDetail(
                warning = warning,
                width = maxWidth,
                height = maxHeight,
                onBack = onBack
            )
        } else {
            RoundIconButton(
                iconResource = Res.drawable.ic_arrow_back,
                contentDescription = Res.string.back,
                onClick = onBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(all = detailEdgePadding)
            )
        }
        val messageResource: StringResource? = state.messageResource
        if (messageResource != null) {
            Text(
                text = stringResource(resource = messageResource),
                style = VadretTheme.textStyles.body,
                color = VadretTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(alignment = Alignment.Center)
                    .padding(all = VadretTheme.spacing.extraLarge)
            )
        }
        if (state.isLoading) {
            VadretProgressBar(modifier = Modifier.align(alignment = Alignment.TopCenter))
        }
    }
}

@Composable
private fun WarningDetail(
    warning: WarningItem,
    width: Dp,
    height: Dp,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors: WarningColors = warning.colors()
    val density: Density = LocalDensity.current
    val statusBarHeight: Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val mapHeight: Dp = width / GOLDEN_RATIO
    val barHeight: Dp = statusBarHeight + detailTopBarHeight
    val headerCollapse: HeaderCollapse = remember { HeaderCollapse() }
    var levelHeightPx: Int by remember { mutableIntStateOf(value = 0) }
    val collapseRangePx: Float = with(receiver = density) { mapHeight.toPx() + levelHeightPx - barHeight.toPx() }
    SideEffect {
        headerCollapse.rangePx = collapseRangePx
    }
    val collapsedColor: Color = VadretTheme.colorScheme.background
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(connection = headerCollapse)
        ) {
            CollapsingHeader(
                mapHeight = mapHeight,
                barHeight = barHeight,
                fraction = { headerCollapse.fraction },
                onLevelHeightMeasured = { measuredHeightPx: Int ->
                    if (levelHeightPx != measuredHeightPx) {
                        levelHeightPx = measuredHeightPx
                    }
                }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    WarningAreaMap(
                        mapArea = warning.mapArea,
                        colors = colors,
                        fitTopPadding = barHeight,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind { drawRect(color = collapsedColor.copy(alpha = headerCollapse.fraction)) }
                    )
                }
                WarningLevelBlock(
                    warning = warning,
                    colors = colors,
                    collapsedColor = collapsedColor,
                    collapsedFraction = { headerCollapse.fraction },
                    contentAlpha = { textAlpha(fraction = headerCollapse.fraction) },
                    modifier = Modifier.clipToBounds()
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f)
            ) {
                WarningFacts(
                    warning = warning,
                    detailMaxLines = Int.MAX_VALUE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(weight = 1f)
                        .verticalScroll(state = rememberScrollState())
                        .padding(
                            start = sheetHorizontalPadding,
                            top = detailVerticalPadding,
                            end = sheetHorizontalPadding
                        )
                )
                WarningFooter(
                    warning = warning,
                    modifier = Modifier.padding(
                        start = sheetHorizontalPadding,
                        top = detailFooterTopPadding,
                        end = sheetHorizontalPadding,
                        bottom = detailVerticalPadding
                    )
                )
            }
        }
        WarningDetailTopBar(
            warning = warning,
            fraction = { headerCollapse.fraction },
            onBack = onBack
        )
    }
}

@Composable
private fun CollapsingHeader(
    mapHeight: Dp,
    barHeight: Dp,
    fraction: () -> Float,
    onLevelHeightMeasured: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier.fillMaxWidth()
    ) { measurables: List<Measurable>, constraints: Constraints ->
        val mapHeightPx: Int = mapHeight.roundToPx()
        val barHeightPx: Float = barHeight.toPx()
        val levelMeasurable: Measurable = measurables.last()
        val naturalLevelHeightPx: Int = levelMeasurable.maxIntrinsicHeight(width = constraints.maxWidth)
        onLevelHeightMeasured(naturalLevelHeightPx)
        val levelHeightPx: Float = naturalLevelHeightPx.toFloat().coerceAtLeast(minimumValue = barHeightPx)
        val collapsedFraction: Float = fraction()
        val shownLevelHeightPx: Int = (levelHeightPx - collapsedFraction * (levelHeightPx - barHeightPx)).roundToInt()
        val shownMapHeightPx: Int = (mapHeightPx * (COLLAPSED - collapsedFraction)).roundToInt()
        val mapPlaceable: Placeable = measurables.first().measure(
            constraints = Constraints.fixed(
                width = constraints.maxWidth,
                height = mapHeightPx
            )
        )
        val levelPlaceable: Placeable = levelMeasurable.measure(
            constraints = Constraints.fixed(
                width = constraints.maxWidth,
                height = shownLevelHeightPx
            )
        )
        layout(
            width = constraints.maxWidth,
            height = shownMapHeightPx + shownLevelHeightPx
        ) {
            mapPlaceable.place(
                x = 0,
                y = shownMapHeightPx - mapHeightPx
            )
            levelPlaceable.place(
                x = 0,
                y = shownMapHeightPx
            )
        }
    }
}

@Composable
private fun WarningDetailTopBar(
    warning: WarningItem,
    fraction: () -> Float,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(space = detailEdgePadding),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(height = detailTopBarHeight)
            .padding(
                start = detailEdgePadding,
                end = sheetHorizontalPadding
            )
    ) {
        RoundIconButton(
            iconResource = Res.drawable.ic_arrow_back,
            contentDescription = Res.string.back,
            onClick = onBack,
            collapsedContentColor = VadretTheme.colorScheme.onBackground,
            collapsedFraction = fraction
        )
        Text(
            text = warning.headline,
            style = VadretTheme.textStyles.title,
            color = VadretTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.graphicsLayer { alpha = titleAlpha(fraction = fraction()) }
        )
    }
}

@Stable
private class HeaderCollapse : NestedScrollConnection {

    var rangePx: Float by mutableFloatStateOf(value = MINIMUM_RANGE_PX)

    private var offsetPx: Float by mutableFloatStateOf(value = EXPANDED)

    val fraction: Float
        get() = (offsetPx / rangePx.coerceAtLeast(minimumValue = MINIMUM_RANGE_PX)).coerceIn(
            minimumValue = EXPANDED,
            maximumValue = COLLAPSED
        )

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource
    ): Offset =
        if (available.y < 0f) {
            collapseBy(delta = available.y)
        } else {
            Offset.Zero
        }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset =
        if (available.y > 0f) {
            collapseBy(delta = available.y)
        } else {
            Offset.Zero
        }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity
    ): Velocity {
        val targetOffsetPx: Float = if (fraction < HALF) {
            EXPANDED
        } else {
            rangePx
        }
        animate(
            initialValue = offsetPx,
            targetValue = targetOffsetPx
        ) { value: Float, _: Float -> offsetPx = value }
        return Velocity.Zero
    }

    private fun collapseBy(delta: Float): Offset {
        val currentOffsetPx: Float = offsetPx.coerceAtMost(maximumValue = rangePx)
        val newOffsetPx: Float = (currentOffsetPx - delta).coerceIn(
            minimumValue = EXPANDED,
            maximumValue = rangePx
        )
        offsetPx = newOffsetPx
        return Offset(
            x = 0f,
            y = currentOffsetPx - newOffsetPx
        )
    }
}

private fun textAlpha(fraction: Float): Float =
    (COLLAPSED - fraction / TEXT_FADE_END).coerceIn(
        minimumValue = EXPANDED,
        maximumValue = COLLAPSED
    )

private fun titleAlpha(fraction: Float): Float =
    ((fraction - TITLE_FADE_START) / (COLLAPSED - TITLE_FADE_START)).coerceIn(
        minimumValue = EXPANDED,
        maximumValue = COLLAPSED
    )

@Preview(device = Devices.PHONE)
@Composable
private fun WarningDetailScreenPreview(
    @PreviewParameter(provider = WarningPreviewParameterProvider::class) warning: WarningItem
) {
    VadretTheme(theme = Theme.LIGHT) {
        WarningDetailScreen(
            state = WarningDetailState(
                warning = warning,
                isLoading = false
            ),
            onEvent = {},
            onBack = {}
        )
    }
}