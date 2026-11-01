package se.vadretapp.vadret.favorite.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

private const val FAVORITE_DATABASE_VERSION = 1

@Database(
    entities = [FavoriteEntity::class],
    version = FAVORITE_DATABASE_VERSION,
    exportSchema = false
)
@ConstructedBy(value = FavoriteDatabaseConstructor::class)
internal abstract class FavoriteDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
}

@Suppress("KotlinNoActualForExpect")
internal expect object FavoriteDatabaseConstructor : RoomDatabaseConstructor<FavoriteDatabase> {
    override fun initialize(): FavoriteDatabase
}