package se.vadretapp.vadret.radar.ui.overlay

import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp

private const val NO_OFFSET = 0
private const val FLOATS_PER_VERTEX = 2
private const val VERTICES_PER_TRIANGLE_PAIR = 6
private const val HEAD_VERTEX = 2
private const val SECOND_TAIL_VERTEX = 1
private const val SECOND_HEAD_VERTEX = 4
private const val HALF = 2f
private val lineWidth = 1.dp

private val trianglePaint: Paint = Paint()
private val linePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

internal actual fun DrawScope.drawColoredTriangles(
    positions: FloatArray,
    colors: IntArray
) {
    drawIntoCanvas { canvas ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            canvas.nativeCanvas.drawVertices(
                Canvas.VertexMode.TRIANGLES,
                positions.size,
                positions,
                NO_OFFSET,
                null,
                NO_OFFSET,
                colors,
                NO_OFFSET,
                null,
                NO_OFFSET,
                NO_OFFSET,
                trianglePaint
            )
        } else {
            linePaint.strokeWidth = lineWidth.toPx()
            drawSegmentLines(
                canvas = canvas.nativeCanvas,
                positions = positions,
                colors = colors
            )
        }
    }
}

private fun drawSegmentLines(
    canvas: Canvas,
    positions: FloatArray,
    colors: IntArray
) {
    val vertexCount: Int = positions.size / FLOATS_PER_VERTEX
    for (firstVertex in 0 until vertexCount step VERTICES_PER_TRIANGLE_PAIR) {
        linePaint.color = colors[firstVertex + HEAD_VERTEX]
        canvas.drawLine(
            midpoint(positions = positions, first = firstVertex, second = firstVertex + SECOND_TAIL_VERTEX, axis = 0),
            midpoint(positions = positions, first = firstVertex, second = firstVertex + SECOND_TAIL_VERTEX, axis = 1),
            midpoint(positions = positions, first = firstVertex + HEAD_VERTEX, second = firstVertex + SECOND_HEAD_VERTEX, axis = 0),
            midpoint(positions = positions, first = firstVertex + HEAD_VERTEX, second = firstVertex + SECOND_HEAD_VERTEX, axis = 1),
            linePaint
        )
    }
}

private fun midpoint(
    positions: FloatArray,
    first: Int,
    second: Int,
    axis: Int
): Float = (positions[first * FLOATS_PER_VERTEX + axis] + positions[second * FLOATS_PER_VERTEX + axis]) / HALF