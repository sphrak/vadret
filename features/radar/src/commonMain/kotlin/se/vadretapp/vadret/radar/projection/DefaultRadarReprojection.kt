package se.vadretapp.vadret.radar.projection

import se.vadretapp.vadret.radar.extension.toRadians
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.update
import kotlin.math.roundToInt

private const val OUTPUT_WIDTH = 720
private const val NO_ECHO: Byte = 0
private const val PIXEL_CENTER = 0.5
private const val BYTE_MASK = 0xFF
private const val NO_DATA = 255
private const val NO_ECHO_LEVEL = 0f

internal class DefaultRadarReprojection : RadarReprojection {

    private val mercatorNorth: Double = WebMercator.y(latitude = RADAR_NORTH_LATITUDE)
    private val mercatorSouth: Double = WebMercator.y(latitude = RADAR_SOUTH_LATITUDE)
    private val longitudeSpan: Double = RADAR_EAST_LONGITUDE - RADAR_WEST_LONGITUDE

    private val outputWidth: Int = OUTPUT_WIDTH
    private val outputHeight: Int = (OUTPUT_WIDTH * (mercatorNorth - mercatorSouth) / longitudeSpan.toRadians()).roundToInt()

    private val samplePointsBySize: AtomicReference<Map<Long, SamplePoints>> = AtomicReference(value = emptyMap())

    override fun reproject(radarRaster: RadarRaster): ReprojectedImage {
        val samplePoints: SamplePoints = cachedSamplePoints(
            sourceWidth = radarRaster.width,
            sourceHeight = radarRaster.height
        )
        return ReprojectedImage(
            echoes = ByteArray(size = outputWidth * outputHeight) { index: Int ->
                reprojectedEcho(
                    radarRaster = radarRaster,
                    samplePoints = samplePoints,
                    index = index
                )
            },
            width = outputWidth,
            height = outputHeight
        )
    }

    private fun cachedSamplePoints(
        sourceWidth: Int,
        sourceHeight: Int
    ): SamplePoints {
        val key: Long = sizeKey(
            width = sourceWidth,
            height = sourceHeight
        )
        val cached: SamplePoints? = samplePointsBySize.load()[key]
        if (cached != null) {
            return cached
        }
        val computed: SamplePoints = samplePoints(
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            latitudes = DoubleArray(size = outputHeight) { row: Int ->
                WebMercator.latitude(y = mercatorNorth - (row + PIXEL_CENTER) / outputHeight * (mercatorNorth - mercatorSouth))
            },
            longitudes = DoubleArray(size = outputWidth) { column: Int ->
                RADAR_WEST_LONGITUDE + (column + PIXEL_CENTER) / outputWidth * longitudeSpan
            }
        )
        samplePointsBySize.update { cachedBySize: Map<Long, SamplePoints> -> cachedBySize + (key to computed) }
        return computed
    }
    private fun reprojectedEcho(
        radarRaster: RadarRaster,
        samplePoints: SamplePoints,
        index: Int
    ): Byte {
        if (samplePoints.isOutside(index = index)) {
            return NO_ECHO
        }
        val topLeft: Int = samplePoints.topLeftIndices[index]
        val right: Int = if (topLeft % radarRaster.width < radarRaster.width - 1) {
            1
        } else {
            0
        }
        val below: Int = if (topLeft / radarRaster.width < radarRaster.height - 1) {
            radarRaster.width
        } else {
            0
        }
        val columnFraction: Float = samplePoints.columnFractions[index]
        val top: Float = interpolate(
            from = radarRaster.echo(index = topLeft),
            to = radarRaster.echo(index = topLeft + right),
            fraction = columnFraction
        )
        val bottom: Float = interpolate(
            from = radarRaster.echo(index = topLeft + below),
            to = radarRaster.echo(index = topLeft + below + right),
            fraction = columnFraction
        )
        val value: Float = interpolate(
            from = top,
            to = bottom,
            fraction = samplePoints.rowFractions[index]
        )
        return value.roundToInt().toByte()
    }

    private fun RadarRaster.echo(index: Int): Float {
        val value: Int = values[index].toInt() and BYTE_MASK
        return if (value == NO_DATA) {
            NO_ECHO_LEVEL
        } else {
            value.toFloat()
        }
    }

    private fun interpolate(
        from: Float,
        to: Float,
        fraction: Float
    ): Float = from + (to - from) * fraction

    private fun sizeKey(
        width: Int,
        height: Int
    ): Long = width.toLong() shl Int.SIZE_BITS or height.toLong()
}