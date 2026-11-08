package se.vadretapp.vadret.place.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity

@Entity(
    tableName = "place",
    primaryKeys = ["locality", "municipality"]
)
internal data class PlaceEntity(
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
    @ColumnInfo(name = "population")
    val population: Int,
    @ColumnInfo(name = "folded_locality")
    val foldedLocality: String
)