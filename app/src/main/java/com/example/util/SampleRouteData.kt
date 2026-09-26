package com.example.util

import com.example.data.local.entity.RouteEntity
import com.example.data.local.entity.WaypointEntity
import com.example.data.model.GpsPoint
import com.example.data.model.WaypointType

object SampleRouteData {

    data class SampleWaypoint(
        val name: String,
        val lat: Double,
        val lon: Double,
        val alt: Double,
        val type: WaypointType,
        val desc: String
    )

    val bawakaraengWaypoints = listOf(
        SampleWaypoint("Basecamp Buluballea", -5.2974, 119.9021, 1540.0, WaypointType.BASECAMP, "Titik registrasi awal & parkir"),
        SampleWaypoint("Pos 1 - Hutan Pinus", -5.2932, 119.9102, 1680.0, WaypointType.POS, "Area teduh hutan pinus"),
        SampleWaypoint("Pos 2 - Sumber Air Lembah", -5.2891, 119.9174, 1840.0, WaypointType.WATER, "Aliran mata air jernih dan segar"),
        SampleWaypoint("Pos 3 - Tanjakan Batu", -5.2842, 119.9248, 1990.0, WaypointType.POS, "Medan bebatuan licin berlumut"),
        SampleWaypoint("Pos 4 - Shelter Kayu", -5.2810, 119.9320, 2120.0, WaypointType.SHELTER, "Pondok istirahat sementara"),
        SampleWaypoint("Pos 5 - Camp Area Bunga", -5.2801, 119.9392, 2240.0, WaypointType.CAMP, "Area mendirikan tenda luas"),
        SampleWaypoint("Pos 6 - Hutan Lumut", -5.2818, 119.9455, 2360.0, WaypointType.POS, "Hutan lumut lembab berkabut"),
        SampleWaypoint("Pos 7 - Punggung Gunung", -5.2829, 119.9510, 2470.0, WaypointType.VIEWPOINT, "Pemandangan lembah Ramma"),
        SampleWaypoint("Pos 8 - Titik Bahaya Tebing", -5.2838, 119.9560, 2580.0, WaypointType.DANGER, "Tebing curam sempit, hati-hati!"),
        SampleWaypoint("Pos 9 - Batas Vegetasi", -5.2841, 119.9610, 2690.0, WaypointType.CAMP, "Camp terakhir sebelum puncak"),
        SampleWaypoint("Pos 10 - Persimpangan Ramma", -5.2845, 119.9645, 2770.0, WaypointType.POS, "Simpang jalur Lembah Ramma"),
        SampleWaypoint("Puncak Gunung Bawakaraeng", -5.2853, 119.9688, 2830.0, WaypointType.SUMMIT, "Puncak tertinggi 2,830 mdpl")
    )

    fun generateBawakaraengTrackPoints(): List<GpsPoint> {
        val points = mutableListOf<GpsPoint>()
        val startTime = System.currentTimeMillis() - 7 * 3600 * 1000

        for (i in 0 until bawakaraengWaypoints.size - 1) {
            val w1 = bawakaraengWaypoints[i]
            val w2 = bawakaraengWaypoints[i + 1]
            val steps = 15 // interpolated subpoints for natural trail curvature

            for (step in 0..steps) {
                val frac = step.toDouble() / steps
                // Add natural wandering jitter to path
                val jitterLat = Math.sin(frac * Math.PI * 4) * 0.0003
                val jitterLon = Math.cos(frac * Math.PI * 4) * 0.0002
                val lat = w1.lat + (w2.lat - w1.lat) * frac + jitterLat
                val lon = w1.lon + (w2.lon - w1.lon) * frac + jitterLon
                val alt = w1.alt + (w2.alt - w1.alt) * frac

                val time = startTime + (points.size * 120 * 1000L)
                points.add(
                    GpsPoint(
                        latitude = lat,
                        longitude = lon,
                        altitude = alt,
                        timestamp = time,
                        speed = 1.1f,
                        accuracy = 3.5f,
                        bearing = GeoUtils.calculateBearing(w1.lat, w1.lon, w2.lat, w2.lon)
                    )
                )
            }
        }
        return points
    }

    fun getSampleRoute(): Pair<RouteEntity, List<WaypointEntity>> {
        val points = generateBawakaraengTrackPoints()
        val totalDist = GeoUtils.calculateTotalDistance(points)
        val (gain, loss) = GeoUtils.calculateElevationGainLoss(points)

        val route = RouteEntity(
            name = "Bawakaraeng via Buluballea",
            description = "Jalur pendakian klasik Gunung Bawakaraeng (2.830 mdpl) melewati 10 Pos dan hutan lumut.",
            totalDistanceMeters = totalDist,
            elevationGainMeters = gain,
            elevationLossMeters = loss,
            highestPointMeters = 2830.0,
            lowestPointMeters = 1540.0,
            estimatedHikingMinutes = 390, // ~6.5 hours
            difficulty = "Hard (Berat)",
            waypointsCount = bawakaraengWaypoints.size,
            coordinatesJson = GeoUtils.pointsToJson(points),
            originalFormat = "GPX",
            isFavorite = true
        )

        val waypoints = bawakaraengWaypoints.map {
            WaypointEntity(
                name = it.name,
                description = it.desc,
                latitude = it.lat,
                longitude = it.lon,
                elevationMeters = it.alt,
                type = it.type.name,
                colorHex = it.type.defaultColorHex
            )
        }

        return Pair(route, waypoints)
    }

    fun getSampleGpxXml(): String {
        val (route, waypoints) = getSampleRoute()
        val points = GeoUtils.jsonToPoints(route.coordinatesJson)

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="AG HIKING Pro" xmlns="http://www.topografix.com/GPX/1/1">
  <metadata>
    <name>${route.name}</name>
    <desc>${route.description}</desc>
  </metadata>
""")
        for (w in waypoints) {
            sb.append("""  <wpt lat="${w.latitude}" lon="${w.longitude}">
    <ele>${w.elevationMeters}</ele>
    <name>${w.name}</name>
    <desc>${w.description}</desc>
    <sym>${w.type}</sym>
  </wpt>
""")
        }
        sb.append("""  <trk>
    <name>${route.name}</name>
    <trkseg>
""")
        for (p in points) {
            sb.append("""      <trkpt lat="${p.latitude}" lon="${p.longitude}">
        <ele>${p.altitude}</ele>
        <time>2026-09-26T06:00:00Z</time>
      </trkpt>
""")
        }
        sb.append("""    </trkseg>
  </trk>
</gpx>""")
        return sb.toString()
    }
}
