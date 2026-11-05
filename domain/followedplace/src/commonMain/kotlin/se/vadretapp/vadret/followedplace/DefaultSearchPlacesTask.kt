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
    override suspend fun invoke(prefix: String): Either<Failure, List<Place>> =
        if (prefix.isBlank()) {
            emptyList<Place>().asRight()
        } else {
            placeRepository.search(
                prefix = prefix,
                limit = SUGGESTION_LIMIT
            )
        }
}