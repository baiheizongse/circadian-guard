package jp.circadianguard.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DetectionEventDao {

    @Insert
    suspend fun insert(event: DetectionEvent): Long

    @Query("SELECT * FROM detection_events ORDER BY ts DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<DetectionEvent>>
}
