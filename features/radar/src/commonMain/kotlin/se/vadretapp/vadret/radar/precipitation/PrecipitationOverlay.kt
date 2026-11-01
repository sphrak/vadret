package se.vadretapp.vadret.radar.precipitation

import androidx.compose.ui.graphics.ImageBitmap

internal sealed interface PrecipitationOverlay {

    class Colored(
        val image: ImageBitmap
    ) : PrecipitationOverlay

    class Echoes(
        val echoes: ImageBitmap,
        val motion: PrecipitationMotion?
    ) : PrecipitationOverlay
}