package edu.bu.reptrack.data.run

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * The path of one run, in its own table so the history list never loads routes it does not
 * draw. One row per run, keyed by the run's id; deleting a run deletes its route (CASCADE).
 */
@Entity(
    tableName = "run_routes",
    foreignKeys = [
        ForeignKey(
            entity = RunEntity::class,
            parentColumns = ["id"],
            childColumns = ["run_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class RunRouteEntity(
    @PrimaryKey @ColumnInfo(name = "run_id") val runId: Long,
    /** The points in Google's encoded polyline format, see [edu.bu.reptrack.domain.run.PolylineCodec]. */
    val polyline: String,
)

/** A run with its route, read in one go. The route is null for runs saved before routes were kept. */
data class RunWithRoute(
    @Embedded val run: RunEntity,
    @Relation(parentColumn = "id", entityColumn = "run_id") val route: RunRouteEntity?,
)
