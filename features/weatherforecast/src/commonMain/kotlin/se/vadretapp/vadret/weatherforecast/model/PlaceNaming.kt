package se.vadretapp.vadret.weatherforecast.model

data class PlaceNaming(
    val label: String = "",
    val isLabelMissing: Boolean = false,
    val takenPlaceName: String? = null
)