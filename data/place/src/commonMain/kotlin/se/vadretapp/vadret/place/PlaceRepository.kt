package se.vadretapp.vadret.place

import androidx.room3.RoomDatabase
import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.place.local.DefaultPlaceLocalDataSource
import se.vadretapp.vadret.place.local.PlaceDatabase
import se.vadretapp.vadret.place.model.Place

internal const val PLACE_DATABASE_NAME = "place.db"

interface PlaceRepository {

    // Places named as the query, then starting with it, then with a word starting with it, the most populated first.
    suspend fun search(
        query: String,
        limit: Int
    ): Either<Failure, List<Place>>
}

internal fun PlaceRepository(
    databaseBuilder: RoomDatabase.Builder<PlaceDatabase>,
    readPlaces: suspend () -> String
): PlaceRepository =
    DefaultPlaceRepository(
        placeLocalDataSource = DefaultPlaceLocalDataSource(
            placeDatabase = databaseBuilder.build(),
            readPlaces = readPlaces
        )
    )