package se.vadretapp.vadret.warning

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import se.vadretapp.vadret.aurora.AuroraRepository
import se.vadretapp.vadret.aurora.model.AuroraCellForecast
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.geohash.model.Geohash

internal class FakeAuroraRepository(
    var result: Either<Failure, List<AuroraCellForecast>> = emptyList<AuroraCellForecast>().asRight()
) : AuroraRepository {

    val requests: MutableList<Pair<String, List<Geohash>>> = mutableListOf()

    override suspend fun getAuroraForecast(
        installationToken: String,
        geohashes: List<Geohash>
    ): Either<Failure, List<AuroraCellForecast>> {
        requests += installationToken to geohashes
        return result
    }
}