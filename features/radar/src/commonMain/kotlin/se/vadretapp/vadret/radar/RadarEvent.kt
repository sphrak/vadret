package se.vadretapp.vadret.radar

import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.radar.model.RadarLayer
import se.vadretapp.vadret.setting.model.HourFormat

internal sealed interface RadarEvent {
    data class OnStart(val initialImageUrl: String?) : RadarEvent
    data object OnLeft : RadarEvent
    data object OnMapLoaded : RadarEvent
    data class OnLocaleChanged(
        val languageTag: String,
        val hourFormat: HourFormat
    ) : RadarEvent
    data class OnDarkThemeChanged(val isDarkTheme: Boolean) : RadarEvent
    data object OnPlayButtonClicked : RadarEvent
    data object OnErrorShown : RadarEvent
    data class OnIndexChanged(val index: Int) : RadarEvent
    data class OnLayerSelected(val layer: RadarLayer) : RadarEvent
    data object OnRadarStationsToggled : RadarEvent
    data class OnPollenTypeSelected(val pollenId: String) : RadarEvent
    data object OnPollenHibernationNoticeDismissed : RadarEvent
    data class OnPegmanDropped(val location: Location) : RadarEvent
    data object OnPlaceIslandDismissed : RadarEvent
}