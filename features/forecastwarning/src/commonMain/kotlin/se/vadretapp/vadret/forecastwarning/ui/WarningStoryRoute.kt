package se.vadretapp.vadret.forecastwarning.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import se.vadretapp.vadret.di.koinViewModel
import se.vadretapp.vadret.forecastwarning.WarningStoryEvent
import se.vadretapp.vadret.forecastwarning.WarningStoryState
import se.vadretapp.vadret.forecastwarning.WarningStoryViewModel
import se.vadretapp.vadret.forecastwarning.model.WarningItem
import se.vadretapp.vadret.theme.model.Theme
import se.vadretapp.vadret.ui.LocalLanguageTag
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.WarningColors
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.close
import se.vadretapp.vadret.ui.resources.ic_close

private const val STORY_DURATION_MILLISECONDS = 8_000
private const val STORY_START = 0f
private const val STORY_END = 1f
private const val PREVIOUS_ZONE_FRACTION = 1f / 3f
private const val STORY_DETAIL_MAX_LINES = 4
private const val PREVIEW_PROGRESS = 0.4f

private val storyEdgePadding = 13.dp
private val storyBottomPadding = 34.dp
private val storyTopPadding = 21.dp

@Composable
fun WarningStoryRoute(
    warningIdentifiers: List<String>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WarningStoryViewModel = koinViewModel()
) {
    val state: WarningStoryState by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(warningIdentifiers) {
        viewModel.onEvent(event = WarningStoryEvent.OnStart(warningIdentifiers = warningIdentifiers))
    }
    WarningStoryScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onDismiss = onDismiss,
        modifier = modifier
    )
}

@Composable
internal fun WarningStoryScreen(
    state: WarningStoryState,
    onEvent: (WarningStoryEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    previewProgress: Float = STORY_START
) {
    val languageTag: String = LocalLanguageTag.current
    LaunchedEffect(languageTag) {
        onEvent(WarningStoryEvent.OnLocaleChanged(languageTag = languageTag))
    }
    val currentOnDismiss: () -> Unit by rememberUpdatedState(newValue = onDismiss)
    LaunchedEffect(state.isFinished) {
        if (state.isFinished) {
            currentOnDismiss()
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = VadretTheme.colorScheme.surface)
    ) {
        val warning: WarningItem? = state.warnings.getOrNull(index = state.currentIndex)
        if (warning != null) {
            val progress: Animatable<Float, *> = remember(state.currentIndex) { Animatable(initialValue = previewProgress) }
            LaunchedEffect(state.currentIndex) {
                onEvent(WarningStoryEvent.OnStoryShown(index = state.currentIndex))
                progress.animateTo(
                    targetValue = STORY_END,
                    animationSpec = tween(
                        durationMillis = STORY_DURATION_MILLISECONDS,
                        easing = LinearEasing
                    )
                )
                onEvent(WarningStoryEvent.OnNext)
            }
            WarningStory(
                warning = warning,
                storyCount = state.warnings.size,
                currentIndex = state.currentIndex,
                currentProgress = { progress.value },
                onEvent = onEvent,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun WarningStory(
    warning: WarningItem,
    storyCount: Int,
    currentIndex: Int,
    currentProgress: () -> Float,
    onEvent: (WarningStoryEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors: WarningColors = warning.colors()
    val statusBarHeight: Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val mapHeight: Dp = maxWidth / GOLDEN_RATIO
        Column(modifier = Modifier.fillMaxSize()) {
            WarningAreaMap(
                mapArea = warning.mapArea,
                colors = colors,
                fitTopPadding = statusBarHeight + storyEdgePadding + segmentHeight + storyEdgePadding + LocalMinimumInteractiveComponentSize.current,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height = mapHeight)
            )
            WarningLevelBlock(
                warning = warning,
                colors = colors
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f)
                    .navigationBarsPadding()
                    .padding(
                        start = sheetHorizontalPadding,
                        top = storyTopPadding,
                        end = sheetHorizontalPadding,
                        bottom = storyBottomPadding
                    )
            ) {
                WarningFacts(
                    warning = warning,
                    detailMaxLines = STORY_DETAIL_MAX_LINES,
                    modifier = Modifier
                        .weight(weight = 1f)
                        .clipToBounds()
                )
                WarningFooter(warning = warning)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(key1 = Unit) {
                    detectTapGestures { offset: Offset ->
                        if (offset.x < size.width * PREVIOUS_ZONE_FRACTION) {
                            onEvent(WarningStoryEvent.OnPrevious)
                        } else {
                            onEvent(WarningStoryEvent.OnNext)
                        }
                    }
                }
        )
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(all = storyEdgePadding)
        ) {
            StoryProgress(
                storyCount = storyCount,
                currentIndex = currentIndex,
                currentProgress = currentProgress
            )
            RoundIconButton(
                iconResource = Res.drawable.ic_close,
                contentDescription = Res.string.close,
                onClick = onDismiss,
                modifier = Modifier.padding(top = storyEdgePadding)
            )
        }
    }
}

@Preview(device = Devices.PHONE)
@Composable
private fun WarningStoryScreenPreview(
    @PreviewParameter(provider = WarningPreviewParameterProvider::class) warning: WarningItem
) {
    VadretTheme(theme = Theme.LIGHT) {
        WarningStoryScreen(
            state = WarningStoryState(
                warnings = persistentListOf(warning, warning, warning),
                currentIndex = 0,
                isLoading = false
            ),
            onEvent = {},
            onDismiss = {},
            previewProgress = PREVIEW_PROGRESS
        )
    }
}