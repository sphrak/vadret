package se.vadretapp.vadret.favorite

import androidx.room3.RoomDatabase
import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.local.DefaultFavoriteLocalDataSource
import se.vadretapp.vadret.favorite.local.FavoriteDatabase
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.idgenerator.IdGenerator
import kotlin.time.Clock

internal const val FAVORITE_DATABASE_NAME = "favorite.db"

interface FavoriteRepository {

    fun observeFavorites(): Flow<Either<Failure, List<Favorite>>>

    suspend fun addFavorite(
        locality: String,
        municipality: String,
        county: String,
        latitude: Double,
        longitude: Double
    ): Either<Failure, Favorite>

    suspend fun removeFavorite(favoriteId: String): Either<Failure, Unit>
}

internal fun FavoriteRepository(
    databaseBuilder: RoomDatabase.Builder<FavoriteDatabase>,
    idGenerator: IdGenerator,
    clock: Clock
): FavoriteRepository =
    DefaultFavoriteRepository(
        favoriteLocalDataSource = DefaultFavoriteLocalDataSource(favoriteDatabase = databaseBuilder.build()),
        idGenerator = idGenerator,
        clock = clock
    )