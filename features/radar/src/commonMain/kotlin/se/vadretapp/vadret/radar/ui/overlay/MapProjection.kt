package se.vadretapp.vadret.radar.ui.overlay

import androidx.compose.ui.geometry.Offset
import se.vadretapp.vadret.location.model.Location

internal interface MapProjection : ScreenProjection {
    fun location(screenOffset: Offset): Location?
}