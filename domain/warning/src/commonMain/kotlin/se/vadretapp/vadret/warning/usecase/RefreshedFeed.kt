package se.vadretapp.vadret.warning.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.flatMap
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.aggregatedfeed.FeedRepository
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.model.SeenWarning
import se.vadretapp.vadret.aggregatedfeed.model.WarningFeed
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.refresh.whileRefreshing
import se.vadretapp.vadret.warning.extension.affects
import se.vadretapp.vadret.warning.model.WarningPlace
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private val feedReuseAge: Duration = 5.minutes

internal fun FeedRepository.observeRefreshedFeed(
    isRefresh: Boolean,
    clock: Clock
): Flow<Either<Failure, List<AggregatedFeed>>> =
    observeAggregatedFeed()
        .whileRefreshing(
            refresh = {
                refreshUnlessRecent(
                    isRefresh = isRefresh,
                    clock = clock
                )
            },
            isNothingSaved = { savedFeed: WarningFeed? -> savedFeed == null },
            whenRefreshFails = { savedFeed: WarningFeed? -> savedFeed },
            isRefreshFirst = isRefresh
        ).map { savedFeed: Either<Failure, WarningFeed?> ->
            savedFeed.map { saved: WarningFeed? -> saved?.aggregatedFeeds.orEmpty() }
        }

// A warning stays unseen until it is seen after it was last published, so an updated warning shows again.
internal fun FeedRepository.observeUnseenWarnings(
    warningPlaces: List<WarningPlace>,
    isRefresh: Boolean,
    clock: Clock
): Flow<Either<Failure, List<AggregatedFeed>>> =
    combine(
        flow = observeRefreshedFeed(
            isRefresh = isRefresh,
            clock = clock
        ),
        flow2 = observeSeenWarnings()
    ) { aggregatedFeeds: Either<Failure, List<AggregatedFeed>>, seenWarnings: Either<Failure, List<SeenWarning>> ->
        aggregatedFeeds.flatMap { feeds: List<AggregatedFeed> ->
            seenWarnings.map { seen: List<SeenWarning> ->
                feeds
                    .filter { aggregatedFeed: AggregatedFeed -> warningPlaces.any { warningPlace: WarningPlace -> aggregatedFeed.affects(warningPlace = warningPlace) } }
                    .unseen(seenWarnings = seen)
            }
        }
    }.distinctUntilChanged()

private suspend fun FeedRepository.refreshUnlessRecent(
    isRefresh: Boolean,
    clock: Clock
): Either<Failure, WarningFeed?> {
    if (!isRefresh) {
        val savedFeed: Either<Failure, WarningFeed?> = observeAggregatedFeed().first()
        if (savedFeed is Either.Right && savedFeed.b.isRecent(now = clock.now())) {
            return savedFeed
        }
    }
    return refreshAggregatedFeed()
}

private fun WarningFeed?.isRecent(now: Instant): Boolean {
    if (this == null) {
        return false
    }
    val age: Duration = now - fetchedAt
    return age >= Duration.ZERO && age < feedReuseAge
}

private fun List<AggregatedFeed>.unseen(seenWarnings: List<SeenWarning>): List<AggregatedFeed> {
    val seenAtByWarningIdentifier: Map<String, Instant> = seenWarnings.associate { seenWarning: SeenWarning ->
        seenWarning.warningIdentifier to seenWarning.seenAt
    }
    return filterNot { aggregatedFeed: AggregatedFeed -> aggregatedFeed.isSeenSincePublished(seenAtByWarningIdentifier = seenAtByWarningIdentifier) }
}

private fun AggregatedFeed.isSeenSincePublished(seenAtByWarningIdentifier: Map<String, Instant>): Boolean {
    val seenAt: Instant = seenAtByWarningIdentifier[identifier] ?: return false
    return seenAt >= publishedAt
}