package se.vadretapp.vadret.radar.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.di.koinViewModel
import se.vadretapp.vadret.icons.rememberIsMotionEnabled
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.radar.RadarEvent
import se.vadretapp.vadret.radar.RadarState
import se.vadretapp.vadret.radar.RadarViewModel
import se.vadretapp.vadret.radar.model.RadarLayer
import se.vadretapp.vadret.radar.ui.overlay.MapProjection
import se.vadretapp.vadret.radar.wind.WindAtLocation
import se.vadretapp.vadret.radar.wind.WindField
import se.vadretapp.vadret.radar.wind.windAt
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.ui.LocalHourFormat
import se.vadretapp.vadret.ui.LocalLanguageTag
import se.vadretapp.vadret.ui.VadretProgressBar
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.VadretToast
import se.vadretapp.vadret.ui.VadretToastState
import se.vadretapp.vadret.ui.format.toOneDecimalText
import se.vadretapp.vadret.ui.rememberVadretToastState
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.almost_there
import se.vadretapp.vadret.ui.resources.m_s
import se.vadretapp.vadret.ui.resources.openstreetmap_copyright_notice
import se.vadretapp.vadret.ui.resources.pegman_hint
import kotlin.math.roundToInt

private val toolbarTopPadding = 16.dp
private val toastDockSpacing = 64.dp
private val attributionShape = RoundedCornerShape(size = 4.dp)
private val attributionPadding = PaddingValues(
    horizontal = 6.dp,
    vertical = 2.dp
)
private const val NO_DOCK_HEIGHT = 0
private const val UNMEASURED_SCREEN_HEIGHT = 0
private const val COVER_FADE_MILLISECONDS = 250
private const val DOCK_FADE_MILLISECONDS = 250
private const val ATTRIBUTION_BACKGROUND_ALPHA = 0.85f
private const val ATTRIBUTION_WEIGHT = 1f

@Composable
fun RadarRoute(
    placeIsland: @Composable (latitude: Double, longitude: Double, onDismiss: () -> Unit) -> Unit,
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
        placeIsland = placeIsland,
        modifier = modifier
    )
}

@Composable
internal fun RadarScreen(
    state: RadarState,
    onEvent: (RadarEvent) -> Unit,
    placeIsland: @Composable (latitude: Double, longitude: Double, onDismiss: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    val languageTag: String = LocalLanguageTag.current
    val hourFormat: HourFormat = LocalHourFormat.current
    LaunchedEffect(languageTag, hourFormat) {
        onEvent(
            RadarEvent.OnLocaleChanged(
                languageTag = languageTag,
                hourFormat = hourFormat
            )
        )
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
    val pegmanHint: String = stringResource(resource = Res.string.pegman_hint)
    var mapProjection: MapProjection? by remember { mutableStateOf(value = null) }
    var screenOrigin: Offset by remember { mutableStateOf(value = Offset.Zero) }
    var screenHeight: Int by remember { mutableIntStateOf(value = UNMEASURED_SCREEN_HEIGHT) }
    var draggedLanding: Offset? by remember { mutableStateOf(value = null) }
    val isPegmanDragged: Boolean by remember { derivedStateOf { draggedLanding != null } }
    val droppedLocation: Location? = state.droppedLocation
    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                screenOrigin = coordinates.positionInRoot()
                screenHeight = coordinates.size.height
            }
    ) {
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
            onMapProjectionChanged = { changedMapProjection: MapProjection -> mapProjection = changedMapProjection },
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
        if (state.isProgressShown) {
            VadretProgressBar(
                modifier = Modifier
                    .align(alignment = Alignment.TopCenter)
                    .statusBarsPadding()
            )
        }
        val shownMapProjection: MapProjection? = mapProjection
        if (droppedLocation != null && shownMapProjection != null) {
            DroppedPegman(
                location = droppedLocation,
                mapProjection = shownMapProjection
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
            pegman = {
                PegmanButton(
                    isFigureLifted = isPegmanDragged,
                    onTapped = { toastState.show(message = pegmanHint) },
                    onDragged = { rootLanding: Offset -> draggedLanding = rootLanding - screenOrigin },
                    onDropped = {
                        val location: Location? = droppedPegmanLocation(
                            landing = draggedLanding,
                            mapProjection = mapProjection,
                            visibleMapBottom = screenHeight - dockHeight
                        )
                        draggedLanding = null
                        if (location != null) {
                            onEvent(RadarEvent.OnPegmanDropped(location = location))
                        }
                    },
                    onDragCancelled = { draggedLanding = null }
                )
            },
            placeIsland = { location: Location ->
                placeIsland(location.latitude, location.longitude) { onEvent(RadarEvent.OnPlaceIslandDismissed) }
            },
            modifier = Modifier.align(alignment = Alignment.BottomCenter)
        )
        if (isPegmanDragged) {
            DraggedPegman(landing = { draggedLanding ?: Offset.Zero })
        }
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
    pegman: @Composable () -> Unit,
    placeIsland: @Composable (Location) -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(space = VadretTheme.spacing.small),
        modifier = modifier.fillMaxWidth()
    ) {
        val droppedLocation: Location? = state.droppedLocation
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(space = VadretTheme.spacing.small),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VadretTheme.spacing.medium)
        ) {
            // The map's credit belongs to the map, so it comes with the map as the loading cover fades away.
            val attributionAlpha: Float by animateFloatAsState(
                targetValue = if (state.isInitialLoading) {
                    0f
                } else {
                    1f
                },
                animationSpec = tween(durationMillis = COVER_FADE_MILLISECONDS)
            )
            Box(
                contentAlignment = Alignment.BottomStart,
                modifier = Modifier.weight(weight = ATTRIBUTION_WEIGHT)
            ) {
                Text(
                    text = stringResource(resource = Res.string.openstreetmap_copyright_notice),
                    style = VadretTheme.textStyles.attribution,
                    color = VadretTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .graphicsLayer { alpha = attributionAlpha }
                        .background(
                            color = VadretTheme.colorScheme.surface.copy(alpha = ATTRIBUTION_BACKGROUND_ALPHA),
                            shape = attributionShape
                        ).padding(paddingValues = attributionPadding)
                )
            }
            if (droppedLocation == null) {
                pegman()
            }
        }
        val isMotionEnabled: Boolean = rememberIsMotionEnabled()
        AnimatedContent(
            targetState = droppedLocation,
            contentKey = { location: Location? -> location != null },
            contentAlignment = Alignment.BottomCenter,
            transitionSpec = { dockTransition(isMotionEnabled = isMotionEnabled) },
            modifier = Modifier.fillMaxWidth()
        ) { location: Location? ->
            if (location != null) {
                Box(
                    modifier = dockModifier.padding(
                        start = VadretTheme.spacing.medium,
                        end = VadretTheme.spacing.medium,
                        bottom = VadretTheme.spacing.medium
                    )
                ) {
                    placeIsland(location)
                }
            } else {
                AnimatedVisibility(
                    visible = state.isDockReady,
                    enter = dockEnterTransition(isMotionEnabled = isMotionEnabled),
                    exit = ExitTransition.None
                ) {
                    RadarDock(
                        state = state,
                        onEvent = onEvent,
                        modifier = dockModifier
                    )
                }
            }
        }
    }
}

