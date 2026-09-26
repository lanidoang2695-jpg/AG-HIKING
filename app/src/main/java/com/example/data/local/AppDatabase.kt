package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.HikeTrackDao
import com.example.data.local.dao.OfflineRegionDao
import com.example.data.local.dao.RouteDao
import com.example.data.local.dao.WaypointDao
import com.example.data.local.entity.HikeTrackEntity
import com.example.data.local.entity.OfflineRegionEntity
import com.example.data.local.entity.RouteEntity
import com.example.data.local.entity.WaypointEntity

@Database(
    entities = [
        HikeTrackEntity::class,
        RouteEntity::class,
        WaypointEntity::class,
        OfflineRegionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hikeTrackDao(): HikeTrackDao
    abstract fun routeDao(): RouteDao
    abstract fun waypointDao(): WaypointDao
    abstract fun offlineRegionDao(): OfflineRegionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aghiking_database.db"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
