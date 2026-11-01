package se.vadretapp.vadret.radar.wind

import se.vadretapp.vadret.wind.model.WindFrame
import se.vadretapp.vadret.wind.model.WindGrid

internal interface WindFieldRenderer {

    suspend fun render(
        windGrid: WindGrid,
        windFrame: WindFrame,
        windPalette: WindPalette
    ): WindField

    suspend fun blend(
        windField: WindField,
        nextWindField: WindField,
        progress: Float
    ): WindField
}