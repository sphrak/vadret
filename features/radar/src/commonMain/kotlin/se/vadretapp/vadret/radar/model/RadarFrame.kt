package se.vadretapp.vadret.radar.model

import se.vadretapp.vadret.ui.model.ElapsedTime

internal data class RadarFrame(
    val imageUrl: String,
    val timeText: String,
    val weekday: String,
    val elapsedTime: ElapsedTime
)