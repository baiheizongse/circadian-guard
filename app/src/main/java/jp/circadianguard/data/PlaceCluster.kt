package jp.circadianguard.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A manually registered place ("Home", "Work", ...) used to label light samples.
 * Corresponds to `PlaceCluster` in spec §3.
 *
 * @param id primary key
 * @param name display name
 * @param lat center latitude (coarse)
 * @param lng center longitude (coarse)
 * @param radiusM matching radius in meters
 * @param createdAt registration timestamp (epoch ms)
 */
@Entity(tableName = "place_clusters")
data class PlaceCluster(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "lat") val lat: Double,
    @ColumnInfo(name = "lng") val lng: Double,
    @ColumnInfo(name = "radius_m") val radiusM: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
