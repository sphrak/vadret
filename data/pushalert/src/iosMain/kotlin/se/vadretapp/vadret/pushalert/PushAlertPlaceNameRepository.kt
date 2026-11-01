package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import kotlinx.coroutines.CoroutineDispatcher
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pushalert.local.DefaultPushAlertPlaceNameLocalDataSource

// The Notification Service Extension cannot open the favorites, so it reads the place names to show from here.
interface PushAlertPlaceNameRepository {
    suspend fun savePlaceNames(placeNamesByCell: Map<String, String>): Either<Failure, Unit>
}

fun PushAlertPlaceNameRepository(
    directoryPath: String,
    ioDispatcher: CoroutineDispatcher
): PushAlertPlaceNameRepository =
    DefaultPushAlertPlaceNameRepository(
        pushAlertPlaceNameLocalDataSource = DefaultPushAlertPlaceNameLocalDataSource(
            directoryPath = directoryPath,
            ioDispatcher = ioDispatcher
        )
    )