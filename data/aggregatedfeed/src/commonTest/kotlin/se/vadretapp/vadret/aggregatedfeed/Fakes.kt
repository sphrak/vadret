package se.vadretapp.vadret.aggregatedfeed

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import se.vadretapp.vadret.aggregatedfeed.local.SeenWarningEntity
import se.vadretapp.vadret.aggregatedfeed.local.SeenWarningLocalDataSource
import se.vadretapp.vadret.aggregatedfeed.remote.ForecastWarningRemoteDataSource
import se.vadretapp.vadret.aggregatedfeed.remote.ForecastWarningsDto
import se.vadretapp.vadret.aggregatedfeed.remote.KrisinformationFeedDto
import se.vadretapp.vadret.aggregatedfeed.remote.KrisinformationRemoteDataSource
import se.vadretapp.vadret.aggregatedfeed.remote.SmhiWarningDto
import se.vadretapp.vadret.aggregatedfeed.remote.SmhiWarningRemoteDataSource
import se.vadretapp.vadret.geohash.model.Geohash

internal class FakeSmhiWarningRemoteDataSource(
    var smhiWarningDtos: List<SmhiWarningDto> = emptyList()
) : SmhiWarningRemoteDataSource {

    var requestCount: Int = 0

    override suspend fun getWarnings(): List<SmhiWarningDto> {
        requestCount++
        return smhiWarningDtos
    }
}

internal class FakeKrisinformationRemoteDataSource(
    var news: () -> List<KrisinformationFeedDto> = { emptyList() },
    var publicAlerts: () -> List<KrisinformationFeedDto> = { emptyList() }
) : KrisinformationRemoteDataSource {

    override suspend fun getNews(): List<KrisinformationFeedDto> = news()

    override suspend fun getPublicAlerts(): List<KrisinformationFeedDto> = publicAlerts()
}

internal class FakeForecastWarningRemoteDataSource(
    var forecastWarnings: () -> ForecastWarningsDto = { ForecastWarningsDto(warnings = emptyList()) }
) : ForecastWarningRemoteDataSource {

    val requests: MutableList<Pair<String, List<Geohash>>> = mutableListOf()

    override suspend fun getWarnings(
        installationToken: String,
        geohashes: List<Geohash>
    ): ForecastWarningsDto {
        requests += installationToken to geohashes
        return forecastWarnings()
    }
}

internal class FailingSeenWarningLocalDataSource(
    private val exception: Exception
) : SeenWarningLocalDataSource {
    override fun observeSeenWarnings(): Flow<List<SeenWarningEntity>> = flow { throw exception }
    override suspend fun markSeen(seenWarningEntity: SeenWarningEntity) = throw exception
}