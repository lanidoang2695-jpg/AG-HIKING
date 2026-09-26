package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.HikeTrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HikeTrackDao {
    @Query("SELECT * FROM hike_tracks ORDER BY startTime DESC")
    fun getAllTracks(): Flow<List<HikeTrackEntity>>

    @Query("SELECT * FROM hike_tracks WHERE id = :id")
    suspend fun getTrackById(id: Long): HikeTrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: HikeTrackEntity): Long

    @Update
    suspend fun updateTrack(track: HikeTrackEntity)

    @Query("DELETE FROM hike_tracks WHERE id = :id")
    suspend fun deleteTrackById(id: Long)

    @Query("DELETE FROM hike_tracks")
    suspend fun deleteAllTracks()
}
