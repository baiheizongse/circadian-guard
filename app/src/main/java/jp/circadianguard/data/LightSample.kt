package jp.circadianguard.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/** Where a sample came from. M1 uses FOREGROUND only (BACKGROUND comes in M2+). */
enum class SampleSource { FOREGROUND, BACKGROUND }

/**
 * A light sample, corresponding to `LightSample` in spec §3.
 *
 * @param ts sampling timestamp (epoch ms)
 * @param clusterId location cluster id (null = unclassified)
 * @param lux illuminance value (lx)
 * @param screenBright screen brightness setting (0-255, null if unavailable)
 * @param source where the sample came from (FOREGROUND / BACKGROUND)
 */
@Entity(tableName = "light_samples")
data class LightSample(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "ts") val ts: Long,
    @ColumnInfo(name = "cluster_id") val clusterId: Long? = null,
    @ColumnInfo(name = "lux") val lux: Float,
    @ColumnInfo(name = "screen_brightness") val screenBright: Int? = null,
    @ColumnInfo(name = "source") val source: SampleSource = SampleSource.FOREGROUND,
)

class Converters {
    @TypeConverter
    fun sourceToString(source: SampleSource): String = source.name

    @TypeConverter
    fun stringToSource(value: String): SampleSource = SampleSource.valueOf(value)

    @TypeConverter
    fun dayTypeToString(dayType: DayType): String = dayType.name

    @TypeConverter
    fun stringToDayType(value: String): DayType = DayType.valueOf(value)

    @TypeConverter
    fun directionToString(direction: DetectionDirection): String = direction.name

    @TypeConverter
    fun stringToDirection(value: String): DetectionDirection = DetectionDirection.valueOf(value)
}
