package se.vadretapp.vadret.place

import io.github.sphrak.either.Either
import kotlinx.coroutines.CoroutineDispatcher
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.place.local.DefaultPlaceLocalDataSource
import se.vadretapp.vadret.place.model.Place

interface PlaceRepository {

    suspend fun search(
        prefix: String,
        limit: Int
    ): Either<Failure, List<Place>>
}

fun PlaceRepository(
    readPlaces: suspend () -> String,
    coroutineDispatcher: CoroutineDispatcher
): PlaceRepository =
    DefaultPlaceRepository(
        placeLocalDataSource = DefaultPlaceLocalDataSource(
            readPlaces = readPlaces,
            coroutineDispatcher = coroutineDispatcher
        )
    )