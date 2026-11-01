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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.jetbrains.compose.resources.stringResource
import org.maplibre.compose.camera.CameraFit
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.expressions.dsl.Feature
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.eq
import org.maplibre.compose.expressions.value.GeometryType
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.layers.Anchor
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.layers.LayerHandle
import org.maplibre.compose.layers.LayerSummary
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.MutableLayerHandle
import org.maplibre.compose.map.CameraConstraints
import org.maplibre.compose.map.MapState
import org.maplibre.compose.map.MapStyleState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.StyleLoadState
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonSource
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.DpPadding
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.openstreetmap_copyright_notice
import se.vadretapp.vadret.forecastwarning.model.MapBounds
import se.vadretapp.vadret.forecastwarning.model.WarningMapArea
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.WarningColors

private const val FILL_LAYER_ID = "vadret-warning-fill"
private const val BAND_LAYER_ID = "vadret-warning-band"
private const val LINE_LAYER_ID = "vadret-warning-line"
private const val MARKER_LAYER_ID = "vadret-warning-marker"
private const val SYMBOL_LAYER_TYPE = "symbol"
private const val TEXT_COLOR_PROPERTY = "text-color"
private const val BACKGROUND_COLOR_PROPERTY = "background-color"
private const val FILL_COLOR_PROPERTY = "fill-color"
private const val LINE_COLOR_PROPERTY = "line-color"
private const val BACKGROUND_LAYER_ID = "background"
private const val WATER_LAYER_ID = "water"
private const val WATERWAY_LAYER_ID = "waterway"
private const val ICON_IMAGE_PROPERTY = "icon-image"
private const val MISSING_SPRITE_ICON = "circle-11"
private const val SPRITE_ICON = "circle_11"
private const val HEX_COLOR_PREFIX = "#"
private const val RGB_MASK = 0xFFFFFF
private const val HEX_RADIX = 16
private const val RGB_HEX_LENGTH = 6
private const val HEX_PADDING = '0'
private const val FILL_OPACITY = 0.75f
private const val BAND_OPACITY = 0.9f
private const val MAXIMUM_ZOOM_LEVEL = 9.0
private const val ZOOM_OUT_LEVELS = 0.5
private const val MAXIMUM_FRAMED_ZOOM_LEVEL = 8.0
private const val TOWN_LABEL_ZOOM_LEVEL = 7.0
private const val OVERVIEW_ZOOM_LEVEL = 3.5
private const val FLAT_PITCH = 0.0
private const val OVERVIEW_LATITUDE = 62.5
private const val OVERVIEW_LONGITUDE = 16.5
private const val ATTRIBUTION_BACKGROUND_ALPHA = 0.85f
private const val FADE_IN_MILLISECONDS = 400
private const val HIDDEN = 0f
private const val OPAQUE = 1f

