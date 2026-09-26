package com.example.data.parser

import android.util.Xml
import com.example.data.local.entity.RouteEntity
import com.example.data.local.entity.WaypointEntity
import com.example.data.model.GpsPoint
import com.example.data.model.WaypointType
import com.example.util.GeoUtils
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

object KmlParser {
    fun parse(inputStream: InputStream, defaultName: String = "Imported KML"): ParsedGpxResult {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        val trackPoints = mutableListOf<GpsPoint>()
        val waypoints = mutableListOf<WaypointEntity>()
        var routeName = defaultName

        var eventType = parser.eventType
        var currentTag = ""
        var insideCoordinates = false
        var insidePlacemark = false
        var placemarkName = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name.lowercase()
                    when (currentTag) {
                        "placemark" -> {
                            insidePlacemark = true
                            placemarkName = ""
                        }
                        "coordinates" -> insideCoordinates = true
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim() ?: ""
                    if (text.isNotEmpty()) {
                        if (currentTag == "name" && insidePlacemark && placemarkName.isEmpty()) {
                            placemarkName = text
                        } else if (currentTag == "name" && routeName == defaultName) {
                            routeName = text
                        } else if (insideCoordinates) {
                            // KML coordinates format: lon,lat,alt lon,lat,alt ...
                            val tuples = text.split("\\s+".toRegex())
                            for (t in tuples) {
                                val parts = t.split(",")
                                if (parts.size >= 2) {
                                    val lon = parts[0].toDoubleOrNull()
                                    val lat = parts[1].toDoubleOrNull()
                                    val alt = if (parts.size > 2) parts[2].toDoubleOrNull() ?: 0.0 else 0.0
                                    if (lat != null && lon != null && GpxParser.isValidCoordinate(lat, lon)) {
                                        trackPoints.add(GpsPoint(latitude = lat, longitude = lon, altitude = alt))
                                    }
                                }
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val endTag = parser.name.lowercase()
                    if (endTag == "coordinates") insideCoordinates = false
                    if (endTag == "placemark") {
                        if (trackPoints.isNotEmpty() && placemarkName.isNotEmpty() && waypoints.isEmpty()) {
                            val wptType = WaypointType.fromString(placemarkName)
                            val pt = trackPoints.last()
                            waypoints.add(
                                WaypointEntity(
                                    name = placemarkName,
                                    latitude = pt.latitude,
                                    longitude = pt.longitude,
                                    elevationMeters = pt.altitude,
                                    type = wptType.name,
                                    colorHex = wptType.defaultColorHex
                                )
                            )
                        }
                        insidePlacemark = false
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        if (trackPoints.isEmpty()) {
            throw IllegalArgumentException("Tidak ada koordinat valid ditemukan dalam file KML.")
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
            originalFormat = "KML"
        )

        return ParsedGpxResult(route, waypoints)
    }
}
