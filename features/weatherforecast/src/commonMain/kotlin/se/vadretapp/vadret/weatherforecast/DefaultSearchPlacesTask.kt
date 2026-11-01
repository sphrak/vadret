package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.place.PlaceRepository
import se.vadretapp.vadret.place.model.Place

private const val SUGGESTION_LIMIT = 10

internal class DefaultSearchPlacesTask(
    private val placeRepository: PlaceRepository
) : SearchPlacesTask {

    override suspend fun invoke(prefix: String): Either<Failure, List<Place>> =
        placeRepository.search(
            prefix = prefix,
            limit = SUGGESTION_LIMIT
        )
}