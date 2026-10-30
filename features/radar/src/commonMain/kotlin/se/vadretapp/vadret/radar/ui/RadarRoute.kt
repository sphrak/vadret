package se.vadretapp.vadret.radar.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.almost_there
import se.vadretapp.vadret.core.resources.m_s
import se.vadretapp.vadret.core.resources.openstreetmap_copyright_notice
import se.vadretapp.vadret.di.koinViewModel
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.radar.RadarEvent
import se.vadretapp.vadret.radar.RadarState
import se.vadretapp.vadret.radar.RadarViewModel
import se.vadretapp.vadret.radar.model.RadarLayer
import se.vadretapp.vadret.radar.wind.WindAtLocation
import se.vadretapp.vadret.radar.wind.WindField
import se.vadretapp.vadret.radar.wind.windAt
import se.vadretapp.vadret.ui.LocalLanguageTag
import se.vadretapp.vadret.ui.VadretProgressBar
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.VadretToast
import se.vadretapp.vadret.ui.VadretToastState
import se.vadretapp.vadret.ui.format.toOneDecimalText
import se.vadretapp.vadret.ui.rememberVadretToastState
import kotlin.math.roundToInt

private val toolbarTopPadding = 16.dp
private val toastDockSpacing = 64.dp
private val attributionShape = RoundedCornerShape(size = 4.dp)
private val attributionPadding = PaddingValues(
    horizontal = 6.dp,
    vertical = 2.dp
)
private const val NO_DOCK_HEIGHT = 0
private const val COVER_FADE_MILLISECONDS = 250
private const val ATTRIBUTION_BACKGROUND_ALPHA = 0.85f

