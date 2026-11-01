package se.vadretapp.vadret.radar.precipitation

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlin.time.Duration

internal class ColoredPrecipitationRenderer(
    private val coroutineDispatcher: CoroutineDispatcher
) : PrecipitationRenderer {

    override suspend fun render(echoes: RadarEchoes): PrecipitationOverlay =
        withContext(context = coroutineDispatcher) {
            PrecipitationOverlay.Colored(image = echoes.coloredImage)
        }

    override suspend fun renderMotion(
        echoes: RadarEchoes,
        nextEchoes: RadarEchoes,
        duration: Duration
    ): PrecipitationOverlay = render(echoes = echoes)
}