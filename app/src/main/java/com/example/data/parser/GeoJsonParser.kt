package com.example.data.parser

import com.example.data.local.entity.RouteEntity
import com.example.data.local.entity.WaypointEntity
import com.example.data.model.GpsPoint
import com.example.data.model.WaypointType
import com.example.util.GeoUtils
import org.json.JSONObject
import java.io.InputStream

object GeoJsonParser {
    fun parse(inputStream: InputStream, defaultName: String = "Imported GeoJSON"): ParsedGpxResult {
        val text = inputStream.bufferedReader().use { it.readText() }
        val root = JSONObject(text)
        val trackPoints = mutableListOf<GpsPoint>()
        val waypoints = mutableListOf<WaypointEntity>()
        var routeName = defaultName

        if (root.optString("type") == "FeatureCollection") {
            val features = root.optJSONArray("features")
            if (features != null) {
                for (i in 0 until features.length()) {
                    val feat = features.getJSONObject(i)
                    val geom = feat.optJSONObject("geometry") ?: continue
                    val props = feat.optJSONObject("properties")
                    val name = props?.optString("name", "") ?: ""
                    if (name.isNotEmpty() && routeName == defaultName) {
                        routeName = name
                    }

                    when (geom.optString("type")) {
                        "LineString" -> {
                            val coords = geom.optJSONArray("coordinates")
                            if (coords != null) {
                                for (j in 0 until coords.length()) {
                                    val pt = coords.getJSONArray(j)
                                    val lon = pt.getDouble(0)
                                    val lat = pt.getDouble(1)
                                    val alt = if (pt.length() > 2) pt.getDouble(2) else 0.0
                                    trackPoints.add(GpsPoint(latitude = lat, longitude = lon, altitude = alt))
                                }
                            }
                        }
                        "Point" -> {
                            val pt = geom.optJSONArray("coordinates")
                            if (pt != null) {
                                val lon = pt.getDouble(0)
                                val lat = pt.getDouble(1)
                                val alt = if (pt.length() > 2) pt.getDouble(2) else 0.0
                                val type = WaypointType.fromString(name)
                                waypoints.add(
                                    WaypointEntity(
                                        name = if (name.isNotEmpty()) name else "Waypoint ${waypoints.size + 1}",
                                        latitude = lat,
                                        longitude = lon,
                                        elevationMeters = alt,
                                        type = type.name,
                                        colorHex = type.defaultColorHex
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        if (trackPoints.isEmpty()) {
            throw IllegalArgumentException("Tidak ada LineString koordinat valid dalam GeoJSON.")
        }

        val totalDist = GeoUtils.calculateTotalDistance(trackPoints)
        val (gain, loss) = GeoUtils.calculateElevationGainLoss(trackPoints)
        val elevations = trackPoints.map { it.altitude }
        val highest = elevations.maxOrNull() ?: 0.0
        val lowest = elevations.minOrNull() ?: 0.0

        val estMinutes = (((totalDist / 1000.0) / 4.0 + gain / 600.0) * 60).toInt().coerceAtLeast(15)

        val route = RouteEntity(
            name = routeName,
            totalDistanceMeters = totalDist,
            elevationGainMeters = gain,
            elevationLossMeters = loss,
            highestPointMeters = highest,
            lowestPointMeters = lowest,
            estimatedHikingMinutes = estMinutes,
            difficulty = if (totalDist > 10000 || gain > 800) "Hard" else "Moderate",
            waypointsCount = waypoints.size,
            coordinatesJson = GeoUtils.pointsToJson(trackPoints),
            originalFormat = "GEOJSON"
        )

        return ParsedGpxResult(route, waypoints)
    }
}
