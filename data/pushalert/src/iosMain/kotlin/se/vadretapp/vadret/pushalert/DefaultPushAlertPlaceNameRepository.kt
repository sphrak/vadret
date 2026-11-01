package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.catch
import se.vadretapp.vadret.common.either
import se.vadretapp.vadret.pushalert.extension.toFailure
import se.vadretapp.vadret.pushalert.local.PushAlertPlaceNameLocalDataSource
import se.vadretapp.vadret.pushalert.model.PushAlertPlaceNamesEntity

internal class DefaultPushAlertPlaceNameRepository(
    private val pushAlertPlaceNameLocalDataSource: PushAlertPlaceNameLocalDataSource
) : PushAlertPlaceNameRepository {
    override suspend fun savePlaceNames(placeNamesByCell: Map<String, String>): Either<Failure, Unit> =
        either {
            pushAlertPlaceNameLocalDataSource.save(pushAlertPlaceNamesEntity = PushAlertPlaceNamesEntity(placeNamesByCell = placeNamesByCell))
        }.catch { exception: Exception -> exception.toFailure() }
}