package com.example.util

import com.example.data.model.GpsPoint
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.*

object GeoUtils {
    const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates great-circle distance between two coordinates in meters (Haversine).
     */
    fun calculateDistanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    fun calculateTotalDistance(points: List<GpsPoint>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until points.size - 1) {
            total += calculateDistanceMeters(
                points[i].latitude, points[i].longitude,
                points[i + 1].latitude, points[i + 1].longitude
            )
        }
        return total
    }

    /**
     * Initial bearing from point 1 to point 2 in degrees (0..360).
     */
    fun calculateBearing(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val theta = atan2(y, x)
        val degrees = Math.toDegrees(theta)
        return ((degrees + 360) % 360).toFloat()
    }

    /**
     * Cardinal direction name from bearing (0..360)
     */
    fun bearingToCardinal(bearing: Float): String {
        val directions = arrayOf("U", "TL", "T", "TG", "S", "BD", "B", "BL") // N, NE, E, SE, S, SW, W, NW in ID
        val index = (((bearing + 22.5f) % 360) / 45).toInt()
        return directions[index.coerceIn(0, 7)]
    }

    /**
     * Elevation gain and loss with minimum threshold to avoid GPS vertical jitter.
     */
    fun calculateElevationGainLoss(points: List<GpsPoint>, thresholdMeters: Double = 3.0): Pair<Double, Double> {
        if (points.size < 2) return Pair(0.0, 0.0)
        var gain = 0.0
        var loss = 0.0
        var lastValidAlt = points.first().altitude

        for (i in 1 until points.size) {
            val currentAlt = points[i].altitude
            val diff = currentAlt - lastValidAlt
            if (abs(diff) >= thresholdMeters) {
                if (diff > 0) gain += diff else loss += abs(diff)
                lastValidAlt = currentAlt
            }
        }
        return Pair(gain, loss)
    }

    /**
     * Finds nearest point on a polyline track and calculates perpendicular/cross-track distance.
     */
    data class OffRouteInfo(
        val distanceToRouteMeters: Double,
        val nearestPoint: GpsPoint,
        val nearestSegmentIndex: Int,
        val bearingToRouteDegrees: Float
    )

    fun calculateOffRoute(currentLat: Double, currentLon: Double, routePoints: List<GpsPoint>): OffRouteInfo? {
        if (routePoints.isEmpty()) return null
        if (routePoints.size == 1) {
            val dist = calculateDistanceMeters(currentLat, currentLon, routePoints[0].latitude, routePoints[0].longitude)
            val bearing = calculateBearing(currentLat, currentLon, routePoints[0].latitude, routePoints[0].longitude)
            return OffRouteInfo(dist, routePoints[0], 0, bearing)
        }

        var minDistance = Double.MAX_VALUE
        var bestPoint = routePoints[0]
        var bestIndex = 0

        // Check distance to each segment and project current point onto segment
        for (i in 0 until routePoints.size - 1) {
            val p1 = routePoints[i]
            val p2 = routePoints[i + 1]

            val projected = projectPointOnSegment(currentLat, currentLon, p1.latitude, p1.longitude, p2.latitude, p2.longitude)
            val dist = calculateDistanceMeters(currentLat, currentLon, projected.latitude, projected.longitude)
            if (dist < minDistance) {
                minDistance = dist
                bestPoint = projected
                bestIndex = i
            }
        }

        val bearingToRoute = calculateBearing(currentLat, currentLon, bestPoint.latitude, bestPoint.longitude)
        return OffRouteInfo(minDistance, bestPoint, bestIndex, bearingToRoute)
    }

    private fun projectPointOnSegment(
        lat: Double, lon: Double,
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): GpsPoint {
        val dx = lon2 - lon1
        val dy = lat2 - lat1
        if (dx == 0.0 && dy == 0.0) return GpsPoint(lat1, lon1)

        val u = ((lon - lon1) * dx + (lat - lat1) * dy) / (dx * dx + dy * dy)
        return when {
            u <= 0.0 -> GpsPoint(lat1, lon1)
            u >= 1.0 -> GpsPoint(lat2, lon2)
            else -> GpsPoint(lat1 + u * dy, lon1 + u * dx)
        }
    }

    /**
     * Calculates area of a closed polygon in square meters.
     */
    fun calculatePolygonAreaMeters(coords: List<Pair<Double, Double>>): Double {
        if (coords.size < 3) return 0.0
        var totalArea = 0.0
        val n = coords.size
        for (i in 0 until n) {
            val j = (i + 1) % n
            val p1 = coords[i]
            val p2 = coords[j]
            val lat1Rad = Math.toRadians(p1.first)
            val lat2Rad = Math.toRadians(p2.first)
            val deltaLonRad = Math.toRadians(p2.second - p1.second)
            totalArea += deltaLonRad * (2 + sin(lat1Rad) + sin(lat2Rad))
        }
        totalArea = abs(totalArea * EARTH_RADIUS_METERS * EARTH_RADIUS_METERS / 4.0)
        return totalArea
    }

    /**
     * Converts Decimal Degrees to DMS formatted string.
     */
    fun toDms(latitude: Double, longitude: Double): String {
        fun formatCoord(value: Double, posChar: Char, negChar: Char): String {
            val hemisphere = if (value >= 0) posChar else negChar
            val absVal = abs(value)
            val degrees = absVal.toInt()
            val minutesDecimal = (absVal - degrees) * 60
            val minutes = minutesDecimal.toInt()
            val seconds = (minutesDecimal - minutes) * 60
            return "%d°%02d'%04.1f\"%c".format(degrees, minutes, seconds, hemisphere)
        }

        return "${formatCoord(latitude, 'N', 'S')}, ${formatCoord(longitude, 'E', 'W')}"
    }

    /**
     * Approximate UTM Zone calculation for coordinates.
     */
    fun toUtmString(latitude: Double, longitude: Double): String {
        val zone = ((longitude + 180) / 6).toInt() + 1
        val hemisphere = if (latitude >= 0) "N" else "S"
        // Simplified easting/northing approximation for display
        val centralMeridian = (zone - 1) * 6 - 180 + 3
        val deltaLon = Math.toRadians(longitude - centralMeridian)
        val latRad = Math.toRadians(latitude)
        val easting = (500000 + EARTH_RADIUS_METERS * deltaLon * cos(latRad)).toInt()
        val northing = (if (latitude >= 0) EARTH_RADIUS_METERS * latRad else 10000000 + EARTH_RADIUS_METERS * latRad).toInt()
        return "Zone $zone$hemisphere ${easting}m E ${northing}m N"
    }

    // JSON serialization
    fun pointsToJson(points: List<GpsPoint>): String {
        val array = JSONArray()
        for (p in points) {
            val obj = JSONObject()
            obj.put("lat", p.latitude)
            obj.put("lon", p.longitude)
            obj.put("alt", p.altitude)
            obj.put("time", p.timestamp)
            obj.put("spd", p.speed.toDouble())
            obj.put("acc", p.accuracy.toDouble())
            obj.put("brg", p.bearing.toDouble())
            array.put(obj)
        }
        return array.toString()
    }

    fun jsonToPoints(json: String): List<GpsPoint> {
        val list = mutableListOf<GpsPoint>()
        if (json.isBlank()) return list
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    GpsPoint(
                        latitude = obj.optDouble("lat", 0.0),
                        longitude = obj.optDouble("lon", 0.0),
                        altitude = obj.optDouble("alt", 0.0),
                        timestamp = obj.optLong("time", 0L),
                        speed = obj.optDouble("spd", 0.0).toFloat(),
                        accuracy = obj.optDouble("acc", 0.0).toFloat(),
                        bearing = obj.optDouble("brg", 0.0).toFloat()
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
