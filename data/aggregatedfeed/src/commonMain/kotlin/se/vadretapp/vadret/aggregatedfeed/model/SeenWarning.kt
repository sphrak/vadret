package se.vadretapp.vadret.aggregatedfeed.model

import kotlin.time.Instant

data class SeenWarning(
    val warningIdentifier: String,
    val seenAt: Instant
)