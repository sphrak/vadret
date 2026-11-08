package se.vadretapp.vadret.aggregatedfeed

import android.content.Context
import androidx.room3.Room
import se.vadretapp.vadret.aggregatedfeed.local.AggregatedFeedDatabase
import kotlin.time.Clock

fun SeenWarningRepository(
    context: Context,
    clock: Clock
): SeenWarningRepository =
    SeenWarningRepository(
        databaseBuilder = Room.databaseBuilder<AggregatedFeedDatabase>(
            context = context,
            name = AGGREGATED_FEED_DATABASE_NAME
        ),
        clock = clock
    )