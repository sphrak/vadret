package se.vadretapp.vadret.favorite.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "favorite",
    indices = [
        Index(
            value = ["locality", "municipality", "county"],
            unique = true
        )
    ]
)
internal data class FavoriteEntity(
    @PrimaryKey
    @ColumnInfo(name = "favorite_id")
    val favoriteId: String,
    @ColumnInfo(name = "locality")
    val locality: String,
    @ColumnInfo(name = "municipality")
    val municipality: String,
    @ColumnInfo(name = "county")
    val county: String,
    @ColumnInfo(name = "latitude")
    val latitude: Double,
    @ColumnInfo(name = "longitude")
    val longitude: Double,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)