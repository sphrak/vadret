package se.vadretapp.vadret.radar.pollen

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import se.vadretapp.vadret.radar.image.argbImageBitmap
import se.vadretapp.vadret.radar.model.PollenMarker
import se.vadretapp.vadret.radar.wind.radarRasterHeight

private const val RASTER_WIDTH = 240

internal class DefaultPollenCloudRenderer(
    private val coroutineDispatcher: CoroutineDispatcher
) : PollenCloudRenderer {

    override suspend fun render(pollenMarkers: List<PollenMarker>): ImageBitmap =
        withContext(context = coroutineDispatcher) {
            val height: Int = radarRasterHeight(width = RASTER_WIDTH)
            argbImageBitmap(
                pixels = pollenCloudPixels(
                    pollenMarkers = pollenMarkers,
                    width = RASTER_WIDTH,
                    height = height
                ),
                width = RASTER_WIDTH,
                height = height
            )
        }
}