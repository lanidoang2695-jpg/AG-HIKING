package com.example.data.parser

import android.util.Xml
import com.example.data.local.entity.RouteEntity
import com.example.data.local.entity.WaypointEntity
import com.example.data.model.GpsPoint
import com.example.data.model.WaypointType
import com.example.util.GeoUtils
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class ParsedGpxResult(
    val route: RouteEntity,
    val waypoints: List<WaypointEntity>
)

object GpxParser {
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun parse(inputStream: InputStream, defaultName: String = "Imported GPX"): ParsedGpxResult {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        val trackPoints = mutableListOf<GpsPoint>()
        val waypoints = mutableListOf<WaypointEntity>()
        var routeName = defaultName
        var routeDesc = ""

        var eventType = parser.eventType
        var currentTag = ""
        var currentLat: Double? = null
        var currentLon: Double? = null
        var currentEle: Double? = null
        var currentTime: Long? = null
        var currentWptName: String? = null
        var currentWptDesc: String? = null
        var currentWptSym: String? = null

        var insideTrackPoint = false
        var insideRoutePoint = false
        var insideWaypoint = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name.lowercase()
                    when (currentTag) {
                        "trkpt" -> {
                            insideTrackPoint = true
                            currentLat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                            currentLon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                            currentEle = null
                            currentTime = null
                        }
                        "rtept" -> {
                            insideRoutePoint = true
                            currentLat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                            currentLon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                            currentEle = null
                            currentTime = null
                        }
                        "wpt" -> {
                            insideWaypoint = true
                            currentLat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                            currentLon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                            currentEle = null
                            currentWptName = null
                            currentWptDesc = null
                            currentWptSym = null
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim() ?: ""
                    if (text.isNotEmpty()) {
                        when {
                            insideWaypoint -> {
                                when (currentTag) {
                                    "name" -> currentWptName = text
                                    "desc" -> currentWptDesc = text
                                    "ele" -> currentEle = text.toDoubleOrNull()
                                    "sym", "type" -> currentWptSym = text
                                }
                            }
                            insideTrackPoint || insideRoutePoint -> {
                                when (currentTag) {
                                    "ele" -> currentEle = text.toDoubleOrNull()
                                    "time" -> {
                                        try {
                                            currentTime = isoDateFormat.parse(text)?.time
                                        } catch (_: Exception) {}
                                    }
                                }
                            }
                            currentTag == "name" && routeName == defaultName -> {
                                routeName = text
                            }
                            currentTag == "desc" && routeDesc.isEmpty() -> {
                                routeDesc = text
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val endTag = parser.name.lowercase()
                    when (endTag) {
                        "trkpt", "rtept" -> {
                            if (currentLat != null && currentLon != null && isValidCoordinate(currentLat, currentLon)) {
                                trackPoints.add(
                                    GpsPoint(
                                        latitude = currentLat,
                                        longitude = currentLon,
                                        altitude = currentEle ?: 0.0,
                                        timestamp = currentTime ?: System.currentTimeMillis()
                                    )
                                )
                            }
                            insideTrackPoint = false
                            insideRoutePoint = false
                        }
                        "wpt" -> {
                            if (currentLat != null && currentLon != null && isValidCoordinate(currentLat, currentLon)) {
                                val wptType = WaypointType.fromString(currentWptSym ?: currentWptName)
                                waypoints.add(
                                    WaypointEntity(
                                        name = currentWptName ?: "Waypoint ${waypoints.size + 1}",
                                        description = currentWptDesc ?: "",
                                        latitude = currentLat,
                                        longitude = currentLon,
                                        elevationMeters = currentEle ?: 0.0,
                                        type = wptType.name,
                                        colorHex = wptType.defaultColorHex
                                    )
                                )
                            }
                            insideWaypoint = false
                        }
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        if (trackPoints.isEmpty() && waypoints.isNotEmpty()) {
            // Fallback: connect waypoints if no track was recorded
            for (w in waypoints) {
                trackPoints.add(GpsPoint(w.latitude, w.longitude, w.elevationMeters))
            }
        }

        if (trackPoints.isEmpty()) {
            throw IllegalArgumentException("Tidak ada koordinat valid ditemukan dalam file GPX.")
        }

        val totalDist = GeoUtils.calculateTotalDistance(trackPoints)
        val (gain, loss) = GeoUtils.calculateElevationGainLoss(trackPoints)
        val elevations = trackPoints.map { it.altitude }
        val highest = elevations.maxOrNull() ?: 0.0
        val lowest = elevations.minOrNull() ?: 0.0

        // Estimated hiking time: Naismith's rule (4 km/h horizontal + 1 hour per 600m ascent)
        val horizontalHours = (totalDist / 1000.0) / 4.0
        val ascentHours = gain / 600.0
        val totalHours = horizontalHours + ascentHours
        val estMinutes = (totalHours * 60).toInt().coerceAtLeast(15)

        val difficulty = when {
            totalDist > 15000 || gain > 1200 -> "Strenuous (Sangat Berat)"
            totalDist > 8000 || gain > 600 -> "Hard (Berat)"
            totalDist > 4000 || gain > 250 -> "Moderate (Sedang)"
            else -> "Easy (Ringan)"
        }

        val route = RouteEntity(
            name = routeName,
            description = routeDesc,
            totalDistanceMeters = totalDist,
            elevationGainMeters = gain,
            elevationLossMeters = loss,
            highestPointMeters = highest,
            lowestPointMeters = lowest,
            estimatedHikingMinutes = estMinutes,
            difficulty = difficulty,
            waypointsCount = waypoints.size,
            coordinatesJson = GeoUtils.pointsToJson(trackPoints),
            originalFormat = "GPX"
        )

        return ParsedGpxResult(route, waypoints)
    }

    fun isValidCoordinate(lat: Double, lon: Double): Boolean {
        return lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0 && (lat != 0.0 || lon != 0.0)
    }
}
