package se.vadretapp.vadret.place

import androidx.room3.Room
import androidx.sqlite.driver.NativeSQLiteDriver
import kotlinx.coroutines.CoroutineDispatcher
import se.vadretapp.vadret.place.local.PlaceDatabase

fun PlaceRepository(
    databaseDirectory: String,
    readPlaces: suspend () -> String,
    coroutineDispatcher: CoroutineDispatcher
): PlaceRepository =
    PlaceRepository(
        databaseBuilder = Room
            .databaseBuilder<PlaceDatabase>(name = "$databaseDirectory/$PLACE_DATABASE_NAME")
            .setDriver(driver = NativeSQLiteDriver())
            .setQueryCoroutineContext(context = coroutineDispatcher),
        readPlaces = readPlaces
    )