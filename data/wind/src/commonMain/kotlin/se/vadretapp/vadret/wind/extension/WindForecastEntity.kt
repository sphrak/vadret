package se.vadretapp.vadret.wind.extension

import se.vadretapp.vadret.wind.local.WindForecastEntity
import se.vadretapp.vadret.wind.local.WindFrameEntity
import se.vadretapp.vadret.wind.model.WindForecast
import se.vadretapp.vadret.wind.model.WindFrame
import se.vadretapp.vadret.wind.model.WindGrid
import kotlin.time.Instant

internal fun WindForecastEntity.toWindForecast(): WindForecast =
    WindForecast(
        referenceTime = Instant.fromEpochMilliseconds(epochMilliseconds = referenceTimeEpochMillis),
        grid = WindGrid(
            columnCount = columnCount,
            rowCount = rowCount,
            longitudes = longitudes,
            latitudes = latitudes
        ),
        frames = frames.map { windFrameEntity: WindFrameEntity -> windFrameEntity.toWindFrame() },
        fetchedAt = Instant.fromEpochMilliseconds(epochMilliseconds = fetchedAtEpochMillis)
    )

private fun WindFrameEntity.toWindFrame(): WindFrame =
    WindFrame(
        validAt = Instant.fromEpochMilliseconds(epochMilliseconds = validAtEpochMillis),
        windSpeeds = windSpeeds,
        windFromDirections = windFromDirections
    )