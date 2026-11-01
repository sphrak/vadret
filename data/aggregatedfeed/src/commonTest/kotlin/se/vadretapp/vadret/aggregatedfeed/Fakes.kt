package se.vadretapp.vadret.aggregatedfeed

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import se.vadretapp.vadret.aggregatedfeed.local.SeenWarningEntity
import se.vadretapp.vadret.aggregatedfeed.local.SeenWarningLocalDataSource
import se.vadretapp.vadret.aggregatedfeed.remote.KrisinformationFeedDto
import se.vadretapp.vadret.aggregatedfeed.remote.KrisinformationRemoteDataSource
import se.vadretapp.vadret.aggregatedfeed.remote.SmhiWarningDto
import se.vadretapp.vadret.aggregatedfeed.remote.SmhiWarningRemoteDataSource

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

internal class EmptySeenWarningLocalDataSource : SeenWarningLocalDataSource {
    override fun observeSeenWarnings(): Flow<List<SeenWarningEntity>> = flowOf(emptyList())
    override suspend fun markSeen(seenWarningEntity: SeenWarningEntity) = Unit
}

internal class FailingSeenWarningLocalDataSource(
    private val exception: Exception
) : SeenWarningLocalDataSource {
    override fun observeSeenWarnings(): Flow<List<SeenWarningEntity>> = flow { throw exception }
    override suspend fun markSeen(seenWarningEntity: SeenWarningEntity) = throw exception
}