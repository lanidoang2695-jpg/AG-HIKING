package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "waypoints")
data class WaypointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val routeId: Long? = null,
    val name: String,
    val description: String = "",
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double = 0.0,
    val type: String, // from WaypointType enum
    val colorHex: String = "#E53935",
    val photoUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
