package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import se.vadretapp.vadret.common.Failure

internal class FakePushAlertPlaceNameRepository(
    private val failure: Failure? = null
) : PushAlertPlaceNameRepository {
    val savedPlaceNames = mutableListOf<Map<String, String>>()

    override suspend fun savePlaceNames(placeNamesByCell: Map<String, String>): Either<Failure, Unit> {
        savedPlaceNames += placeNamesByCell
        return failure?.asLeft() ?: Unit.asRight()
    }
}