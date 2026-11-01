package se.vadretapp.vadret.radar.precipitation

import se.vadretapp.vadret.radar.projection.PrecipitationColorScale

private const val SAMPLE_SPACING = 4
private const val CELL_SAMPLES = MOTION_CELL_SIZE / SAMPLE_SPACING
private const val SEARCH_RADIUS = 4
private const val SEARCH_SIDE = SEARCH_RADIUS * 2 + 1
private const val MINIMUM_CELL_SIGNAL = 256f
private const val SMOOTHING_PASSES = 2
private const val NEIGHBOUR_REACH = 1
private const val BYTE_MASK = 0xFF
private const val NO_SIGNAL = 0f
private const val NO_DISPLACEMENT = 0f
private const val NO_OFFSET = 0f
private const val MAXIMUM_SAMPLE_OFFSET = 0.5f
private const val PARABOLA_DIVISOR = 2f

private class SampleGrid(
    val values: FloatArray,
    val width: Int,
    val height: Int
) {

    fun at(
        x: Int,
        y: Int
    ): Float =
        if (x in 0 until width && y in 0 until height) {
            values[y * width + x]
        } else {
            NO_SIGNAL
        }
}

private class CellBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

private class Displacement(
    val eastward: Float,
    val southward: Float
)

internal object EchoMotionEstimator {

    fun estimate(
        previous: ByteArray,
        next: ByteArray,
        width: Int,
        height: Int
    ): MotionField {
        val previousSamples: SampleGrid = samples(
            echoes = previous,
            width = width,
            height = height
        )
        val nextSamples: SampleGrid = samples(
            echoes = next,
            width = width,
            height = height
        )
        val columns: Int = divideRoundingUp(
            dividend = previousSamples.width,
            divisor = CELL_SAMPLES
        )
        val rows: Int = divideRoundingUp(
            dividend = previousSamples.height,
            divisor = CELL_SAMPLES
        )
        val eastward = FloatArray(size = columns * rows) { Float.NaN }
        val southward = FloatArray(size = columns * rows) { Float.NaN }
        for (cellIndex in eastward.indices) {
            val displacement: Displacement = cellDisplacement(
                previous = previousSamples,
                next = nextSamples,
                cellBounds = cellBounds(
                    column = cellIndex % columns,
                    row = cellIndex / columns,
                    samples = previousSamples
                )
            ) ?: continue
            eastward[cellIndex] = displacement.eastward
            southward[cellIndex] = displacement.southward
        }
        fillUnknownCells(
            eastward = eastward,
            southward = southward,
            columns = columns,
            rows = rows
        )
        repeat(times = SMOOTHING_PASSES) {
            smooth(
                values = eastward,
                columns = columns,
                rows = rows
            )
            smooth(
                values = southward,
                columns = columns,
                rows = rows
            )
        }
        return MotionField(
            columns = columns,
            rows = rows,
            eastward = eastward,
            southward = southward
        )
    }

    private fun samples(
        echoes: ByteArray,
        width: Int,
        height: Int
    ): SampleGrid {
        val sampleWidth: Int = divideRoundingUp(
            dividend = width,
            divisor = SAMPLE_SPACING
        )
        val sampleHeight: Int = divideRoundingUp(
            dividend = height,
            divisor = SAMPLE_SPACING
        )
        val values = FloatArray(size = sampleWidth * sampleHeight)
        for (index in echoes.indices) {
            val signal: Int = (echoes[index].toInt() and BYTE_MASK) - PrecipitationColorScale.firstVisibleEchoLevel
            if (signal > 0) {
                val sampleIndex: Int = index / width / SAMPLE_SPACING * sampleWidth + index % width / SAMPLE_SPACING
                values[sampleIndex] += signal.toFloat()
            }
        }
        return SampleGrid(
            values = values,
            width = sampleWidth,
            height = sampleHeight
        )
    }

    private fun cellBounds(
        column: Int,
        row: Int,
        samples: SampleGrid
    ): CellBounds =
        CellBounds(
            left = column * CELL_SAMPLES,
            top = row * CELL_SAMPLES,
            right = minOf(
                a = (column + 1) * CELL_SAMPLES,
                b = samples.width
            ),
            bottom = minOf(
                a = (row + 1) * CELL_SAMPLES,
                b = samples.height
            )
        )

    private fun cellDisplacement(
        previous: SampleGrid,
        next: SampleGrid,
        cellBounds: CellBounds
    ): Displacement? {
        val cellSignal: Float = signal(
            samples = previous,
            cellBounds = cellBounds
        )
        if (cellSignal < MINIMUM_CELL_SIGNAL) {
            return null
        }
        val differences = FloatArray(size = SEARCH_SIDE * SEARCH_SIDE) { candidate: Int ->
            difference(
                previous = previous,
                next = next,
                cellBounds = cellBounds,
                shiftX = candidate % SEARCH_SIDE - SEARCH_RADIUS,
                shiftY = candidate / SEARCH_SIDE - SEARCH_RADIUS
            )
        }
        val best: Int = differences.indices.minWith(
            comparator = compareBy<Int> { candidate: Int -> differences[candidate] }
                .thenBy { candidate: Int -> squaredShift(candidate = candidate) }
        )
        val shiftX: Int = best % SEARCH_SIDE - SEARCH_RADIUS
        val shiftY: Int = best / SEARCH_SIDE - SEARCH_RADIUS
        val eastwardOffset: Float = sampleOffset(
            before = differences.differenceAt(
                shiftX = shiftX - 1,
                shiftY = shiftY
            ),
            best = differences[best],
            after = differences.differenceAt(
                shiftX = shiftX + 1,
                shiftY = shiftY
            )
        )
        val southwardOffset: Float = sampleOffset(
            before = differences.differenceAt(
                shiftX = shiftX,
                shiftY = shiftY - 1
            ),
            best = differences[best],
            after = differences.differenceAt(
                shiftX = shiftX,
                shiftY = shiftY + 1
            )
        )
        return Displacement(
            eastward = (shiftX + eastwardOffset) * SAMPLE_SPACING,
            southward = (shiftY + southwardOffset) * SAMPLE_SPACING
        )
    }

