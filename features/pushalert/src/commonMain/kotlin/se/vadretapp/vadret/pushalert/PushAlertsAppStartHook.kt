package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.onResult
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.followedplace.ObserveFollowedPlacesTask
import se.vadretapp.vadret.followedplace.model.FollowedPlace
import se.vadretapp.vadret.lifecycle.AppStartHook
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.pushalert.usecase.ObserveAuroraEnabledTask
import se.vadretapp.vadret.pushalert.usecase.ObservePollenAllergensTask
import se.vadretapp.vadret.pushalert.usecase.SyncPushAlertsTask
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.setting.model.Language
import se.vadretapp.vadret.setting.usecase.ObserveHourFormatTask
import se.vadretapp.vadret.setting.usecase.ObserveShownLanguageTask

internal class PushAlertsAppStartHook(
    private val syncPushAlertsTask: SyncPushAlertsTask,
    private val observeFollowedPlacesTask: ObserveFollowedPlacesTask,
    private val observePollenAllergensTask: ObservePollenAllergensTask,
    private val observeShownLanguageTask: ObserveShownLanguageTask,
    private val observeHourFormatTask: ObserveHourFormatTask,
    private val observeAuroraEnabledTask: ObserveAuroraEnabledTask
) : AppStartHook {

    // The server learns of a place chosen or saved, an allergen picked, a language or clock chosen, or the aurora lab
    // turned on or off, anywhere in the app or on the phone, not only on the push alert settings, so the notifications
    // it writes follow them. The first sync waits for all of them, so a change made while it runs is already followed
    // and syncs again.
    override suspend fun onAppStart() {
        combine(
            flow = observeFollowedPlacesTask()
                .map { followedPlaces: Either<Failure, List<FollowedPlace>> ->
                    followedPlaces.map { places: List<FollowedPlace> -> places.coordinates() }
                }.distinctUntilChanged(),
            flow2 = observePollenAllergensTask().distinctUntilChanged(),
            flow3 = observeShownLanguageTask().distinctUntilChanged(),
            flow4 = observeHourFormatTask().distinctUntilChanged(),
            flow5 = observeAuroraEnabledTask().distinctUntilChanged()
        ) {
                _: Either<Failure, List<Pair<Double, Double>>>,
                _: Either<Failure, Set<String>>,
                _: Either<Failure, Language>,
                _: Either<Failure, HourFormat>,
                _: Either<Failure, Boolean>
            ->
        }.collect { sync() }
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