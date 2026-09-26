package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.WaypointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaypointDao {
    @Query("SELECT * FROM waypoints ORDER BY createdAt DESC")
    fun getAllWaypoints(): Flow<List<WaypointEntity>>

    @Query("SELECT * FROM waypoints WHERE routeId = :routeId ORDER BY createdAt ASC")
    fun getWaypointsForRoute(routeId: Long): Flow<List<WaypointEntity>>

    @Query("SELECT * FROM waypoints WHERE routeId = :routeId ORDER BY createdAt ASC")
    suspend fun getWaypointsForRouteSync(routeId: Long): List<WaypointEntity>

    @Query("SELECT * FROM waypoints WHERE id = :id")
    suspend fun getWaypointById(id: Long): WaypointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoint(waypoint: WaypointEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoints(waypoints: List<WaypointEntity>)

    @Update
    suspend fun updateWaypoint(waypoint: WaypointEntity)

    @Query("DELETE FROM waypoints WHERE id = :id")
    suspend fun deleteWaypointById(id: Long)

    @Query("DELETE FROM waypoints WHERE routeId = :routeId")
    suspend fun deleteWaypointsForRoute(routeId: Long)
}
