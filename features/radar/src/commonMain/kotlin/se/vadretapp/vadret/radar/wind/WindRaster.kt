package se.vadretapp.vadret.radar.wind

import se.vadretapp.vadret.radar.extension.toRadians
import se.vadretapp.vadret.radar.projection.RADAR_EAST_LONGITUDE
import se.vadretapp.vadret.radar.projection.RADAR_NORTH_LATITUDE
import se.vadretapp.vadret.radar.projection.RADAR_SOUTH_LATITUDE
import se.vadretapp.vadret.radar.projection.RADAR_WEST_LONGITUDE
import se.vadretapp.vadret.radar.projection.WebMercator
import se.vadretapp.vadret.wind.model.WindFrame
import se.vadretapp.vadret.wind.model.WindGrid
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val CORNER_COUNT = 3
private const val NO_POINT = -1
private const val TRANSPARENT = 0
private const val PIXEL_CENTER = 0.5f
private const val EDGE_TOLERANCE = -1e-4f

private val mercatorNorth: Double = WebMercator.y(latitude = RADAR_NORTH_LATITUDE)
private val mercatorSouth: Double = WebMercator.y(latitude = RADAR_SOUTH_LATITUDE)
private const val LONGITUDE_SPAN: Double = RADAR_EAST_LONGITUDE - RADAR_WEST_LONGITUDE

internal class WindRaster(
    val width: Int,
    val height: Int,
    private val pointIndices: IntArray,
    private val weights: FloatArray
) {

    fun render(
        windFrame: WindFrame,
        windPalette: WindPalette
    ): WindFieldValues {
        val pointCount: Int = windFrame.windSpeeds.size
        val pointSpeeds = FloatArray(size = pointCount) { index: Int -> windFrame.windSpeeds[index] }
        val pointEastward = FloatArray(size = pointCount)
        val pointNorthward = FloatArray(size = pointCount)
        for (index in 0 until pointCount) {
            val towardRadians: Double = windFrame.windFromDirections[index].toDouble().toRadians()
            pointEastward[index] = -pointSpeeds[index] * sin(towardRadians).toFloat()
            pointNorthward[index] = -pointSpeeds[index] * cos(towardRadians).toFloat()
        }
        val pixelCount: Int = width * height
        val speeds = FloatArray(size = pixelCount) { pixel: Int -> interpolate(values = pointSpeeds, pixel = pixel) }
        return WindFieldValues(
            width = width,
            height = height,
            eastward = FloatArray(size = pixelCount) { pixel: Int -> interpolate(values = pointEastward, pixel = pixel) },
            northward = FloatArray(size = pixelCount) { pixel: Int -> interpolate(values = pointNorthward, pixel = pixel) },
            speeds = speeds,
            colors = IntArray(size = pixelCount) { pixel: Int ->
                val speed: Float = speeds[pixel]
                if (speed.isNaN()) {
                    TRANSPARENT
                } else {
                    WindColorScale.argb(
                        windSpeed = speed,
                        windPalette = windPalette
                    )
                }
            }
        )
    }

    private fun interpolate(
        values: FloatArray,
        pixel: Int
    ): Float {
        val offset: Int = pixel * CORNER_COUNT
        if (pointIndices[offset] == NO_POINT) {
            return Float.NaN
        }
        var value = 0f
        for (corner in 0 until CORNER_COUNT) {
            value += values[pointIndices[offset + corner]] * weights[offset + corner]
        }
        return value
    }
}

