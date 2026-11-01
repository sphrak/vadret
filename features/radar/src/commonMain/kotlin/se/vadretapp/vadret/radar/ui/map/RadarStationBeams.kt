package se.vadretapp.vadret.radar.ui.map

internal const val BEAM_DEGREES_IN_CIRCLE = 360f
private const val GOLDEN_ANGLE_DEGREES = 137.50777f
private const val SHORTEST_TURN_MILLISECONDS = 14_000L
private const val TURN_MILLISECONDS_STEP = 1_226L
private const val TURN_MILLISECONDS_SPREAD = 4_800L

internal fun beamTurnMilliseconds(stationIndex: Int): Long = SHORTEST_TURN_MILLISECONDS + stationIndex * TURN_MILLISECONDS_STEP % TURN_MILLISECONDS_SPREAD

internal fun beamDegrees(
    stationIndex: Int,
    timeMilliseconds: Long
): Float {
    val turnMilliseconds: Long = beamTurnMilliseconds(stationIndex = stationIndex)
    val turnFraction: Float = (timeMilliseconds % turnMilliseconds).toFloat() / turnMilliseconds
    return (stationIndex * GOLDEN_ANGLE_DEGREES + turnFraction * BEAM_DEGREES_IN_CIRCLE) % BEAM_DEGREES_IN_CIRCLE
}