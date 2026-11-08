package se.vadretapp.vadret.warning.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.flatMap
import io.github.sphrak.either.map
import se.vadretapp.vadret.aggregatedfeed.FeedRepository
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.model.ForecastWarningRequest
import se.vadretapp.vadret.aggregatedfeed.model.WarningFeed
import se.vadretapp.vadret.aggregatedfeed.model.WarningReference
import se.vadretapp.vadret.common.Failure

internal class DefaultGetReferencedWarningIdentifiersTask(
    private val feedRepository: FeedRepository,
    private val getForecastWarningRequestTask: GetForecastWarningRequestTask
) : GetReferencedWarningIdentifiersTask {

    override suspend fun invoke(warningReference: WarningReference): Either<Failure, List<String>> {
        val forecastWarningRequest: ForecastWarningRequest? = getForecastWarningRequestTask.forecastWarningRequestOrNull()
        return feedRepository.getAggregatedFeed(forecastWarningRequest = forecastWarningRequest).flatMap { aggregatedFeeds: List<AggregatedFeed> ->
            val savedIdentifiers: List<String> = aggregatedFeeds.identifiersReferencedBy(warningReference = warningReference)
            if (savedIdentifiers.isEmpty()) {
                feedRepository.refreshAggregatedFeed(forecastWarningRequest = forecastWarningRequest).map { warningFeed: WarningFeed ->
                    warningFeed.aggregatedFeeds.identifiersReferencedBy(warningReference = warningReference)
                }
            } else {
                savedIdentifiers.asRight()
            }
        }
    }
}

private fun List<AggregatedFeed>.identifiersReferencedBy(warningReference: WarningReference): List<String> =
    map { aggregatedFeed: AggregatedFeed -> aggregatedFeed.identifier }
        .filter { warningIdentifier: String -> warningReference.isReferenceTo(warningIdentifier = warningIdentifier) }