package se.vadretapp.vadret.radar.precipitation

internal const val MOTION_CELL_SIZE = 32
internal const val MAXIMUM_ECHO_DISPLACEMENT = 20f

internal class MotionField(
    val columns: Int,
    val rows: Int,
    val eastward: FloatArray,
    val southward: FloatArray
)