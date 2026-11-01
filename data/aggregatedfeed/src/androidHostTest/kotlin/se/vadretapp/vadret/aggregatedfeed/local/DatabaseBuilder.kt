package se.vadretapp.vadret.aggregatedfeed.local

import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

internal actual fun <T : RoomDatabase> RoomDatabase.Builder<T>.forTests(): RoomDatabase.Builder<T> =
    setDriver(driver = BundledSQLiteDriver())
        .allowMainThreadQueries()