internal fun WindGrid.toWindRaster(width: Int): WindRaster {
    val height: Int = (width * (mercatorNorth - mercatorSouth) / LONGITUDE_SPAN.toRadians()).roundToInt()
    val rasterXs = FloatArray(size = longitudes.size) { index: Int -> rasterX(longitude = longitudes[index], width = width) }
    val rasterYs = FloatArray(size = latitudes.size) { index: Int -> rasterY(latitude = latitudes[index], height = height) }
    val pointIndices = IntArray(size = width * height * CORNER_COUNT) { NO_POINT }
    val weights = FloatArray(size = width * height * CORNER_COUNT)
    val triangleRasterizer = TriangleRasterizer(
        width = width,
        height = height,
        rasterXs = rasterXs,
        rasterYs = rasterYs,
        pointIndices = pointIndices,
        weights = weights
    )
    for (row in 0 until rowCount - 1) {
        for (column in 0 until columnCount - 1) {
            val southWest: Int = row * columnCount + column
            val southEast: Int = southWest + 1
            val northWest: Int = southWest + columnCount
            val northEast: Int = northWest + 1
            triangleRasterizer.rasterize(first = southWest, second = southEast, third = northEast)
            triangleRasterizer.rasterize(first = southWest, second = northEast, third = northWest)
        }
    }
    return WindRaster(
        width = width,
        height = height,
        pointIndices = pointIndices,
        weights = weights
    )
}

internal fun rasterX(
    longitude: Double,
    width: Int
): Float = ((longitude - RADAR_WEST_LONGITUDE) / LONGITUDE_SPAN * width).toFloat()

internal fun rasterY(
    latitude: Double,
    height: Int
): Float = ((mercatorNorth - WebMercator.y(latitude = latitude)) / (mercatorNorth - mercatorSouth) * height).toFloat()

internal fun rasterLongitude(
    x: Float,
    width: Int
): Double = RADAR_WEST_LONGITUDE + x / width * LONGITUDE_SPAN

internal fun rasterLatitude(
    y: Float,
    height: Int
): Double = WebMercator.latitude(y = mercatorNorth - y / height * (mercatorNorth - mercatorSouth))

private class TriangleRasterizer(
    private val width: Int,
    private val height: Int,
    private val rasterXs: FloatArray,
    private val rasterYs: FloatArray,
    private val pointIndices: IntArray,
    private val weights: FloatArray
) {

    fun rasterize(
        first: Int,
        second: Int,
        third: Int
    ) {
        val x1: Float = rasterXs[first]
        val y1: Float = rasterYs[first]
        val x2: Float = rasterXs[second]
        val y2: Float = rasterYs[second]
        val x3: Float = rasterXs[third]
        val y3: Float = rasterYs[third]
        val determinant: Float = (y2 - y3) * (x1 - x3) + (x3 - x2) * (y1 - y3)
        if (determinant == 0f) {
            return
        }
        val minimumX: Int = max(0, floor(min(x1, min(x2, x3))).toInt())
        val maximumX: Int = min(width - 1, ceil(max(x1, max(x2, x3))).toInt())
        val minimumY: Int = max(0, floor(min(y1, min(y2, y3))).toInt())
        val maximumY: Int = min(height - 1, ceil(max(y1, max(y2, y3))).toInt())
        for (y in minimumY..maximumY) {
            val centerY: Float = y + PIXEL_CENTER
            for (x in minimumX..maximumX) {
                val centerX: Float = x + PIXEL_CENTER
                val firstWeight: Float = ((y2 - y3) * (centerX - x3) + (x3 - x2) * (centerY - y3)) / determinant
                val secondWeight: Float = ((y3 - y1) * (centerX - x3) + (x1 - x3) * (centerY - y3)) / determinant
                val thirdWeight: Float = 1f - firstWeight - secondWeight
                val offset: Int = (y * width + x) * CORNER_COUNT
                val isInside: Boolean = firstWeight >= EDGE_TOLERANCE && secondWeight >= EDGE_TOLERANCE && thirdWeight >= EDGE_TOLERANCE
                if (isInside && pointIndices[offset] == NO_POINT) {
                    pointIndices[offset] = first
                    pointIndices[offset + 1] = second
                    pointIndices[offset + 2] = third
                    weights[offset] = firstWeight
                    weights[offset + 1] = secondWeight
                    weights[offset + 2] = thirdWeight
                }
            }
        }
    }
}