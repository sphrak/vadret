package se.vadretapp.vadret.forecastwarning.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.stringResource
import org.maplibre.compose.camera.CameraFit
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.map.CameraConstraints
import org.maplibre.compose.map.MapState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.util.DpPadding
import org.maplibre.spatialk.geojson.BoundingBox
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.openstreetmap_copyright_notice
import se.vadretapp.vadret.forecastwarning.model.MapBounds
import se.vadretapp.vadret.forecastwarning.model.WarningMapArea
import se.vadretapp.vadret.forecastwarning.ui.map.RetainedWarningMap
import se.vadretapp.vadret.forecastwarning.ui.map.WarningMapHolder
import se.vadretapp.vadret.forecastwarning.ui.map.WarningMapLayers
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.WarningColors

private const val MAXIMUM_ZOOM_LEVEL = 9.0
private const val ZOOM_OUT_LEVELS = 0.5
private const val MAXIMUM_FRAMED_ZOOM_LEVEL = 8.0
private const val TOWN_LABEL_ZOOM_LEVEL = 7.0
private const val FLAT_PITCH = 0.0
private const val ATTRIBUTION_BACKGROUND_ALPHA = 0.85f
private const val FADE_IN_MILLISECONDS = 400
private const val HIDDEN = 0f
private const val OPAQUE = 1f

private val attributionShape = RoundedCornerShape(size = 4.dp)
private val attributionPadding = PaddingValues(
    horizontal = 6.dp,
    vertical = 2.dp
)

private val warningMapConstraints = CameraConstraints {
    maxZoom = MAXIMUM_ZOOM_LEVEL
    maxPitch = FLAT_PITCH
}

@Composable
internal fun WarningAreaMap(
    mapArea: WarningMapArea,
    colors: WarningColors,
    fitTopPadding: Dp,
    modifier: Modifier = Modifier
) {
    if (LocalInspectionMode.current) {
        Box(modifier = modifier.background(color = VadretTheme.colorScheme.surfaceContainerHighest))
        return
    }
    val warningMapHolder: WarningMapHolder = LocalWarningMapHolder.current
    SideEffect {
        warningMapHolder.isRequested = true
    }
    val retainedWarningMap: RetainedWarningMap? = warningMapHolder.retainedWarningMap
    var isFramed: Boolean by remember { mutableStateOf(value = false) }
    val coverAlpha: Float by animateFloatAsState(
        targetValue = if (isFramed) {
            HIDDEN
        } else {
            OPAQUE
        },
        animationSpec = tween(durationMillis = FADE_IN_MILLISECONDS)
    )
    Box(modifier = modifier) {
        if (retainedWarningMap != null) {
            FramedWarningMap(
                retainedWarningMap = retainedWarningMap,
                mapArea = mapArea,
                colors = colors,
                fitTopPadding = fitTopPadding,
                onFramed = { isFramed = true },
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = coverAlpha }
                .background(color = VadretTheme.colorScheme.surfaceContainerHighest)
        )
        Text(
            text = stringResource(resource = Res.string.openstreetmap_copyright_notice),
            style = VadretTheme.textStyles.attribution,
            color = VadretTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(alignment = Alignment.BottomEnd)
                .padding(all = VadretTheme.spacing.extraSmall)
                .background(
                    color = VadretTheme.colorScheme.surface.copy(alpha = ATTRIBUTION_BACKGROUND_ALPHA),
                    shape = attributionShape
                ).padding(paddingValues = attributionPadding)
        )
    }
}

// The kept map still shows the last warning until this one is framed and styled, so the cover only lifts after that.
@Composable
private fun FramedWarningMap(
    retainedWarningMap: RetainedWarningMap,
    mapArea: WarningMapArea,
    colors: WarningColors,
    fitTopPadding: Dp,
    onFramed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mapState: MapState = retainedWarningMap.mapState
    val currentOnFramed: () -> Unit by rememberUpdatedState(newValue = onFramed)
    val geoJson: String? = mapArea.geoJson
    SideEffect {
        retainedWarningMap.warningMapLayers.value = if (geoJson != null) {
            WarningMapLayers(
                geoJson = geoJson,
                colors = colors
            )
        } else {
            null
        }
    }
    val fitPadding: Dp = VadretTheme.spacing.extraLarge
    LaunchedEffect(mapArea.bounds, fitTopPadding) {
        val framedCamera: CameraPosition = mapState.cameraForBounds(
            boundingBox = mapArea.bounds.toBoundingBox(),
            fit = CameraFit(
                fitPadding = DpPadding(
                    left = fitPadding,
                    top = fitTopPadding,
                    right = fitPadding,
                    bottom = fitPadding
                )
            )
        )
        mapState.setCameraPosition(
            position = if (geoJson != null) {
                framedCamera.copy(zoom = framedCamera.zoom.zoomedOut())
            } else {
                framedCamera
            }
        )
        snapshotFlow { retainedWarningMap.isStyled.value }.first { isStyled: Boolean -> isStyled }
        currentOnFramed()
    }
    MaplibreMap(
        state = mapState,
        cameraConstraints = warningMapConstraints,
        interactions = MapInteractions.None,
        overlay = {},
        modifier = modifier
    )
}

private fun Double.zoomedOut(): Double =
    (this - ZOOM_OUT_LEVELS)
        .coerceAtLeast(minimumValue = coerceAtMost(maximumValue = TOWN_LABEL_ZOOM_LEVEL))
        .coerceAtMost(maximumValue = MAXIMUM_FRAMED_ZOOM_LEVEL)

private fun MapBounds.toBoundingBox(): BoundingBox =
    BoundingBox(
        west = west,
        south = south,
        east = east,
        north = north
    )