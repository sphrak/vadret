package se.vadretapp.vadret.favorite

import android.content.Context
import androidx.room3.Room
import se.vadretapp.vadret.favorite.local.FavoriteDatabase
import se.vadretapp.vadret.idgenerator.IdGenerator
import kotlin.time.Clock

fun FavoriteRepository(
    context: Context,
    idGenerator: IdGenerator,
    clock: Clock
): FavoriteRepository =
    FavoriteRepository(
        databaseBuilder = Room.databaseBuilder<FavoriteDatabase>(
            context = context,
            name = FAVORITE_DATABASE_NAME
        ),
        idGenerator = idGenerator,
        clock = clock
    )