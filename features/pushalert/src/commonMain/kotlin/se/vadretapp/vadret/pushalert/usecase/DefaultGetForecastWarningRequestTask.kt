package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.flatMap
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.first
import se.vadretapp.vadret.aggregatedfeed.model.ForecastWarningRequest
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.followedplace.MAXIMUM_FOLLOWED_PLACES
import se.vadretapp.vadret.followedplace.ObserveFollowedPlacesTask
import se.vadretapp.vadret.followedplace.model.FollowedPlace
import se.vadretapp.vadret.forecastpreference.ForecastPreferenceRepository
import se.vadretapp.vadret.forecastpreference.model.ForecastPreferences
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.geohash.model.geohashOf
import se.vadretapp.vadret.pushalert.PushAlertRepository
import se.vadretapp.vadret.pushalert.model.InstallationToken
import se.vadretapp.vadret.warning.usecase.GetForecastWarningRequestTask

internal class DefaultGetForecastWarningRequestTask(
    private val pushAlertRepository: PushAlertRepository,
    private val observeFollowedPlacesTask: ObserveFollowedPlacesTask,
    private val forecastPreferenceRepository: ForecastPreferenceRepository
) : GetForecastWarningRequestTask {

    override suspend fun invoke(): Either<Failure, ForecastWarningRequest?> =
        pushAlertRepository.installationToken().flatMap { installationToken: InstallationToken? ->
            if (installationToken == null) {
                null.asRight()
            } else {
                pushAlertRepository.syncedCurrentGeohash().flatMap { syncedCurrentGeohash: Geohash? ->
                    observeFollowedPlacesTask().first().flatMap { followedPlaces: List<FollowedPlace> ->
                        forecastPreferenceRepository.getForecastPreferences().map { forecastPreferences: ForecastPreferences ->
                            ForecastWarningRequest(
                                installationToken = installationToken.value,
                                geohashes = (listOfNotNull(syncedCurrentGeohash) + followedPlaces.toGeohashes()).toSwedishGeohashes(),
                                isAuroraIncluded = forecastPreferences.isAuroraEnabled
                            )
                        }
                    }
                }
            }
        }

    private fun List<FollowedPlace>.toGeohashes(): List<Geohash> =
        mapNotNull { followedPlace: FollowedPlace ->
            geohashOf(
                latitude = followedPlace.latitude,
                longitude = followedPlace.longitude
            )
        }

    private fun List<Geohash>.toSwedishGeohashes(): List<Geohash> =
        filter { geohash: Geohash -> geohash.isInSwedishWaters() }
            .distinct()
            .take(n = MAXIMUM_FOLLOWED_PLACES)
            .sortedBy { geohash: Geohash -> geohash.value }
}