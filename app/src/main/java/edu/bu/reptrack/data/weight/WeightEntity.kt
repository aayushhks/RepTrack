package edu.bu.reptrack.data.weight

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Table row for one weigh-in. */
@Entity(tableName = "weights")
data class WeightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "logged_at") val loggedAt: Long,
    @ColumnInfo(name = "weight_kg") val weightKg: Float,
)
