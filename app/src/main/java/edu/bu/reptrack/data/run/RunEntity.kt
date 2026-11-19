package edu.bu.reptrack.data.run

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Table row for one finished walk or run. Only totals: the route is never stored. */
@Entity(tableName = "runs")
data class RunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "finished_at") val finishedAt: Long,
    @ColumnInfo(name = "distance_m") val distanceM: Double,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    val calories: Int,
)
