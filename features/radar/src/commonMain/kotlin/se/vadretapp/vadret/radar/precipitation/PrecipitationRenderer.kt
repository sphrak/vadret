package se.vadretapp.vadret.radar.precipitation

import kotlin.time.Duration

internal interface PrecipitationRenderer {
    suspend fun render(echoes: RadarEchoes): PrecipitationOverlay
    suspend fun renderMotion(
        echoes: RadarEchoes,
        nextEchoes: RadarEchoes,
        duration: Duration
    ): PrecipitationOverlay
}