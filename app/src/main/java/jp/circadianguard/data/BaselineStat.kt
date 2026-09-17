package jp.circadianguard.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import java.util.Calendar

/** Day classification used for baseline cells. */
enum class DayType {
    WEEKDAY, WEEKEND;

    companion object {
        fun from(calendar: Calendar): DayType {
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            return if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                WEEKEND
            } else {
                WEEKDAY
            }
        }
    }
}

/**
 * Running baseline statistics for one (place x hour x day-type) cell.
 * Corresponds to `BaselineStat` in spec §3.
 *
 * Composite key: (clusterId, hourBucket, dayType).
 *
 * @param m2 Welford sum of squared deviations (used to derive the standard deviation)
 * @param n number of samples incorporated
 */
@Entity(
    tableName = "baseline_stats",
    primaryKeys = ["cluster_id", "hour_bucket", "day_type"],
)
data class BaselineStat(
    @ColumnInfo(name = "cluster_id") val clusterId: Long,
    @ColumnInfo(name = "hour_bucket") val hourBucket: Int,
    @ColumnInfo(name = "day_type") val dayType: DayType,
    @ColumnInfo(name = "mean") val mean: Float,
    @ColumnInfo(name = "m2") val m2: Float,
    @ColumnInfo(name = "n") val n: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
