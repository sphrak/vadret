package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.place.PlaceRepository
import se.vadretapp.vadret.place.model.Place

private const val SUGGESTION_LIMIT = 10

internal class DefaultSearchPlacesTask(
    private val placeRepository: PlaceRepository
) : SearchPlacesTask {

    // An empty search finds nothing, so it never reaches the places.
    override suspend fun invoke(query: String): Either<Failure, List<Place>> =
        if (query.isBlank()) {
            emptyList<Place>().asRight()
        } else {
            placeRepository.search(
                query = query,
                limit = SUGGESTION_LIMIT
            )
        }
}