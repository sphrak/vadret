package se.vadretapp.vadret.aggregatedfeed.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

private const val AGGREGATED_FEED_DATABASE_VERSION = 1

@Database(
    entities = [SeenWarningEntity::class],
    version = AGGREGATED_FEED_DATABASE_VERSION,
    exportSchema = false
)
@ConstructedBy(value = AggregatedFeedDatabaseConstructor::class)
internal abstract class AggregatedFeedDatabase : RoomDatabase() {
    abstract fun seenWarningDao(): SeenWarningDao
}

@Suppress("KotlinNoActualForExpect")
internal expect object AggregatedFeedDatabaseConstructor : RoomDatabaseConstructor<AggregatedFeedDatabase> {
    override fun initialize(): AggregatedFeedDatabase
}