package se.vadretapp.vadret.warning.extension

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.onResult
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.aggregatedfeed.FeedRepository
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.model.SeenWarning
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.warning.model.WarningPlace
import kotlin.time.Instant

// A warning stays unseen until it is seen after it was last published, so an updated warning shows again.
internal fun FeedRepository.observeUnseenWarnings(
    warningPlaces: List<WarningPlace>,
    isRefresh: Boolean
): Flow<Either<Failure, List<AggregatedFeed>>> =
    flow {
        getAggregatedFeed(isRefresh = isRefresh)
            .onResult(
                onError = { failure: Failure -> emit(failure.asLeft()) },
                onSuccess = { aggregatedFeeds: List<AggregatedFeed> ->
                    emitAll(
                        observeUnseen(
                            affectingWarnings = aggregatedFeeds.filter { aggregatedFeed: AggregatedFeed ->
                                warningPlaces.any { warningPlace: WarningPlace -> aggregatedFeed.affects(warningPlace = warningPlace) }
                            }
                        )
                    )
                }
            )
    }.distinctUntilChanged()

private fun FeedRepository.observeUnseen(affectingWarnings: List<AggregatedFeed>): Flow<Either<Failure, List<AggregatedFeed>>> =
    observeSeenWarnings()
        .map { result: Either<Failure, List<SeenWarning>> ->
            result.map { seenWarnings: List<SeenWarning> ->
                val seenAtByWarningIdentifier: Map<String, Instant> = seenWarnings.associate { seenWarning: SeenWarning ->
                    seenWarning.warningIdentifier to seenWarning.seenAt
                }
                affectingWarnings.filterNot { aggregatedFeed: AggregatedFeed -> aggregatedFeed.isSeenSincePublished(seenAtByWarningIdentifier = seenAtByWarningIdentifier) }
            }
        }

private fun AggregatedFeed.isSeenSincePublished(seenAtByWarningIdentifier: Map<String, Instant>): Boolean {
    val seenAt: Instant = seenAtByWarningIdentifier[identifier] ?: return false
    return seenAt >= publishedAt
}