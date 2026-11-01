package se.vadretapp.vadret.favorite.local

import androidx.room3.Room
import kotlinx.coroutines.CoroutineDispatcher

internal fun inMemoryFavoriteDatabase(coroutineDispatcher: CoroutineDispatcher): FavoriteDatabase =
    Room
        .inMemoryDatabaseBuilder<FavoriteDatabase>()
        .setQueryCoroutineContext(context = coroutineDispatcher)
        .forTests()
        .build()