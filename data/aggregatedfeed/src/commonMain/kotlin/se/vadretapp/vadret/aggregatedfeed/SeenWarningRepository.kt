package se.vadretapp.vadret.aggregatedfeed

import androidx.room3.RoomDatabase
import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.aggregatedfeed.local.AggregatedFeedDatabase
import se.vadretapp.vadret.aggregatedfeed.local.DefaultSeenWarningLocalDataSource
import se.vadretapp.vadret.aggregatedfeed.model.SeenWarning
import se.vadretapp.vadret.common.Failure
import kotlin.time.Clock

internal const val AGGREGATED_FEED_DATABASE_NAME = "aggregatedfeed.db"

interface SeenWarningRepository {

    fun observeSeenWarnings(): Flow<Either<Failure, List<SeenWarning>>>

    suspend fun markSeen(warningIdentifier: String): Either<Failure, Unit>
}

internal fun SeenWarningRepository(
    databaseBuilder: RoomDatabase.Builder<AggregatedFeedDatabase>,
    clock: Clock
): SeenWarningRepository =
    DefaultSeenWarningRepository(
        seenWarningLocalDataSource = DefaultSeenWarningLocalDataSource(aggregatedFeedDatabase = databaseBuilder.build()),
        clock = clock
    )