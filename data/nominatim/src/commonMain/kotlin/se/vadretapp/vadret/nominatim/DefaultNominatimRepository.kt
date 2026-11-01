package se.vadretapp.vadret.nominatim

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.catch
import se.vadretapp.vadret.common.either
import se.vadretapp.vadret.nominatim.extension.toFailure
import se.vadretapp.vadret.nominatim.extension.toLocality
import se.vadretapp.vadret.nominatim.model.Locality
import se.vadretapp.vadret.nominatim.remote.NominatimRemoteDataSource

internal class DefaultNominatimRepository(
    private val nominatimRemoteDataSource: NominatimRemoteDataSource
) : NominatimRepository {

    override suspend fun getLocality(
        latitude: Double,
        longitude: Double
    ): Either<Failure, Locality> =
        either {
            nominatimRemoteDataSource
                .reverse(
                    latitude = latitude,
                    longitude = longitude
                ).toLocality()
        }.catch { exception: Exception -> exception.toFailure() }
}