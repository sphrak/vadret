package se.vadretapp.vadret.aggregatedfeed.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "seen_warning")
internal data class SeenWarningEntity(
    @PrimaryKey
    @ColumnInfo(name = "warning_identifier")
    val warningIdentifier: String,
    @ColumnInfo(name = "seen_at_epoch_millis")
    val seenAtEpochMillis: Long
)