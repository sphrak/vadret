package se.vadretapp.vadret.radar.wind

internal const val OUTSIDE_FIELD: Int = -1

internal class WindFieldValues(
    val width: Int,
    val height: Int,
    val eastward: FloatArray,
    val northward: FloatArray,
    val speeds: FloatArray,
    val colors: IntArray
) {

    fun pixelAt(
        x: Float,
        y: Float
    ): Int {
        val column: Int = x.toInt()
        val row: Int = y.toInt()
        return if (x < 0f || y < 0f || column >= width || row >= height) {
            OUTSIDE_FIELD
        } else {
            row * width + column
        }
    }

    fun valueAt(
        values: FloatArray,
        pixel: Int
    ): Float =
        if (pixel == OUTSIDE_FIELD) {
            Float.NaN
        } else {
            values[pixel]
        }
}