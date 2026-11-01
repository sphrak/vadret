package se.vadretapp.vadret.radar

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.radar.model.Radar

internal class DefaultGetRadarTask(
    private val radarRepository: RadarRepository
) : GetRadarTask {

    override suspend fun invoke(imageUrl: String?): Either<Failure, Radar> =
        if (imageUrl == null) {
            radarRepository.getRadar()
        } else {
            radarRepository.getRadarContaining(imageUrl = imageUrl)
        }
}