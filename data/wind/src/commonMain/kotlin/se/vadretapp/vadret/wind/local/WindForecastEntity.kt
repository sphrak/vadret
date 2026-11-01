package se.vadretapp.vadret.wind.local

internal data class WindForecastEntity(
    val referenceTimeEpochMillis: Long,
    val firstValidAtEpochMillis: Long,
    val columnCount: Int,
    val rowCount: Int,
    val longitudes: List<Double>,
    val latitudes: List<Double>,
    val frames: List<WindFrameEntity>
)