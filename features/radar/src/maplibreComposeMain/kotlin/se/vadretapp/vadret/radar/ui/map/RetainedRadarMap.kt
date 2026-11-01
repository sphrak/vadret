package se.vadretapp.vadret.radar.ui.map

import androidx.compose.runtime.MutableState
import org.maplibre.compose.map.MapState

internal class RetainedRadarMap(
    val mapState: MapState,
    val radarMapLayers: MutableState<RadarMapLayers?>
)