package se.vadretapp.vadret.place

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.catch
import se.vadretapp.vadret.common.either
import se.vadretapp.vadret.place.extension.toFailure
import se.vadretapp.vadret.place.extension.toPlace
import se.vadretapp.vadret.place.local.PlaceEntity
import se.vadretapp.vadret.place.local.PlaceLocalDataSource
import se.vadretapp.vadret.place.model.Place

internal class DefaultPlaceRepository(
    private val placeLocalDataSource: PlaceLocalDataSource
) : PlaceRepository {

    override suspend fun search(
        query: String,
        limit: Int
    ): Either<Failure, List<Place>> =
        either {
            placeLocalDataSource
                .search(
                    query = query,
                    limit = limit
                ).map { placeEntity: PlaceEntity -> placeEntity.toPlace() }
        }.catch { exception: Exception -> exception.toFailure() }
}