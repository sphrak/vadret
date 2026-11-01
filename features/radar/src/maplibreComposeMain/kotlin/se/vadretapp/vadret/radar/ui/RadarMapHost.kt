package se.vadretapp.vadret.radar.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.map.MapState
import org.maplibre.compose.map.StyleLoadState
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import se.vadretapp.vadret.radar.ui.map.RadarMapLayers
import se.vadretapp.vadret.radar.ui.map.RadarRasterLayers
import se.vadretapp.vadret.radar.ui.map.RetainedRadarMap
import se.vadretapp.vadret.radar.ui.map.initialRadarCamera
import se.vadretapp.vadret.radar.ui.map.restyle
import se.vadretapp.vadret.ui.LocalLanguageTag
import se.vadretapp.vadret.ui.VadretTheme

private const val LIGHT_STYLE_URI = "https://tiles.openfreemap.org/styles/positron"
private const val DARK_STYLE_URI = "https://tiles.openfreemap.org/styles/dark"
private const val LANGUAGE_SEPARATOR = '-'

internal val LocalRetainedRadarMap: ProvidableCompositionLocal<RetainedRadarMap> = staticCompositionLocalOf {
    error("RadarRoute must be placed inside RadarMapHost")
}

@Composable
fun RadarMapHost(content: @Composable () -> Unit) {
    val isDark: Boolean = VadretTheme.isDark
    val language: String = LocalLanguageTag.current.substringBefore(delimiter = LANGUAGE_SEPARATOR)
    val baseStyle: BaseStyle = remember(isDark) {
        BaseStyle.Uri(
            uri = if (isDark) {
                DARK_STYLE_URI
            } else {
                LIGHT_STYLE_URI
            }
        )
    }
    val radarMapLayers: MutableState<RadarMapLayers?> = remember { mutableStateOf(value = null) }
    val mapState: MapState = rememberMapState(
        baseStyle = baseStyle,
        initialCameraPosition = CameraPosition(
            center = Position(
                longitude = initialRadarCamera.longitude,
                latitude = initialRadarCamera.latitude
            ),
            zoom = initialRadarCamera.zoom
        )
    ) {
        val shownLayers: RadarMapLayers? = radarMapLayers.value
        if (shownLayers != null) {
            RadarRasterLayers(radarMapLayers = shownLayers)
        }
    }
    val styleLoadState: StyleLoadState = mapState.style.loadState
    LaunchedEffect(styleLoadState, language, isDark) {
        if (styleLoadState == StyleLoadState.Ready) {
            mapState.style.restyle(
                language = language,
                isDark = isDark
            )
        }
    }
    val retainedRadarMap: RetainedRadarMap = remember(mapState) {
        RetainedRadarMap(
            mapState = mapState,
            radarMapLayers = radarMapLayers
        )
    }
    CompositionLocalProvider(
        value = LocalRetainedRadarMap provides retainedRadarMap,
        content = content
    )
}