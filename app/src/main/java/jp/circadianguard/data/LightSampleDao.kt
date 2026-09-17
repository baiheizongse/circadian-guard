package jp.circadianguard.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LightSampleDao {

    @Insert
    suspend fun insert(sample: LightSample)

    @Query("SELECT * FROM light_samples WHERE ts >= :startTs AND ts < :endTs ORDER BY ts ASC")
    suspend fun getSamplesBetween(startTs: Long, endTs: Long): List<LightSample>

    @Query("SELECT * FROM light_samples ORDER BY ts DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<LightSample>
}
