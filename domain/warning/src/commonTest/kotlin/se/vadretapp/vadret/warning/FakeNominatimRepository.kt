package se.vadretapp.vadret.warning

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.nominatim.NominatimRepository
import se.vadretapp.vadret.nominatim.model.Locality

internal class FakeNominatimRepository(
    var locality: Either<Failure, Locality> = Locality(
        name = "Funäsdalen",
        municipality = "Härjedalen",
        county = "Jämtland"
    ).asRight()
) : NominatimRepository {

    override suspend fun getLocality(
        latitude: Double,
        longitude: Double
    ): Either<Failure, Locality> = locality
}