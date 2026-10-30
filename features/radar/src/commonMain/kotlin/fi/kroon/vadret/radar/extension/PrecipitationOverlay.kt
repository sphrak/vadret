package fi.kroon.vadret.radar.extension

import fi.kroon.vadret.radar.precipitation.PrecipitationOverlay

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