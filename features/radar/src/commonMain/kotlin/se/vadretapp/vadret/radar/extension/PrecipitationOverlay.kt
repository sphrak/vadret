package se.vadretapp.vadret.radar.extension

import se.vadretapp.vadret.radar.precipitation.PrecipitationOverlay

internal fun PrecipitationOverlay.withoutMotion(): PrecipitationOverlay =
    when (this) {
        is PrecipitationOverlay.Colored -> this
        is PrecipitationOverlay.Echoes -> if (motion == null) {
            this
        } else {
            PrecipitationOverlay.Echoes(
                echoes = echoes,
                motion = null
            )
        }
    }