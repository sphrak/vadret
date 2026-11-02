package se.vadretapp.vadret.forecastwarning.ui.map

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import org.maplibre.compose.map.MapState

internal class RetainedWarningMap(
    val mapState: MapState,
    val warningMapLayers: MutableState<WarningMapLayers?>,
    val isStyled: State<Boolean>
)