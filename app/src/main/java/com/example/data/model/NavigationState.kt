package com.example.data.model

data class NavigationState(
    val isNavigating: Boolean = false,
    val targetRouteName: String = "",
    val isOnTrack: Boolean = true,
    val crossTrackDistanceMeters: Double = 0.0,
    val bearingToRouteDegrees: Float = 0f,
    val nearestRouteLat: Double = 0.0,
    val nearestRouteLon: Double = 0.0,
    val distanceToDestinationMeters: Double = 0.0,
    val remainingRouteDistanceMeters: Double = 0.0,
    val elevationDifferenceMeters: Double = 0.0,
    val nextWaypointName: String = "",
    val nextWaypointDistanceMeters: Double = 0.0,
    val nextWaypointBearing: Float = 0f,
    val estimatedTimeRemainingSeconds: Long = 0,
    val offRouteAlert: Boolean = false
)
