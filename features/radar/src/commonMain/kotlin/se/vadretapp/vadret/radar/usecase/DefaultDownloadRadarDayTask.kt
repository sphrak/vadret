package se.vadretapp.vadret.radar.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.flatMap
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.radar.RadarRepository

private const val MAXIMUM_SINGLE_IMAGE_DOWNLOADS = 10

internal class DefaultDownloadRadarDayTask(
    private val radarRepository: RadarRepository
) : DownloadRadarDayTask {

    // The archive costs as much data as the whole day, so it only replaces image requests when many are missing.
    override suspend fun invoke(imageUrls: List<String>): Either<Failure, Unit> =
        radarRepository
            .getMissingRadarImageUrls(imageUrls = imageUrls)
            .flatMap { missingImageUrls: List<String> ->
                if (missingImageUrls.size > MAXIMUM_SINGLE_IMAGE_DOWNLOADS) {
                    radarRepository.downloadRadarDays(imageUrls = imageUrls)
                } else {
                    downloadImages(imageUrls = missingImageUrls)
                }
            }

    private suspend fun downloadImages(imageUrls: List<String>): Either<Failure, Unit> {
        var downloaded: Either<Failure, Unit> = Unit.asRight()
        for (imageUrl in imageUrls) {
            if (downloaded is Either.Right) {
                downloaded = radarRepository.getRadarImage(imageUrl = imageUrl).flatMap { Unit.asRight() }
            }
        }
        return downloaded
    }
}