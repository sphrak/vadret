package se.vadretapp.vadret.radar

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.radar.model.PollenMarker
import se.vadretapp.vadret.radar.model.RadarFrame
import se.vadretapp.vadret.radar.model.RadarLayer
import se.vadretapp.vadret.radar.model.WindTimeFrame
import se.vadretapp.vadret.radar.precipitation.PrecipitationOverlay
import se.vadretapp.vadret.radar.wind.WindField

internal val layersWithPollen: ImmutableList<RadarLayer> = RadarLayer.entries.toImmutableList()
internal val layersWithoutPollen: ImmutableList<RadarLayer> = persistentListOf(
    RadarLayer.PRECIPITATION,
    RadarLayer.WIND
)

internal data class RadarState(
    val layer: RadarLayer = RadarLayer.PRECIPITATION,
    val layers: ImmutableList<RadarLayer> = layersWithoutPollen,
    val isPollenHibernationNoticeVisible: Boolean = false,
    val precipitationFrames: ImmutableList<RadarFrame> = persistentListOf(),
    val precipitationIndex: Int = 0,
    val precipitationOverlay: PrecipitationOverlay? = null,
    val windFrames: ImmutableList<WindTimeFrame> = persistentListOf(),
    val windIndex: Int = 0,
    val windField: WindField? = null,
    val windTransitionField: WindField? = null,
    val deviceLocation: Location? = null,
    val droppedLocation: Location? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = true,
    val isMapLoaded: Boolean = false,
    val isPrecipitationUnavailable: Boolean = false,
    val areRadarStationsShown: Boolean = false,
    val pollenTypes: ImmutableList<PollenType> = persistentListOf(),
    val selectedPollenId: String? = null,
    val pollenMarkers: ImmutableList<PollenMarker> = persistentListOf(),
    val pollenClouds: ImageBitmap? = null,
    val pollenDayText: String? = null,
    val pollenDateText: String? = null,
    val errorMessageResource: StringResource? = null
) {

    val layerIndex: Int = layers.indexOf(element = layer)

    val isInitialLoading: Boolean = !isMapLoaded || (layer == RadarLayer.PRECIPITATION && precipitationOverlay == null && !isPrecipitationUnavailable)

    val shownWindField: WindField? = windTransitionField ?: windField
}