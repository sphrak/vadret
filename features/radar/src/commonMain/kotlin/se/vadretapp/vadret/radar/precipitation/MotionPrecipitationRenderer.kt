package se.vadretapp.vadret.radar.precipitation

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import se.vadretapp.vadret.radar.extension.toImageBitmap
import se.vadretapp.vadret.radar.projection.ReprojectedImage
import kotlin.time.Duration

internal class MotionPrecipitationRenderer(
    private val coroutineDispatcher: CoroutineDispatcher
) : PrecipitationRenderer {

    override suspend fun render(echoes: RadarEchoes): PrecipitationOverlay =
        PrecipitationOverlay.Echoes(
            echoes = echoes.image,
            motion = null
        )

    override suspend fun renderMotion(
        echoes: RadarEchoes,
        nextEchoes: RadarEchoes,
        duration: Duration
    ): PrecipitationOverlay {
        val previous: ReprojectedImage = echoes.reprojectedImage
        val next: ReprojectedImage = nextEchoes.reprojectedImage
        if (previous.width != next.width || previous.height != next.height) {
            return render(echoes = echoes)
        }
        return withContext(context = coroutineDispatcher) {
            val motionField: MotionField = EchoMotionEstimator.estimate(
                previous = previous.echoes,
                next = next.echoes,
                width = previous.width,
                height = previous.height
            )
            PrecipitationOverlay.Echoes(
                echoes = echoes.image,
                motion = PrecipitationMotion(
                    nextEchoes = nextEchoes.image,
                    field = motionField.toImageBitmap(),
                    duration = duration
                )
            )
        }
    }
}