    private fun signal(
        samples: SampleGrid,
        cellBounds: CellBounds
    ): Float {
        var signal: Float = NO_SIGNAL
        for (y in cellBounds.top until cellBounds.bottom) {
            for (x in cellBounds.left until cellBounds.right) {
                signal += samples.at(
                    x = x,
                    y = y
                )
            }
        }
        return signal
    }

    private fun difference(
        previous: SampleGrid,
        next: SampleGrid,
        cellBounds: CellBounds,
        shiftX: Int,
        shiftY: Int
    ): Float {
        var difference: Float = NO_SIGNAL
        for (y in cellBounds.top until cellBounds.bottom) {
            for (x in cellBounds.left until cellBounds.right) {
                val previousSignal: Float = previous.at(
                    x = x,
                    y = y
                )
                val nextSignal: Float = next.at(
                    x = x + shiftX,
                    y = y + shiftY
                )
                val signalDifference: Float = previousSignal - nextSignal
                difference += signalDifference * signalDifference
            }
        }
        return difference
    }

    private fun squaredShift(candidate: Int): Int {
        val shiftX: Int = candidate % SEARCH_SIDE - SEARCH_RADIUS
        val shiftY: Int = candidate / SEARCH_SIDE - SEARCH_RADIUS
        return shiftX * shiftX + shiftY * shiftY
    }

    private fun FloatArray.differenceAt(
        shiftX: Int,
        shiftY: Int
    ): Float? =
        if (shiftX in -SEARCH_RADIUS..SEARCH_RADIUS && shiftY in -SEARCH_RADIUS..SEARCH_RADIUS) {
            this[(shiftY + SEARCH_RADIUS) * SEARCH_SIDE + shiftX + SEARCH_RADIUS]
        } else {
            null
        }

    private fun sampleOffset(
        before: Float?,
        best: Float,
        after: Float?
    ): Float {
        if (before == null || after == null) {
            return NO_OFFSET
        }
        val curvature: Float = before - PARABOLA_DIVISOR * best + after
        if (curvature <= NO_SIGNAL) {
            return NO_OFFSET
        }
        return ((before - after) / (PARABOLA_DIVISOR * curvature)).coerceIn(
            minimumValue = -MAXIMUM_SAMPLE_OFFSET,
            maximumValue = MAXIMUM_SAMPLE_OFFSET
        )
    }

    private fun fillUnknownCells(
        eastward: FloatArray,
        southward: FloatArray,
        columns: Int,
        rows: Int
    ) {
        if (eastward.all { value: Float -> value.isNaN() }) {
            eastward.fill(element = NO_DISPLACEMENT)
            southward.fill(element = NO_DISPLACEMENT)
            return
        }
        while (eastward.any { value: Float -> value.isNaN() }) {
            val knownEastward: FloatArray = eastward.copyOf()
            val knownSouthward: FloatArray = southward.copyOf()
            for (cellIndex in eastward.indices) {
                if (knownEastward[cellIndex].isNaN()) {
                    eastward[cellIndex] = neighbourMean(
                        values = knownEastward,
                        cellIndex = cellIndex,
                        columns = columns,
                        rows = rows
                    )
                    southward[cellIndex] = neighbourMean(
                        values = knownSouthward,
                        cellIndex = cellIndex,
                        columns = columns,
                        rows = rows
                    )
                }
            }
        }
    }

    private fun smooth(
        values: FloatArray,
        columns: Int,
        rows: Int
    ) {
        val unsmoothed: FloatArray = values.copyOf()
        for (cellIndex in values.indices) {
            values[cellIndex] = neighbourMean(
                values = unsmoothed,
                cellIndex = cellIndex,
                columns = columns,
                rows = rows
            )
        }
    }

    private fun neighbourMean(
        values: FloatArray,
        cellIndex: Int,
        columns: Int,
        rows: Int
    ): Float {
        val column: Int = cellIndex % columns
        val row: Int = cellIndex / columns
        var sum: Float = NO_DISPLACEMENT
        var count = 0
        for (neighbourRow in row - NEIGHBOUR_REACH..row + NEIGHBOUR_REACH) {
            for (neighbourColumn in column - NEIGHBOUR_REACH..column + NEIGHBOUR_REACH) {
                if (neighbourRow in 0 until rows && neighbourColumn in 0 until columns) {
                    val value: Float = values[neighbourRow * columns + neighbourColumn]
                    if (!value.isNaN()) {
                        sum += value
                        count++
                    }
                }
            }
        }
        return if (count > 0) {
            sum / count
        } else {
            Float.NaN
        }
    }

    private fun divideRoundingUp(
        dividend: Int,
        divisor: Int
    ): Int = (dividend + divisor - 1) / divisor
}