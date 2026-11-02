package se.vadretapp.vadret.radar.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.map.CameraConstraints
import org.maplibre.compose.map.MapState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.StyleLoadState
import org.maplibre.compose.util.PreparedImage
import org.maplibre.spatialk.geojson.BoundingBox
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.radar.model.PollenMarker
import se.vadretapp.vadret.radar.model.RadarLayer
import se.vadretapp.vadret.radar.precipitation.PrecipitationOverlay
import se.vadretapp.vadret.radar.ui.map.RadarCameraLimits
import se.vadretapp.vadret.radar.ui.map.RadarMapLayers
import se.vadretapp.vadret.radar.ui.map.RetainedRadarMap
import se.vadretapp.vadret.radar.ui.map.radarCameraLimits
import se.vadretapp.vadret.radar.ui.overlay.CameraScreenProjection
import se.vadretapp.vadret.radar.ui.overlay.MapProjection
import se.vadretapp.vadret.radar.ui.overlay.RadarOverlay
import se.vadretapp.vadret.radar.ui.overlay.rememberWindOpacity
import se.vadretapp.vadret.radar.wind.WindAtLocation
import se.vadretapp.vadret.radar.wind.WindField
import kotlin.math.roundToInt

private const val MAXIMUM_ZOOM_LEVEL = 12.0
private const val NO_PITCH = 0.0
private const val ZOOM_STEPS_PER_LEVEL = 100.0

private val radarInteractions = MapInteractions {
    camera {
        rotate { enabled = false }
        pitch { enabled = false }
    }
}

