package se.vadretapp.vadret.radar.ui.overlay

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Paint
import org.jetbrains.skia.VertexMode

private val trianglePaint: Paint = Paint().apply { isAntiAlias = true }

internal actual fun DrawScope.drawColoredTriangles(
    positions: FloatArray,
    colors: IntArray
) {
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawVertices(
            vertexMode = VertexMode.TRIANGLES,
            positions = positions,
            colors = colors,
            texCoords = null,
            indices = null,
            blendMode = BlendMode.DST,
            paint = trianglePaint
        )
    }
}