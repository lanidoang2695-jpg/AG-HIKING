package com.example.data.parser

import com.example.data.local.entity.HikeTrackEntity
import com.example.data.local.entity.WaypointEntity
import com.example.data.model.GpsPoint
import com.example.util.GeoUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object GpxExporter {
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun generateFileName(trackTitle: String, extension: String = "gpx"): String {
        val cleanName = trackTitle.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return "AG_HIKING_${cleanName}_$dateStr.$extension"
    }

    fun toGpx(track: HikeTrackEntity, waypoints: List<WaypointEntity> = emptyList()): String {
        val points = GeoUtils.jsonToPoints(track.pointsJson)
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="AG HIKING Pro - Outdoor GPS" xmlns="http://www.topografix.com/GPX/1/1">
  <metadata>
    <name>${escapeXml(track.title)}</name>
    <desc>${escapeXml(track.note)}</desc>
    <time>${isoDateFormat.format(Date(track.startTime))}</time>
  </metadata>
""")

        // Waypoints
        for (w in waypoints) {
            sb.append("""  <wpt lat="${w.latitude}" lon="${w.longitude}">
    <ele>${w.elevationMeters}</ele>
    <name>${escapeXml(w.name)}</name>
    <desc>${escapeXml(w.description)}</desc>
    <sym>${w.type}</sym>
  </wpt>
""")
        }

        // Track
        sb.append("""  <trk>
    <name>${escapeXml(track.title)}</name>
    <trkseg>
""")
        for (p in points) {
            sb.append("""      <trkpt lat="${p.latitude}" lon="${p.longitude}">
        <ele>${p.altitude}</ele>
        <time>${isoDateFormat.format(Date(p.timestamp))}</time>
        <speed>${p.speed}</speed>
      </trkpt>
""")
        }
        sb.append("""    </trkseg>
  </trk>
</gpx>""")
        return sb.toString()
    }

    fun toKml(track: HikeTrackEntity): String {
        val points = GeoUtils.jsonToPoints(track.pointsJson)
        val coordString = points.joinToString(" ") { "${it.longitude},${it.latitude},${it.altitude}" }
        return """<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2">
  <Document>
    <name>${escapeXml(track.title)}</name>
    <Placemark>
      <name>${escapeXml(track.title)}</name>
      <LineString>
        <altitudeMode>absolute</altitudeMode>
        <coordinates>$coordString</coordinates>
      </LineString>
    </Placemark>
  </Document>
</kml>"""
    }

    fun toGeoJson(track: HikeTrackEntity): String {
        val points = GeoUtils.jsonToPoints(track.pointsJson)
        val coords = points.joinToString(",") { "[${it.longitude},${it.latitude},${it.altitude}]" }
        return """{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "properties": {
        "name": "${escapeJson(track.title)}",
        "distance": ${track.totalDistanceMeters},
        "duration": ${track.durationSeconds},
        "gain": ${track.elevationGainMeters},
        "loss": ${track.elevationLossMeters}
      },
      "geometry": {
        "type": "LineString",
        "coordinates": [$coords]
      }
    }
  ]
}"""
    }

    fun toCsv(track: HikeTrackEntity): String {
        val points = GeoUtils.jsonToPoints(track.pointsJson)
        val sb = StringBuilder("latitude,longitude,altitude,timestamp,speed,accuracy,bearing\n")
        for (p in points) {
            sb.append("${p.latitude},${p.longitude},${p.altitude},${p.timestamp},${p.speed},${p.accuracy},${p.bearing}\n")
        }
        return sb.toString()
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun escapeJson(text: String): String {
        return text.replace("\"", "\\\"").replace("\n", " ")
    }
}
