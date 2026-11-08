package se.vadretapp.vadret.aurora.model

import kotlin.time.Instant

data class AuroraHour(
    val time: Instant,
    val score: Int
)