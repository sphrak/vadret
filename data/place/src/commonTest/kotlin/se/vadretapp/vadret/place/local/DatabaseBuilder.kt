package se.vadretapp.vadret.place.local

import androidx.room3.RoomDatabase

internal expect fun <T : RoomDatabase> RoomDatabase.Builder<T>.forTests(): RoomDatabase.Builder<T>