package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hike_tracks")
data class HikeTrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val totalDistanceMeters: Double,
    val durationSeconds: Long,
    val elevationGainMeters: Double,
    val elevationLossMeters: Double,
    val maxAltitudeMeters: Double,
    val minAltitudeMeters: Double,
    val avgSpeedKmh: Double,
    val maxSpeedKmh: Double,
    val pointsJson: String, // serialized List<GpsPoint>
    val note: String = ""
)
