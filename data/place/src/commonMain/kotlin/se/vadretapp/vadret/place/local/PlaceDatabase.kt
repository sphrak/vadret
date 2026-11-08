package se.vadretapp.vadret.place.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

private const val PLACE_DATABASE_VERSION = 1

@Database(
    entities = [
        PlaceEntity::class,
        PlaceSeedEntity::class
    ],
    version = PLACE_DATABASE_VERSION,
    exportSchema = false
)
@ConstructedBy(value = PlaceDatabaseConstructor::class)
internal abstract class PlaceDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao
}

@Suppress("KotlinNoActualForExpect")
internal expect object PlaceDatabaseConstructor : RoomDatabaseConstructor<PlaceDatabase> {
    override fun initialize(): PlaceDatabase
}