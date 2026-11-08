package se.vadretapp.vadret.place

import android.content.Context
import androidx.room3.Room
import se.vadretapp.vadret.place.local.PlaceDatabase

fun PlaceRepository(
    context: Context,
    readPlaces: suspend () -> String
): PlaceRepository =
    PlaceRepository(
        databaseBuilder = Room.databaseBuilder<PlaceDatabase>(
            context = context,
            name = PLACE_DATABASE_NAME
        ),
        readPlaces = readPlaces
    )