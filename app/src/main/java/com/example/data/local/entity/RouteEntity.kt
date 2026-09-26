package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hiking_routes")
data class RouteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val totalDistanceMeters: Double,
    val elevationGainMeters: Double,
    val elevationLossMeters: Double,
    val highestPointMeters: Double,
    val lowestPointMeters: Double,
    val estimatedHikingMinutes: Int,
    val difficulty: String, // Easy, Moderate, Hard, Strenuous
    val waypointsCount: Int,
    val coordinatesJson: String, // serialized List<GpsPoint>
    val originalFormat: String, // GPX, KML, GEOJSON, MANUAL
    val isFavorite: Boolean = false,
    val importedAt: Long = System.currentTimeMillis()
)
