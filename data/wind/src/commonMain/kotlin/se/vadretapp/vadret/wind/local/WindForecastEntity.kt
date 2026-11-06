package se.vadretapp.vadret.wind.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class WindForecastEntity(
    @SerialName(value = "reference_time_epoch_millis")
    val referenceTimeEpochMillis: Long,
    @SerialName(value = "first_valid_at_epoch_millis")
    val firstValidAtEpochMillis: Long,
    @SerialName(value = "column_count")
    val columnCount: Int,
    @SerialName(value = "row_count")
    val rowCount: Int,
    @SerialName(value = "longitudes")
    val longitudes: List<Double>,
    @SerialName(value = "latitudes")
    val latitudes: List<Double>,
    @SerialName(value = "frames")
    val frames: List<WindFrameEntity>
)