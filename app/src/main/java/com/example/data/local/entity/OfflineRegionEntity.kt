package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_regions")
data class OfflineRegionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
    val minZoom: Int,
    val maxZoom: Int,
    val layerType: String,
    val totalTiles: Int,
    val downloadedTiles: Int,
    val sizeBytes: Long,
    val status: String, // COMPLETED, DOWNLOADING, PAUSED, ERROR
    val createdAt: Long = System.currentTimeMillis()
)
