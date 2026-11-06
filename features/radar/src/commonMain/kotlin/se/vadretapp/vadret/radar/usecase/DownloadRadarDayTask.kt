package se.vadretapp.vadret.radar.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure

internal interface DownloadRadarDayTask {
    suspend operator fun invoke(imageUrls: List<String>): Either<Failure, Unit>
}