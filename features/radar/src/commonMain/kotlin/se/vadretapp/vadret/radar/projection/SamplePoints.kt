package se.vadretapp.vadret.radar.projection

import kotlin.math.floor

private const val OUTSIDE = -1
private const val PIXEL_CENTER = 0.5
private const val GRID_STEP = 8

internal class SamplePoints(
    val topLeftIndices: IntArray,
    val columnFractions: FloatArray,
    val rowFractions: FloatArray
) {

    fun isOutside(index: Int): Boolean = topLeftIndices[index] == OUTSIDE
}

private class SourceGrid(
    val nodeColumnCount: Int,
    val columns: DoubleArray,
    val rows: DoubleArray
)

internal fun samplePoints(
    sourceWidth: Int,
    sourceHeight: Int,
    latitudes: DoubleArray,
    longitudes: DoubleArray
): SamplePoints {
    val outputWidth: Int = longitudes.size
    val outputHeight: Int = latitudes.size
    val sourceGrid: SourceGrid = sourceGrid(
        sourceWidth = sourceWidth,
        sourceHeight = sourceHeight,
        latitudes = latitudes,
        longitudes = longitudes
    )
    val pixelCount: Int = outputWidth * outputHeight
    val topLeftIndices = IntArray(size = pixelCount) { OUTSIDE }
    val columnFractions = FloatArray(size = pixelCount)
    val rowFractions = FloatArray(size = pixelCount)
    for (index in 0 until pixelCount) {
        val outputColumn: Int = index % outputWidth
        val outputRow: Int = index / outputWidth
        val column: Double = sourceGrid.interpolate(
            values = sourceGrid.columns,
            outputColumn = outputColumn,
            outputRow = outputRow,
            outputWidth = outputWidth,
            outputHeight = outputHeight
        )
        val row: Double = sourceGrid.interpolate(
            values = sourceGrid.rows,
            outputColumn = outputColumn,
            outputRow = outputRow,
            outputWidth = outputWidth,
            outputHeight = outputHeight
        )
        if (column >= 0.0 && column < sourceWidth && row >= 0.0 && row < sourceHeight) {
            val sampleColumn: Double = (column - PIXEL_CENTER).coerceIn(minimumValue = 0.0, maximumValue = sourceWidth - 1.0)
            val sampleRow: Double = (row - PIXEL_CENTER).coerceIn(minimumValue = 0.0, maximumValue = sourceHeight - 1.0)
            val leftColumn: Int = floor(sampleColumn).toInt()
            val topRow: Int = floor(sampleRow).toInt()
            topLeftIndices[index] = topRow * sourceWidth + leftColumn
            columnFractions[index] = (sampleColumn - leftColumn).toFloat()
            rowFractions[index] = (sampleRow - topRow).toFloat()
        }
    }
    return SamplePoints(
        topLeftIndices = topLeftIndices,
        columnFractions = columnFractions,
        rowFractions = rowFractions
    )
}

private fun sourceGrid(
    sourceWidth: Int,
    sourceHeight: Int,
    latitudes: DoubleArray,
    longitudes: DoubleArray
): SourceGrid {
    val metersPerColumn: Double = (RADAR_EAST_EASTING - RADAR_WEST_EASTING) / sourceWidth
    val metersPerRow: Double = (RADAR_NORTH_NORTHING - RADAR_SOUTH_NORTHING) / sourceHeight
    val nodeColumnCount: Int = nodeCount(outputSize = longitudes.size)
    val nodeRowCount: Int = nodeCount(outputSize = latitudes.size)
    val columns = DoubleArray(size = nodeColumnCount * nodeRowCount)
    val rows = DoubleArray(size = nodeColumnCount * nodeRowCount)
    for (nodeRow in 0 until nodeRowCount) {
        for (nodeColumn in 0 until nodeColumnCount) {
            val projectedPoint: ProjectedPoint = Sweref99Tm.project(
                latitude = latitudes[nodePosition(node = nodeRow, outputSize = latitudes.size)],
                longitude = longitudes[nodePosition(node = nodeColumn, outputSize = longitudes.size)]
            )
            val node: Int = nodeRow * nodeColumnCount + nodeColumn
            columns[node] = (projectedPoint.easting - RADAR_WEST_EASTING) / metersPerColumn
            rows[node] = (RADAR_NORTH_NORTHING - projectedPoint.northing) / metersPerRow
        }
    }
    return SourceGrid(
        nodeColumnCount = nodeColumnCount,
        columns = columns,
        rows = rows
    )
}

private fun SourceGrid.interpolate(
    values: DoubleArray,
    outputColumn: Int,
    outputRow: Int,
    outputWidth: Int,
    outputHeight: Int
): Double {
    val leftNode: Int = outputColumn / GRID_STEP
    val topNode: Int = outputRow / GRID_STEP
    val columnFraction: Double = nodeFraction(output = outputColumn, node = leftNode, outputSize = outputWidth)
    val rowFraction: Double = nodeFraction(output = outputRow, node = topNode, outputSize = outputHeight)
    val topLeft: Int = topNode * nodeColumnCount + leftNode
    val bottomLeft: Int = topLeft + nodeColumnCount
    val top: Double = values[topLeft] + (values[topLeft + 1] - values[topLeft]) * columnFraction
    val bottom: Double = values[bottomLeft] + (values[bottomLeft + 1] - values[bottomLeft]) * columnFraction
    return top + (bottom - top) * rowFraction
}

private fun nodeCount(outputSize: Int): Int = (outputSize - 1 + GRID_STEP - 1) / GRID_STEP + 1

private fun nodePosition(
    node: Int,
    outputSize: Int
): Int = minOf(node * GRID_STEP, outputSize - 1)

private fun nodeFraction(
    output: Int,
    node: Int,
    outputSize: Int
): Double {
    val start: Int = nodePosition(node = node, outputSize = outputSize)
    val end: Int = nodePosition(node = node + 1, outputSize = outputSize)
    return if (end > start) {
        (output - start).toDouble() / (end - start)
    } else {
        0.0
    }
}