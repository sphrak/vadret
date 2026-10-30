package se.vadretapp.vadret.radar.pollen

import androidx.compose.ui.graphics.ImageBitmap
import se.vadretapp.vadret.radar.model.PollenMarker

internal interface PollenCloudRenderer {
    suspend fun render(pollenMarkers: List<PollenMarker>): ImageBitmap
}