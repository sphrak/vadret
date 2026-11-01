package se.vadretapp.vadret.favorite.local

import androidx.room3.RoomDatabase
import androidx.sqlite.driver.NativeSQLiteDriver

internal actual fun <T : RoomDatabase> RoomDatabase.Builder<T>.forTests(): RoomDatabase.Builder<T> = setDriver(driver = NativeSQLiteDriver())