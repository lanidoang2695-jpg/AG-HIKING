package com.example.data.model

data class GpsPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0, // meters above sea level
    val timestamp: Long = System.currentTimeMillis(),
    val speed: Float = 0f, // m/s
    val accuracy: Float = 0f, // meters
    val bearing: Float = 0f // degrees
)
