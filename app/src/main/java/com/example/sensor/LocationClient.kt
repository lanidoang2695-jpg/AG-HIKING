package com.example.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.example.data.model.GpsPoint
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class GpsAccuracyLevel {
    EXCELLENT, // < 5m
    GOOD,      // 5m - 15m
    FAIR,      // 15m - 35m
    POOR       // > 35m
}

data class CurrentGpsState(
    val point: GpsPoint? = null,
    val isGpsEnabled: Boolean = false,
    val accuracyLevel: GpsAccuracyLevel = GpsAccuracyLevel.POOR,
    val satellitesCount: Int = 0,
    val provider: String = "GPS"
)

class LocationClient(private val context: Context) {
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    fun isGpsEnabled(): Boolean {
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
    }

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(intervalMs: Long = 2000L): Flow<CurrentGpsState> = callbackFlow {
        var lastState = CurrentGpsState(isGpsEnabled = isGpsEnabled())

        // 1. Setup FusedLocationProviderClient
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .setMinUpdateDistanceMeters(1.0f)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    val point = mapLocationToGpsPoint(loc)
                    val accLevel = when {
                        loc.accuracy < 6f -> GpsAccuracyLevel.EXCELLENT
                        loc.accuracy < 16f -> GpsAccuracyLevel.GOOD
                        loc.accuracy < 36f -> GpsAccuracyLevel.FAIR
                        else -> GpsAccuracyLevel.POOR
                    }
                    lastState = CurrentGpsState(
                        point = point,
                        isGpsEnabled = isGpsEnabled(),
                        accuracyLevel = accLevel,
                        provider = loc.provider ?: "FusedGPS"
                    )
                    trySend(lastState)
                }
            }

            override fun onLocationAvailability(avail: LocationAvailability) {
                lastState = lastState.copy(isGpsEnabled = avail.isLocationAvailable)
                trySend(lastState)
            }
        }

        // 2. Fallback Android standard LocationManager listener
        val standardListener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                val point = mapLocationToGpsPoint(loc)
                val accLevel = when {
                    loc.accuracy < 6f -> GpsAccuracyLevel.EXCELLENT
                    loc.accuracy < 16f -> GpsAccuracyLevel.GOOD
                    loc.accuracy < 36f -> GpsAccuracyLevel.FAIR
                    else -> GpsAccuracyLevel.POOR
                }
                lastState = CurrentGpsState(
                    point = point,
                    isGpsEnabled = isGpsEnabled(),
                    accuracyLevel = accLevel,
                    provider = loc.provider ?: "GPS_Raw"
                )
                trySend(lastState)
            }

            override fun onProviderEnabled(provider: String) {
                lastState = lastState.copy(isGpsEnabled = true)
                trySend(lastState)
            }

            override fun onProviderDisabled(provider: String) {
                lastState = lastState.copy(isGpsEnabled = false)
                trySend(lastState)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        try {
            fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (_: Exception) {
            // Fallback to standard provider
            try {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    intervalMs,
                    1.0f,
                    standardListener,
                    Looper.getMainLooper()
                )
            } catch (_: Exception) {}
        }

        awaitClose {
            try {
                fusedClient.removeLocationUpdates(locationCallback)
            } catch (_: Exception) {}
            try {
                locationManager?.removeUpdates(standardListener)
            } catch (_: Exception) {}
        }
    }

    private fun mapLocationToGpsPoint(loc: Location): GpsPoint {
        return GpsPoint(
            latitude = loc.latitude,
            longitude = loc.longitude,
            altitude = loc.altitude,
            timestamp = loc.time,
            speed = loc.speed,
            accuracy = loc.accuracy,
            bearing = loc.bearing
        )
    }
}
