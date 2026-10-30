package se.vadretapp.vadret.radar.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.collections.immutable.ImmutableList
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.radar.model.PollenMarker
import se.vadretapp.vadret.radar.model.RadarLayer
import se.vadretapp.vadret.radar.precipitation.PrecipitationOverlay
import se.vadretapp.vadret.radar.wind.WindAtLocation
import se.vadretapp.vadret.radar.wind.WindField

@Composable
internal expect fun RadarMap(
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
    modifier: Modifier = Modifier
)