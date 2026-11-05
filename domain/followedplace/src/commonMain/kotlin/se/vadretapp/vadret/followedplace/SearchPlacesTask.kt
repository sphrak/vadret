package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.place.PlaceRepository
import se.vadretapp.vadret.place.model.Place

interface SearchPlacesTask {

    suspend operator fun invoke(prefix: String): Either<Failure, List<Place>>
}

fun SearchPlacesTask(placeRepository: PlaceRepository): SearchPlacesTask = DefaultSearchPlacesTask(placeRepository = placeRepository)