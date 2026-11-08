package se.vadretapp.vadret.place.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "place_seed")
internal data class PlaceSeedEntity(
    @PrimaryKey
    @ColumnInfo(name = "seed_version")
    val seedVersion: String
)