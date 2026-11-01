package se.vadretapp.vadret.radar.ui.map

import se.vadretapp.vadret.radar.wind.RasterArea

internal class RasterProjection(
    private val originX: Float,
    private val originY: Float,
    private val scaleX: Float,
    private val scaleY: Float
) {

    val screenPixelsPerRasterPixel: Float = scaleX

    fun screenX(rasterX: Float): Float = originX + rasterX * scaleX

    fun screenY(rasterY: Float): Float = originY + rasterY * scaleY

    fun visibleArea(
        screenWidth: Int,
        screenHeight: Int,
        rasterWidth: Int,
        rasterHeight: Int
    ): RasterArea =
        RasterArea(
            left = ((0f - originX) / scaleX).coerceIn(minimumValue = 0f, maximumValue = rasterWidth.toFloat()),
            top = ((0f - originY) / scaleY).coerceIn(minimumValue = 0f, maximumValue = rasterHeight.toFloat()),
            right = ((screenWidth - originX) / scaleX).coerceIn(minimumValue = 0f, maximumValue = rasterWidth.toFloat()),
            bottom = ((screenHeight - originY) / scaleY).coerceIn(minimumValue = 0f, maximumValue = rasterHeight.toFloat())
        )
}