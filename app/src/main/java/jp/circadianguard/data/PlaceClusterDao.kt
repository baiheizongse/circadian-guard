package jp.circadianguard.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceClusterDao {

    @Insert
    suspend fun insert(cluster: PlaceCluster): Long

    @Delete
    suspend fun delete(cluster: PlaceCluster)

    @Query("SELECT * FROM place_clusters ORDER BY created_at ASC")
    fun getAll(): Flow<List<PlaceCluster>>
}
