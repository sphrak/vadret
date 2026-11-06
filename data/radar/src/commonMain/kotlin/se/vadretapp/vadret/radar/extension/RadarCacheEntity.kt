package se.vadretapp.vadret.radar.extension

import se.vadretapp.vadret.radar.local.RadarCacheEntity
import se.vadretapp.vadret.radar.local.RadarImageEntity
import se.vadretapp.vadret.radar.model.Radar
import se.vadretapp.vadret.radar.model.RadarImage
import se.vadretapp.vadret.radar.model.SavedRadar
import kotlin.time.Instant

internal fun RadarCacheEntity.toRadar(): Radar =
    Radar(
        images = images.map { radarImageEntity: RadarImageEntity ->
            RadarImage(
                imageUrl = radarImageEntity.imageUrl,
                validAt = Instant.fromEpochMilliseconds(epochMilliseconds = radarImageEntity.validAtEpochMillis)
            )
        }
    )

internal fun RadarCacheEntity?.toSavedRadar(): SavedRadar =
    SavedRadar(
        radar = this?.toRadar() ?: Radar(images = emptyList()),
        fetchedAt = if (this != null) {
            Instant.fromEpochMilliseconds(epochMilliseconds = fetchedAtEpochMillis)
        } else {
            null
        }
    )