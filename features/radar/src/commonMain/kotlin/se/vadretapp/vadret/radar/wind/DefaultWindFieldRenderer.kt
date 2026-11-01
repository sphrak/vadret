package se.vadretapp.vadret.radar.wind

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import se.vadretapp.vadret.radar.extension.towards
import se.vadretapp.vadret.radar.image.argbImageBitmap
import se.vadretapp.vadret.wind.model.WindFrame
import se.vadretapp.vadret.wind.model.WindGrid
import kotlin.concurrent.atomics.AtomicReference

private const val RASTER_WIDTH = 240

private class PreparedRaster(
    val windGrid: WindGrid,
    val windRaster: WindRaster
)

internal class DefaultWindFieldRenderer(
    private val coroutineDispatcher: CoroutineDispatcher
) : WindFieldRenderer {

    private val preparedRaster: AtomicReference<PreparedRaster?> = AtomicReference(value = null)

    override suspend fun render(
        windGrid: WindGrid,
        windFrame: WindFrame,
        windPalette: WindPalette
    ): WindField =
        withContext(context = coroutineDispatcher) {
            windRaster(windGrid = windGrid)
                .render(
                    windFrame = windFrame,
                    windPalette = windPalette
                ).toWindField()
        }

    override suspend fun blend(
        windField: WindField,
        nextWindField: WindField,
        progress: Float
    ): WindField =
        withContext(context = coroutineDispatcher) {
            windField.values
                .towards(
                    nextWindFieldValues = nextWindField.values,
                    progress = progress
                ).toWindField()
        }

    private fun WindFieldValues.toWindField(): WindField =
        WindField(
            values = this,
            image = argbImageBitmap(
                pixels = colors,
                width = width,
                height = height
            )
        )

    private fun windRaster(windGrid: WindGrid): WindRaster {
        val current: PreparedRaster? = preparedRaster.load()
        return if (current != null && current.windGrid == windGrid) {
            current.windRaster
        } else {
            val windRaster: WindRaster = windGrid.toWindRaster(width = RASTER_WIDTH)
            preparedRaster.store(
                newValue = PreparedRaster(
                    windGrid = windGrid,
                    windRaster = windRaster
                )
            )
            windRaster
        }
    }
}