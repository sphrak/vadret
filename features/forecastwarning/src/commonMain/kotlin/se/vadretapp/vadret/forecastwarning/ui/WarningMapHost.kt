package se.vadretapp.vadret.forecastwarning.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.expressions.dsl.Feature
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.eq
import org.maplibre.compose.expressions.value.GeometryType
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.layers.Anchor
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.layers.LayerHandle
import org.maplibre.compose.layers.LayerSummary
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.MutableLayerHandle
import org.maplibre.compose.map.MapState
import org.maplibre.compose.map.MapStyleState
import org.maplibre.compose.map.StyleLoadState
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonSource
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.Position
import se.vadretapp.vadret.forecastwarning.ui.map.RetainedWarningMap
import se.vadretapp.vadret.forecastwarning.ui.map.WarningMapHolder
import se.vadretapp.vadret.forecastwarning.ui.map.WarningMapLayers
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.WarningColors

private const val EXACT_TOLERANCE = 0f
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
private const val OVERVIEW_ZOOM_LEVEL = 3.5
private const val OVERVIEW_LATITUDE = 62.5
private const val OVERVIEW_LONGITUDE = 16.5

private val lineWidth = 2.5.dp
private val bandWidth = 10.dp
private val markerRadius = 8.dp

internal val LocalWarningMapHolder: ProvidableCompositionLocal<WarningMapHolder> = staticCompositionLocalOf {
    error("WarningAreaMap must be placed inside WarningMapHost")
}

// The map outlives each warning, so opening the next one frames a map that is already loaded and styled.
@Composable
fun WarningMapHost(content: @Composable () -> Unit) {
    val warningMapHolder: WarningMapHolder = remember { WarningMapHolder() }
    if (warningMapHolder.isRequested) {
        RetainWarningMap(warningMapHolder = warningMapHolder)
    }
    CompositionLocalProvider(
        value = LocalWarningMapHolder provides warningMapHolder,
        content = content
    )
}

@Composable
private fun RetainWarningMap(warningMapHolder: WarningMapHolder) {
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
    val warningMapLayers: MutableState<WarningMapLayers?> = remember { mutableStateOf(value = null) }
    val isStyled: MutableState<Boolean> = remember { mutableStateOf(value = false) }
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
        val shownLayers: WarningMapLayers? = warningMapLayers.value
        if (shownLayers != null) {
            key(shownLayers.geoJson) {
                WarningAreaLayers(
                    geoJson = shownLayers.geoJson,
                    colors = shownLayers.colors
                )
            }
        }
    }
    val darkLabelColor: String = VadretTheme.colorScheme.onSurface.toHexColor()
    val darkLandColor: String = VadretTheme.colorScheme.surfaceContainerHighest.toHexColor()
    val darkWaterColor: String = VadretTheme.colorScheme.background.toHexColor()
    val styleLoadState: StyleLoadState = mapState.style.loadState
    LaunchedEffect(styleLoadState, isDark, darkLabelColor, darkLandColor, darkWaterColor) {
        if (isDark && styleLoadState == StyleLoadState.Ready) {
            mapState.style.restyleDarkLabels(color = darkLabelColor)
            mapState.style.restyleDarkGround(
                landColor = darkLandColor,
                waterColor = darkWaterColor
            )
        }
        isStyled.value = styleLoadState == StyleLoadState.Ready || styleLoadState is StyleLoadState.Failed
    }
    val retainedWarningMap: RetainedWarningMap = remember(mapState) {
        RetainedWarningMap(
            mapState = mapState,
            warningMapLayers = warningMapLayers,
            isStyled = isStyled
        )
    }
    SideEffect {
        warningMapHolder.retainedWarningMap = retainedWarningMap
    }
}

@Composable
@MaplibreComposable
private fun WarningAreaLayers(
    geoJson: String,
    colors: WarningColors
) {
    val source: GeoJsonSource = rememberGeoJsonSource(
        data = GeoJsonData.JsonString(json = geoJson),
        block = { tolerance = EXACT_TOLERANCE }
    )
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

private fun Color.toHexColor(): String =
    HEX_COLOR_PREFIX + (toArgb() and RGB_MASK).toString(radix = HEX_RADIX).padStart(
        length = RGB_HEX_LENGTH,
        padChar = HEX_PADDING
    )