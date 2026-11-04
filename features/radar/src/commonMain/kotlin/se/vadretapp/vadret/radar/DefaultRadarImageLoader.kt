package se.vadretapp.vadret.radar

import coil3.Image
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.flatMap
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.radar.exception.TiffFormatException
import se.vadretapp.vadret.radar.extension.toFailure
import se.vadretapp.vadret.radar.precipitation.RadarEchoes
import se.vadretapp.vadret.radar.projection.RadarReprojection

internal class DefaultRadarImageLoader(
    private val platformContext: PlatformContext,
    private val radarRepository: RadarRepository,
    radarReprojection: RadarReprojection
) : RadarImageLoader {

    private val imageLoader: ImageLoader = ImageLoader.Builder(context = platformContext)
        .components {
            add(TiffRadarDecoderFactory(radarReprojection = radarReprojection))
        }
        .diskCache(diskCache = null)
        .build()

    override suspend fun load(imageUrl: String): Either<Failure, RadarEchoes> =
        radarRepository
            .getRadarImage(imageUrl = imageUrl)
            .flatMap { image: ByteArray ->
                decode(
                    imageUrl = imageUrl,
                    image = image
                )
            }

    private suspend fun decode(
        imageUrl: String,
        image: ByteArray
    ): Either<Failure, RadarEchoes> {
        val request: ImageRequest = ImageRequest.Builder(context = platformContext)
            .data(data = image)
            .memoryCacheKey(key = imageUrl)
            .build()
        return when (val result: ImageResult = imageLoader.execute(request = request)) {
            is SuccessResult -> result.image.toRadarEchoes(imageUrl = imageUrl)
            is ErrorResult -> result.throwable.toFailure().asLeft()
        }
    }

    private fun Image.toRadarEchoes(imageUrl: String): Either<Failure, RadarEchoes> =
        if (this is RadarEchoesImage) {
            radarEchoes.asRight()
        } else {
            TiffFormatException(message = "$imageUrl is not a radar image").toFailure().asLeft()
        }
}