package se.vadretapp.vadret.radar

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.radar.precipitation.RadarEchoes

internal interface RadarImageLoader {

    suspend fun load(imageUrl: String): Either<Failure, RadarEchoes>
}