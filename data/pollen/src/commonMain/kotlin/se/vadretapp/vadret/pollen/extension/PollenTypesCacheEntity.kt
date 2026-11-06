package se.vadretapp.vadret.pollen.extension

import se.vadretapp.vadret.pollen.local.PollenTypeEntity
import se.vadretapp.vadret.pollen.local.PollenTypesCacheEntity
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.pollen.model.PollenTypes
import kotlin.time.Instant

internal fun PollenTypesCacheEntity.toPollenTypes(): List<PollenType> = pollenTypes.map { pollenTypeEntity: PollenTypeEntity -> pollenTypeEntity.toPollenType() }

internal fun PollenTypesCacheEntity.toPollenTypesWithFetchTime(): PollenTypes =
    PollenTypes(
        pollenTypes = toPollenTypes(),
        fetchedAt = Instant.fromEpochMilliseconds(epochMilliseconds = fetchedAtEpochMillis)
    )