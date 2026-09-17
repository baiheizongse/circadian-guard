package jp.circadianguard.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        LightSample::class,
        PlaceCluster::class,
        BaselineStat::class,
        DetectionEvent::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun lightSampleDao(): LightSampleDao
    abstract fun placeClusterDao(): PlaceClusterDao
    abstract fun baselineStatDao(): BaselineStatDao
    abstract fun detectionEventDao(): DetectionEventDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "circadian_guard.db",
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
