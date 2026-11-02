package se.vadretapp.vadret.nominatim

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.catch
import se.vadretapp.vadret.common.either
import se.vadretapp.vadret.nominatim.extension.toFailure
import se.vadretapp.vadret.nominatim.extension.toLocality
import se.vadretapp.vadret.nominatim.local.LocalityInMemoryDataSource
import se.vadretapp.vadret.nominatim.model.Locality
import se.vadretapp.vadret.nominatim.remote.NominatimRemoteDataSource

internal class DefaultNominatimRepository(
    private val nominatimRemoteDataSource: NominatimRemoteDataSource,
    private val localityInMemoryDataSource: LocalityInMemoryDataSource
) : NominatimRepository {

    override suspend fun getLocality(
        latitude: Double,
        longitude: Double
    ): Either<Failure, Locality> {
        val rememberedLocality: Locality? = localityInMemoryDataSource.locality(
            latitude = latitude,
            longitude = longitude
        )
        if (rememberedLocality != null) {
            return rememberedLocality.asRight()
        }
        return either {
            nominatimRemoteDataSource
                .reverse(
                    latitude = latitude,
                    longitude = longitude
                ).toLocality()
        }.catch { exception: Exception -> exception.toFailure() }
            .map { locality: Locality ->
                localityInMemoryDataSource.remember(
                    latitude = latitude,
                    longitude = longitude,
                    locality = locality
                )
                locality
            }
    }
}