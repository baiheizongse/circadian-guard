package jp.circadianguard.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Which way an observation deviated from its baseline. */
enum class DetectionDirection { BRIGHTER, DIMMER }

/**
 * A recorded deviation from the baseline for a place/hour/day-type cell.
 * Corresponds to `DetectionEvent` in spec §3.
 */
@Entity(tableName = "detection_events")
data class DetectionEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "ts") val ts: Long,
    @ColumnInfo(name = "cluster_id") val clusterId: Long,
    @ColumnInfo(name = "hour_bucket") val hourBucket: Int,
    @ColumnInfo(name = "observed_lux") val observedLux: Float,
    @ColumnInfo(name = "baseline_mean") val baselineMean: Float,
    @ColumnInfo(name = "baseline_std") val baselineStd: Float,
    @ColumnInfo(name = "direction") val direction: DetectionDirection,
)
