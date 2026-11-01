package se.vadretapp.vadret.radar.ui.overlay

import androidx.compose.ui.graphics.drawscope.DrawScope

internal expect fun DrawScope.drawColoredTriangles(
    positions: FloatArray,
    colors: IntArray
)