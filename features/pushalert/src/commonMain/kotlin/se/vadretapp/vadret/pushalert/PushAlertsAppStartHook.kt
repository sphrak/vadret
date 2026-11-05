package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.onResult
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.followedplace.ObserveFollowedPlacesTask
import se.vadretapp.vadret.followedplace.model.FollowedPlace
import se.vadretapp.vadret.lifecycle.AppStartHook
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.pushalert.usecase.SyncPushAlertsTask

internal class PushAlertsAppStartHook(
    private val syncPushAlertsTask: SyncPushAlertsTask,
    private val observeFollowedPlacesTask: ObserveFollowedPlacesTask
) : AppStartHook {

    // The server learns of a place chosen or saved anywhere in the app, not only on the push alert settings.
    override suspend fun onAppStart() {
        sync()
        observeFollowedPlacesTask()
            .map { followedPlaces: Either<Failure, List<FollowedPlace>> -> followedPlaces.map { places: List<FollowedPlace> -> places.coordinates() } }
            .distinctUntilChanged()
            .drop(count = 1)
            .collect { sync() }
    }

    private suspend fun sync() {
        syncPushAlertsTask().onResult(
            onError = { failure: Failure -> logger.warn { "Push alerts could not be brought up to date: ${failure.errorCode}" } },
            onSuccess = { logger.debug { "Push alerts are up to date" } }
        )
    }
}

private fun List<FollowedPlace>.coordinates(): List<Pair<Double, Double>> = map { followedPlace: FollowedPlace -> followedPlace.latitude to followedPlace.longitude }

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.pushalert")