@Composable
private fun RadarDock(
    state: RadarState,
    onEvent: (RadarEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeline: RadarTimeline? = radarTimeline(state = state)
    if (state.layer == RadarLayer.POLLEN) {
        PollenCard(
            pollenTypes = state.pollenTypes,
            selectedPollenId = state.selectedPollenId,
            dayText = state.pollenDayText,
            dateText = state.pollenDateText,
            onPollenTypeSelected = { pollenId: String -> onEvent(RadarEvent.OnPollenTypeSelected(pollenId = pollenId)) },
            modifier = modifier
        )
    } else if (timeline != null) {
        RadarTimelineCard(
            layer = state.layer,
            timeline = timeline,
            isPlaying = state.isPlaying,
            onPlayClick = { onEvent(RadarEvent.OnPlayButtonClicked) },
            onIndexChange = { index: Int -> onEvent(RadarEvent.OnIndexChanged(index = index)) },
            modifier = modifier
        )
    }
}

private fun dockEnterTransition(isMotionEnabled: Boolean): EnterTransition =
    if (isMotionEnabled) {
        fadeIn(animationSpec = tween(durationMillis = DOCK_FADE_MILLISECONDS))
    } else {
        EnterTransition.None
    }

private fun dockTransition(isMotionEnabled: Boolean): ContentTransform {
    if (!isMotionEnabled) {
        return EnterTransition.None togetherWith ExitTransition.None
    }
    return ContentTransform(
        targetContentEnter = slideInVertically(initialOffsetY = { height: Int -> height }),
        initialContentExit = slideOutVertically(targetOffsetY = { height: Int -> height }),
        sizeTransform = SizeTransform(clip = false)
    )
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

// A figure let go over the radar card would land out of sight, so it is not dropped.
private fun droppedPegmanLocation(
    landing: Offset?,
    mapProjection: MapProjection?,
    visibleMapBottom: Int
): Location? {
    if (landing == null || mapProjection == null || landing.y > visibleMapBottom) {
        return null
    }
    return mapProjection.location(screenOffset = landing)
}

private fun WindAtLocation.windSpeedText(unit: String): String = "${windSpeed.toDouble().toOneDecimalText()} $unit"