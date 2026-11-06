package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pushalert.model.PushAlertPlace
import se.vadretapp.vadret.pushalert.usecase.ObservePushAlertPlacesTask

internal class FakeObservePushAlertPlacesTask(places: List<PushAlertPlace>) : ObservePushAlertPlacesTask {
    val places = MutableStateFlow(value = places)

    override fun invoke(): Flow<Either<Failure, List<PushAlertPlace>>> = places.map { pushAlertPlaces: List<PushAlertPlace> -> pushAlertPlaces.asRight() }
}