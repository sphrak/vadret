package se.vadretapp.vadret.aurora

import io.github.sphrak.either.Either
import se.vadretapp.vadret.aurora.extension.toAuroraCellForecasts
import se.vadretapp.vadret.aurora.extension.toFailure
import se.vadretapp.vadret.aurora.model.AuroraCellForecast
import se.vadretapp.vadret.aurora.remote.AuroraRemoteDataSource
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.catch
import se.vadretapp.vadret.common.either
import se.vadretapp.vadret.geohash.model.Geohash

internal class DefaultAuroraRepository(
    private val auroraRemoteDataSource: AuroraRemoteDataSource
) : AuroraRepository {

    override suspend fun getAuroraForecast(
        installationToken: String,
        geohashes: List<Geohash>
    ): Either<Failure, List<AuroraCellForecast>> =
        either {
            auroraRemoteDataSource
                .getAuroraForecast(
                    installationToken = installationToken,
                    geohashes = geohashes
                ).toAuroraCellForecasts()
        }.catch { exception: Exception -> exception.toFailure() }
}