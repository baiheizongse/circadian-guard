package jp.circadianguard.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BaselineStatDao {

    @Query(
        "SELECT * FROM baseline_stats " +
            "WHERE cluster_id = :clusterId AND hour_bucket = :hourBucket AND day_type = :dayType " +
            "LIMIT 1",
    )
    suspend fun get(clusterId: Long, hourBucket: Int, dayType: DayType): BaselineStat?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stat: BaselineStat)
}
