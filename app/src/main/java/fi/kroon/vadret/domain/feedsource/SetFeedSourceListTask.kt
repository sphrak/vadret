package fi.kroon.vadret.domain.feedsource

import fi.kroon.vadret.data.failure.Failure
import fi.kroon.vadret.data.feedsource.local.FeedSourceDao
import fi.kroon.vadret.data.feedsource.mapper.FeedSourceEntityMapper
import fi.kroon.vadret.data.feedsource.model.FeedSource
import fi.kroon.vadret.data.feedsource.model.FeedSourceEntity
import fi.kroon.vadret.util.extension.asRight
import io.github.sphrak.either.Either
import io.reactivex.Single
import javax.inject.Inject

class SetFeedSourceListTask @Inject constructor(
    private val localDao: FeedSourceDao
) {
    operator fun invoke(feedSourceList: List<FeedSource>): Single<Either<Failure, List<Long>>> {
        val feedSourceEntityList: List<FeedSourceEntity> = FeedSourceEntityMapper(feedSourceList)
        return localDao
            .insert(entityList = feedSourceEntityList)
            .map { ids: List<Long> ->
                ids.asRight()
            }
    }
}