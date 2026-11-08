package se.vadretapp.vadret.place.local

import androidx.room3.Room
import kotlinx.coroutines.CoroutineDispatcher

internal fun inMemoryPlaceDatabase(coroutineDispatcher: CoroutineDispatcher): PlaceDatabase =
    Room
        .inMemoryDatabaseBuilder<PlaceDatabase>()
        .setQueryCoroutineContext(context = coroutineDispatcher)
        .forTests()
        .build()