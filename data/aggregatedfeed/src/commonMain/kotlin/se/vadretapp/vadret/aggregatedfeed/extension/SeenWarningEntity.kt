package se.vadretapp.vadret.aggregatedfeed.extension

import se.vadretapp.vadret.aggregatedfeed.local.SeenWarningEntity
import se.vadretapp.vadret.aggregatedfeed.model.SeenWarning
import kotlin.time.Instant

internal fun SeenWarningEntity.toSeenWarning(): SeenWarning =
    SeenWarning(
        warningIdentifier = warningIdentifier,
        seenAt = Instant.fromEpochMilliseconds(epochMilliseconds = seenAtEpochMillis)
    )