@Composable
internal actual fun RadarMap(
    layer: RadarLayer,
    precipitationOverlay: PrecipitationOverlay?,
    windField: WindField?,
    deviceLocation: Location?,
    windAtLocation: WindAtLocation?,
    windSpeedText: String?,
    areRadarStationsShown: Boolean,
    pollenMarkers: ImmutableList<PollenMarker>,
    pollenClouds: ImageBitmap?,
    obscuredBottom: Int,
    onMapLoaded: () -> Unit,
    onMapProjectionChanged: (MapProjection) -> Unit,
    modifier: Modifier
) {
    val retainedRadarMap: RetainedRadarMap = LocalRetainedRadarMap.current
    val mapState: MapState = retainedRadarMap.mapState
    val currentOnMapLoaded: () -> Unit by rememberUpdatedState(newValue = onMapLoaded)
    val coloredPrecipitation: PreparedImage? = rememberPreparedImage(image = (precipitationOverlay as? PrecipitationOverlay.Colored)?.image)
    val windImage: PreparedImage? = rememberPreparedImage(image = windField?.image)
    val pollenImage: PreparedImage? = rememberPreparedImage(image = pollenClouds)
    val windOpacity: Float = rememberWindOpacity(isWindShown = layer == RadarLayer.WIND && windField != null)
    SideEffect {
        retainedRadarMap.radarMapLayers.value = RadarMapLayers(
            layer = layer,
            coloredPrecipitation = coloredPrecipitation,
            windImage = windImage,
            windOpacity = windOpacity,
            pollenImage = pollenImage
        )
    }
    val styleLoadState: StyleLoadState = mapState.style.loadState
    LaunchedEffect(styleLoadState) {
        if (styleLoadState == StyleLoadState.Ready || styleLoadState is StyleLoadState.Failed) {
            currentOnMapLoaded()
        }
    }
    var viewportSize: IntSize by remember { mutableStateOf(value = IntSize.Zero) }
    val density: Density = LocalDensity.current
    val currentOnMapProjectionChanged: (MapProjection) -> Unit by rememberUpdatedState(newValue = onMapProjectionChanged)
    val mapProjection: MapProjection = remember(mapState, density) {
        CameraMapProjection {
            cameraScreenProjection(
                cameraPosition = mapState.cameraPosition,
                viewportSize = viewportSize,
                density = density
            )
        }
    }
    LaunchedEffect(mapProjection) {
        currentOnMapProjectionChanged(mapProjection)
    }
    val steppedZoom: Double by remember(mapState) {
        derivedStateOf { (mapState.cameraPosition.zoom * ZOOM_STEPS_PER_LEVEL).roundToInt() / ZOOM_STEPS_PER_LEVEL }
    }
    val cameraConstraints: CameraConstraints = remember(viewportSize, obscuredBottom, steppedZoom, density) {
        radarCameraConstraints(
            viewportSize = viewportSize,
            obscuredBottom = obscuredBottom,
            zoom = steppedZoom,
            density = density
        )
    }
    Box(modifier = modifier.onSizeChanged { size: IntSize -> viewportSize = size }) {
        MaplibreMap(
            state = mapState,
            cameraConstraints = cameraConstraints,
            interactions = radarInteractions,
            overlay = {},
            modifier = Modifier.fillMaxSize()
        )
        RadarOverlay(
            screenProjection = {
                cameraScreenProjection(
                    cameraPosition = mapState.cameraPosition,
                    viewportSize = viewportSize,
                    density = density
                )
            },
            layer = layer,
            precipitationOverlay = precipitationOverlay,
            windField = windField,
            windOpacity = windOpacity,
            deviceLocation = deviceLocation,
            windAtLocation = windAtLocation,
            windSpeedText = windSpeedText,
            areRadarStationsShown = areRadarStationsShown,
            pollenMarkers = pollenMarkers,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// Reads the camera on every call, so what is placed with it follows the map.
private class CameraMapProjection(
    private val cameraScreenProjection: () -> CameraScreenProjection
) : MapProjection {

    override fun screenOffset(
        latitude: Double,
        longitude: Double
    ): Offset =
        cameraScreenProjection().screenOffset(
            latitude = latitude,
            longitude = longitude
        )

    override fun location(screenOffset: Offset): Location = cameraScreenProjection().location(screenOffset = screenOffset)
}

private fun cameraScreenProjection(
    cameraPosition: CameraPosition,
    viewportSize: IntSize,
    density: Density
): CameraScreenProjection =
    CameraScreenProjection(
        centerLatitude = cameraPosition.center.latitude,
        centerLongitude = cameraPosition.center.longitude,
        zoom = cameraPosition.zoom,
        viewportWidth = viewportSize.width.toFloat(),
        viewportHeight = viewportSize.height.toFloat(),
        density = density.density
    )

@Composable
private fun rememberPreparedImage(image: ImageBitmap?): PreparedImage? {
    val preparedImage: PreparedImage? by produceState<PreparedImage?>(initialValue = null, image) {
        value = if (image != null) {
            withContext(context = Dispatchers.Default) { PreparedImage.fromBitmap(bitmap = image) }
        } else {
            null
        }
    }
    return preparedImage
}

private fun radarCameraConstraints(
    viewportSize: IntSize,
    obscuredBottom: Int,
    zoom: Double,
    density: Density
): CameraConstraints {
    if (viewportSize == IntSize.Zero) {
        return CameraConstraints {
            maxZoom = MAXIMUM_ZOOM_LEVEL
            maxPitch = NO_PITCH
        }
    }
    val radarCameraLimits: RadarCameraLimits = radarCameraLimits(
        viewportWidth = (viewportSize.width / density.density).toDouble(),
        viewportHeight = (viewportSize.height / density.density).toDouble(),
        obscuredBottom = (obscuredBottom / density.density).toDouble(),
        zoom = zoom
    )
    return CameraConstraints {
        minZoom = radarCameraLimits.minimumZoom
        maxZoom = MAXIMUM_ZOOM_LEVEL
        maxPitch = NO_PITCH
        boundingBox = BoundingBox(
            west = radarCameraLimits.westLongitude,
            south = radarCameraLimits.southLatitude,
            east = radarCameraLimits.eastLongitude,
            north = radarCameraLimits.northLatitude
        )
    }
}