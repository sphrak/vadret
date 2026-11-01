package se.vadretapp.vadret.radar.precipitation

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.time.Duration

internal class PrecipitationMotion(
    val nextEchoes: ImageBitmap,
    val field: ImageBitmap,
    val duration: Duration
)