private val lineWidth = 2.5.dp
private val bandWidth = 10.dp
private val markerRadius = 8.dp
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
    val isDark: Boolean = VadretTheme.isDark
    val baseStyle: BaseStyle = remember(isDark) {
        BaseStyle.Uri(
            uri = if (isDark) {
                "https://tiles.openfreemap.org/styles/dark"
            } else {
                "https://tiles.openfreemap.org/styles/positron"
            }
        )
    }
    val mapState: MapState = rememberMapState(
        baseStyle = baseStyle,
        initialCameraPosition = CameraPosition(
            center = Position(
                longitude = OVERVIEW_LONGITUDE,
                latitude = OVERVIEW_LATITUDE
            ),
            zoom = OVERVIEW_ZOOM_LEVEL
        )
    ) {
        val geoJson: String? = mapArea.geoJson
        if (geoJson != null) {
            key(geoJson) {
                WarningAreaLayers(
                    geoJson = geoJson,
                    colors = colors
                )
            }
        }
    }
    val fitPadding: Dp = VadretTheme.spacing.extraLarge
    var isFramed: Boolean by remember { mutableStateOf(value = false) }
    val coverAlpha: Float by animateFloatAsState(
        targetValue = if (isFramed) {
            HIDDEN
        } else {
            OPAQUE
        },
        animationSpec = tween(durationMillis = FADE_IN_MILLISECONDS)
    )
    val darkLabelColor: String = VadretTheme.colorScheme.onSurface.toHexColor()
    val darkLandColor: String = VadretTheme.colorScheme.surfaceContainerHighest.toHexColor()
    val darkWaterColor: String = VadretTheme.colorScheme.background.toHexColor()
    LaunchedEffect(mapArea.bounds, fitTopPadding, isDark) {
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
            position = if (mapArea.geoJson != null) {
                framedCamera.copy(zoom = framedCamera.zoom.zoomedOut())
            } else {
                framedCamera
            }
        )
        val styleLoadState: StyleLoadState = snapshotFlow { mapState.style.loadState }.first { styleLoadState: StyleLoadState ->
            styleLoadState == StyleLoadState.Ready || styleLoadState is StyleLoadState.Failed
        }
        if (isDark && styleLoadState == StyleLoadState.Ready) {
            mapState.style.restyleDarkLabels(color = darkLabelColor)
            mapState.style.restyleDarkGround(
                landColor = darkLandColor,
                waterColor = darkWaterColor
            )
        }
        isFramed = true
    }
    Box(modifier = modifier) {
        MaplibreMap(
            state = mapState,
            cameraConstraints = warningMapConstraints,
            interactions = MapInteractions.None,
            overlay = {},
            modifier = Modifier.fillMaxSize()
        )
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

@Composable
@MaplibreComposable
private fun WarningAreaLayers(
    geoJson: String,
    colors: WarningColors
) {
    val source: GeoJsonSource = rememberGeoJsonSource(data = GeoJsonData.JsonString(json = geoJson))
    Anchor.Above(predicate = { layerSummary: LayerSummary -> layerSummary.type != SYMBOL_LAYER_TYPE }) {
        FillLayer(
            id = FILL_LAYER_ID,
            source = source,
            filter = Feature.geometryType() eq const(GeometryType.Polygon),
            color = const(colors.container),
            opacity = const(FILL_OPACITY)
        )
        LineLayer(
            id = BAND_LAYER_ID,
            source = source,
            filter = Feature.geometryType() eq const(GeometryType.LineString),
            color = const(colors.container),
            opacity = const(BAND_OPACITY),
            width = const(bandWidth),
            cap = const(LineCap.Round),
            join = const(LineJoin.Round)
        )
        LineLayer(
            id = LINE_LAYER_ID,
            source = source,
            color = const(colors.content),
            width = const(lineWidth),
            cap = const(LineCap.Round),
            join = const(LineJoin.Round)
        )
        CircleLayer(
            id = MARKER_LAYER_ID,
            source = source,
            filter = Feature.geometryType() eq const(GeometryType.Point),
            color = const(colors.container),
            radius = const(markerRadius),
            strokeColor = const(colors.content),
            strokeWidth = const(lineWidth)
        )
    }
}

private suspend fun MapStyleState.restyleDarkLabels(color: String) {
    layers
        .filter { layerHandle: LayerHandle -> layerHandle.type == SYMBOL_LAYER_TYPE }
        .forEach { layerHandle: LayerHandle ->
            val mutableLayerHandle: MutableLayerHandle? = layerHandle.asMutable
            if (mutableLayerHandle != null) {
                mutableLayerHandle.setPaintProperty(
                    name = TEXT_COLOR_PROPERTY,
                    value = JsonPrimitive(value = color)
                )
                val iconImage: String? = layerHandle.getProperty(name = ICON_IMAGE_PROPERTY)?.toString()
                if (iconImage != null && iconImage.contains(other = MISSING_SPRITE_ICON)) {
                    mutableLayerHandle.setLayoutProperty(
                        name = ICON_IMAGE_PROPERTY,
                        value = Json.parseToJsonElement(
                            string = iconImage.replace(
                                oldValue = MISSING_SPRITE_ICON,
                                newValue = SPRITE_ICON
                            )
                        )
                    )
                }
            }
        }
}

private suspend fun MapStyleState.restyleDarkGround(
    landColor: String,
    waterColor: String
) {
    layers[BACKGROUND_LAYER_ID]?.asMutable?.setPaintProperty(
        name = BACKGROUND_COLOR_PROPERTY,
        value = JsonPrimitive(value = landColor)
    )
    layers[WATER_LAYER_ID]?.asMutable?.setPaintProperty(
        name = FILL_COLOR_PROPERTY,
        value = JsonPrimitive(value = waterColor)
    )
    layers[WATERWAY_LAYER_ID]?.asMutable?.setPaintProperty(
        name = LINE_COLOR_PROPERTY,
        value = JsonPrimitive(value = waterColor)
    )
}

private fun Double.zoomedOut(): Double =
    (this - ZOOM_OUT_LEVELS)
        .coerceAtLeast(minimumValue = coerceAtMost(maximumValue = TOWN_LABEL_ZOOM_LEVEL))
        .coerceAtMost(maximumValue = MAXIMUM_FRAMED_ZOOM_LEVEL)

private fun Color.toHexColor(): String =
    HEX_COLOR_PREFIX + (toArgb() and RGB_MASK).toString(radix = HEX_RADIX).padStart(
        length = RGB_HEX_LENGTH,
        padChar = HEX_PADDING
    )

private fun MapBounds.toBoundingBox(): BoundingBox =
    BoundingBox(
        west = west,
        south = south,
        east = east,
        north = north
    )