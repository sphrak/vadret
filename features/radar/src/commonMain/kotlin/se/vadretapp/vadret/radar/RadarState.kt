package se.vadretapp.vadret.radar

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.radar.model.RadarFrame
import se.vadretapp.vadret.radar.model.RadarLayer
import se.vadretapp.vadret.radar.model.WindTimeFrame
import se.vadretapp.vadret.radar.precipitation.PrecipitationOverlay
import se.vadretapp.vadret.radar.wind.WindField

internal data class RadarState(
    val layer: RadarLayer = RadarLayer.PRECIPITATION,
    val precipitationFrames: ImmutableList<RadarFrame> = persistentListOf(),
    val precipitationIndex: Int = 0,
    val precipitationOverlay: PrecipitationOverlay? = null,
    val windFrames: ImmutableList<WindTimeFrame> = persistentListOf(),
    val windIndex: Int = 0,
    val windField: WindField? = null,
    val windTransitionField: WindField? = null,
    val deviceLocation: Location? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = true,
    val isMapLoaded: Boolean = false,
    val isPrecipitationUnavailable: Boolean = false,
    val areRadarStationsShown: Boolean = false,
    val errorMessageResource: StringResource? = null
) {

    val isInitialLoading: Boolean = !isMapLoaded || (layer == RadarLayer.PRECIPITATION && precipitationOverlay == null && !isPrecipitationUnavailable)

    val shownWindField: WindField? = windTransitionField ?: windField
}