@Composable
fun RadarRoute(
    modifier: Modifier = Modifier,
    initialImageUrl: String? = null,
    viewModel: RadarViewModel = koinViewModel()
) {
    val state: RadarState by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(initialImageUrl) {
        viewModel.onEvent(event = RadarEvent.OnStart(initialImageUrl = initialImageUrl))
    }
    RadarScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

@Composable
internal fun RadarScreen(
    state: RadarState,
    onEvent: (RadarEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    val languageTag: String = LocalLanguageTag.current
    LaunchedEffect(languageTag) {
        onEvent(RadarEvent.OnLocaleChanged(languageTag = languageTag))
    }
    val isDarkTheme: Boolean = VadretTheme.isDark
    LaunchedEffect(isDarkTheme) {
        onEvent(RadarEvent.OnDarkThemeChanged(isDarkTheme = isDarkTheme))
    }
    DisposableEffect(Unit) {
        onDispose { onEvent(RadarEvent.OnLeft) }
    }
    if (state.isPollenHibernationNoticeVisible) {
        PollenHibernationDialog(onDismiss = { onEvent(RadarEvent.OnPollenHibernationNoticeDismissed) })
    }
    val errorMessageResource: StringResource? = state.errorMessageResource
    if (errorMessageResource != null) {
        val message: String = stringResource(resource = errorMessageResource)
        LaunchedEffect(errorMessageResource) {
            snackbarHostState.showSnackbar(message = message)
            onEvent(RadarEvent.OnErrorShown)
        }
    }
    val windAtLocation: WindAtLocation? = windAtDeviceLocation(
        layer = state.layer,
        windField = state.shownWindField,
        deviceLocation = state.deviceLocation
    )

    var dockHeight: Int by remember { mutableIntStateOf(value = NO_DOCK_HEIGHT) }
    val toastState: VadretToastState = rememberVadretToastState()
    val almostThere: String = stringResource(resource = Res.string.almost_there)
    Box(modifier = modifier.fillMaxSize()) {
        RadarMap(
            layer = state.layer,
            precipitationOverlay = state.precipitationOverlay,
            windField = state.shownWindField,
            deviceLocation = state.deviceLocation,
            windAtLocation = windAtLocation,
            windSpeedText = windAtLocation?.windSpeedText(unit = stringResource(resource = Res.string.m_s)),
            areRadarStationsShown = state.areRadarStationsShown,
            pollenMarkers = state.pollenMarkers,
            pollenClouds = state.pollenClouds,
            obscuredBottom = dockHeight,
            onMapLoaded = { onEvent(RadarEvent.OnMapLoaded) },
            modifier = Modifier.fillMaxSize()
        )
        AnimatedVisibility(
            visible = state.isInitialLoading,
            enter = EnterTransition.None,
            exit = fadeOut(animationSpec = tween(durationMillis = COVER_FADE_MILLISECONDS)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = VadretTheme.colorScheme.background)
            )
        }
        if (state.isLoading || state.isInitialLoading) {
            VadretProgressBar(
                modifier = Modifier
                    .align(alignment = Alignment.TopCenter)
                    .statusBarsPadding()
            )
        }
        HiddenToggle(
            onAlmostThere = { toastState.show(message = almostThere) },
            onToggle = { onEvent(RadarEvent.OnRadarStationsToggled) },
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .statusBarsPadding()
        )
        RadarLayerToolbar(
            layers = state.layers,
            selectedLayerIndex = state.layerIndex,
            onLayerSelected = { layer: RadarLayer -> onEvent(RadarEvent.OnLayerSelected(layer = layer)) },
            modifier = Modifier
                .align(alignment = Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = toolbarTopPadding)
        )
        RadarBottomPanel(
            state = state,
            onEvent = onEvent,
            onDockHeightChanged = { height: Int -> dockHeight = height },
            modifier = Modifier.align(alignment = Alignment.BottomCenter)
        )
        VadretToast(
            state = toastState,
            modifier = Modifier
                .align(alignment = Alignment.BottomCenter)
                .padding(bottom = with(receiver = LocalDensity.current) { dockHeight.toDp() } + toastDockSpacing)
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(alignment = Alignment.BottomCenter)
        )
    }
}

@Composable
private fun RadarBottomPanel(
    state: RadarState,
    onEvent: (RadarEvent) -> Unit,
    onDockHeightChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density: Density = LocalDensity.current
    val dockCorner: CornerSize = VadretTheme.shapes.large.topStart
    val dockModifier: Modifier = Modifier.onSizeChanged { size: IntSize ->
        val cornerHeight: Float = dockCorner.toPx(
            shapeSize = size.toSize(),
            density = density
        )
        onDockHeightChanged((size.height - cornerHeight).roundToInt())
    }
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(space = VadretTheme.spacing.small),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(resource = Res.string.openstreetmap_copyright_notice),
            style = VadretTheme.textStyles.attribution,
            color = VadretTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = VadretTheme.spacing.medium)
                .background(
                    color = VadretTheme.colorScheme.surface.copy(alpha = ATTRIBUTION_BACKGROUND_ALPHA),
                    shape = attributionShape
                ).padding(paddingValues = attributionPadding)
        )
        val timeline: RadarTimeline? = radarTimeline(state = state)
        if (state.layer == RadarLayer.POLLEN) {
            PollenCard(
                pollenTypes = state.pollenTypes,
                selectedPollenId = state.selectedPollenId,
                dayText = state.pollenDayText,
                dateText = state.pollenDateText,
                onPollenTypeSelected = { pollenId: String -> onEvent(RadarEvent.OnPollenTypeSelected(pollenId = pollenId)) },
                modifier = dockModifier
            )
        } else if (timeline != null || state.isLoading || state.isInitialLoading) {
            RadarTimelineCard(
                layer = state.layer,
                timeline = timeline,
                isPlaying = state.isPlaying,
                onPlayClick = { onEvent(RadarEvent.OnPlayButtonClicked) },
                onIndexChange = { index: Int -> onEvent(RadarEvent.OnIndexChanged(index = index)) },
                modifier = dockModifier
            )
        }
    }
}

@Composable
private fun windAtDeviceLocation(
    layer: RadarLayer,
    windField: WindField?,
    deviceLocation: Location?
): WindAtLocation? =
    remember(layer, windField, deviceLocation) {
        if (layer == RadarLayer.WIND && windField != null && deviceLocation != null) {
            windField.values.windAt(
                latitude = deviceLocation.latitude,
                longitude = deviceLocation.longitude
            )
        } else {
            null
        }
    }

private fun WindAtLocation.windSpeedText(unit: String): String = "${windSpeed.toDouble().toOneDecimalText()} $unit"