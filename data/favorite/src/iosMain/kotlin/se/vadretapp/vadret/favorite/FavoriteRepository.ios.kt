package se.vadretapp.vadret.favorite

import androidx.room3.Room
import androidx.sqlite.driver.NativeSQLiteDriver
import kotlinx.coroutines.CoroutineDispatcher
import se.vadretapp.vadret.favorite.local.FavoriteDatabase
import se.vadretapp.vadret.idgenerator.IdGenerator
import kotlin.time.Clock

fun FavoriteRepository(
    databaseDirectory: String,
    idGenerator: IdGenerator,
    clock: Clock,
    coroutineDispatcher: CoroutineDispatcher
): FavoriteRepository =
    FavoriteRepository(
        databaseBuilder = Room
            .databaseBuilder<FavoriteDatabase>(name = "$databaseDirectory/$FAVORITE_DATABASE_NAME")
            .setDriver(driver = NativeSQLiteDriver())
            .setQueryCoroutineContext(context = coroutineDispatcher),
        idGenerator = idGenerator,
        clock = clock
    )