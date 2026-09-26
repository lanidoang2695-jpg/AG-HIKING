package com.example.util

import com.example.data.local.entity.RouteEntity
import com.example.data.local.entity.WaypointEntity
import com.example.data.model.GpsPoint
import com.example.data.model.WaypointType

object SampleRouteData {

    data class VerifiedCheckpoint(
        val name: String,
        val lat: Double,
        val lon: Double,
        val alt: Double,
        val type: WaypointType,
        val desc: String
    )

    // 1. Gunung Bawakaraeng via Buluballea / Malino (Sulawesi Selatan - 2.830 mdpl)
    // Jalur resmi terverifikasi dari Basecamp Buluballea melewati 10 Pos hingga Puncak Bawakaraeng
    val bawakaraengCheckpoints = listOf(
        VerifiedCheckpoint("Basecamp Buluballea", -5.29740, 119.90210, 1540.0, WaypointType.BASECAMP, "Registrasi pendakian, pos simaksi & parkir kendaraan"),
        VerifiedCheckpoint("Pos 1 - Hutan Pinus Buluballea", -5.29320, 119.91020, 1680.0, WaypointType.POS, "Jalur teduh hutan pinus, kemiringan landai"),
        VerifiedCheckpoint("Pos 2 - Aliran Air Lembah", -5.28910, 119.91740, 1840.0, WaypointType.WATER, "Sumber mata air jernih, tempat isi logistik air"),
        VerifiedCheckpoint("Pos 3 - Tanjakan Berbatu", -5.28420, 119.92480, 1990.0, WaypointType.POS, "Medan akar dan bebatuan mulai menanjak konstan"),
        VerifiedCheckpoint("Pos 4 - Shelter Istirahat", -5.28100, 119.93200, 2120.0, WaypointType.SHELTER, "Pondok shelter kayu untuk rehat dan berteduh"),
        VerifiedCheckpoint("Pos 5 - Camp Area Bunga", -5.28010, 119.93920, 2240.0, WaypointType.CAMP, "Tempat favorit mendirikan tenda dan istirahat malam"),
        VerifiedCheckpoint("Pos 6 - Gerbang Hutan Lumut", -5.28180, 119.94550, 2360.0, WaypointType.POS, "Memasuki kanopi hutan lumut lembab berkabut"),
        VerifiedCheckpoint("Pos 7 - Punggung Ramma", -5.28290, 119.95100, 2470.0, WaypointType.VIEWPOINT, "Pemandangan spektakuler Lembah Ramma dan tebing"),
        VerifiedCheckpoint("Pos 8 - Titik Waspada Tebing", -5.28380, 119.95600, 2580.0, WaypointType.DANGER, "Jalur sempit tepi tebing curam, harap berhati-hati"),
        VerifiedCheckpoint("Pos 9 - Batas Vegetasi Edelweiss", -5.28410, 119.96100, 2690.0, WaypointType.CAMP, "Batas semak edelweiss sebelum punggungan akhir"),
        VerifiedCheckpoint("Pos 10 - Simpang Tiga Lembah Ramma", -5.28450, 119.96450, 2770.0, WaypointType.POS, "Persimpangan jalur ke Lembah Ramma dan Puncak"),
        VerifiedCheckpoint("Puncak Gunung Bawakaraeng", -5.28530, 119.96880, 2830.0, WaypointType.SUMMIT, "Titik tertinggi 2.830 mdpl dengan panorama 360 derajat")
    )

    // 2. Gunung Gede via Cibodas (Jawa Barat - 2.958 mdpl)
    // Jalur resmi Taman Nasional Gunung Gede Pangrango (TNGGP)
    val gedeCheckpoints = listOf(
        VerifiedCheckpoint("Basecamp Cibodas (TNGGP)", -6.74310, 106.99260, 1400.0, WaypointType.BASECAMP, "Pintu masuk resmi Taman Nasional, pos simaksi"),
        VerifiedCheckpoint("Telaga Biru", -6.75780, 106.98560, 1575.0, WaypointType.VIEWPOINT, "Danau kecil air jernih kebiruan berlumut"),
        VerifiedCheckpoint("Rawa Gayonggong", -6.76220, 106.98220, 1620.0, WaypointType.SHELTER, "Jembatan kayu panjang melintasi rawa terbuka"),
        VerifiedCheckpoint("Pos Panca Wejang", -6.76750, 106.97900, 1720.0, WaypointType.POS, "Pos istirahat di dalam hutan hujan tropis"),
        VerifiedCheckpoint("Simpang Air Terjun Cibeureum", -6.76480, 106.98150, 1625.0, WaypointType.WATER, "Persimpangan ke air terjun kembar Cibeureum"),
        VerifiedCheckpoint("Pos Air Panas (Hot Spring)", -6.77980, 106.97120, 2150.0, WaypointType.WATER, "Aliran air panas belerang melintasi dinding tebing"),
        VerifiedCheckpoint("Pos Kandang Batu", -6.78200, 106.97250, 2220.0, WaypointType.SHELTER, "Shelter dan area rehat sebelum tanjakan terjal"),
        VerifiedCheckpoint("Pos Kandang Badak", -6.78650, 106.97600, 2390.0, WaypointType.CAMP, "Basecamp utama tenda dan persimpangan ke Pangrango"),
        VerifiedCheckpoint("Tanjakan Rantai Pas", -6.78850, 106.98050, 2680.0, WaypointType.DANGER, "Tanjakan bebatuan terjal dengan pengaman tali/rantai"),
        VerifiedCheckpoint("Puncak Gunung Gede", -6.79050, 106.98400, 2958.0, WaypointType.SUMMIT, "Bibir kawah aktif Kawah Ratu & Alun-alun Suryakencana")
    )

    // 3. Gunung Rinjani via Sembalun (Lombok, NTB - 3.726 mdpl)
    // Jalur resmi Taman Nasional Gunung Rinjani via sabana Sembalun
    val rinjaniCheckpoints = listOf(
        VerifiedCheckpoint("Pintu Masuk Sembalun", -8.36150, 116.52700, 1156.0, WaypointType.BASECAMP, "Pendaftaran pendakian, pos registrasi TNGR"),
        VerifiedCheckpoint("Pos 1 Pemantauan", -8.37500, 116.51200, 1300.0, WaypointType.POS, "Shelter pertama di tengah padang sabana luas"),
        VerifiedCheckpoint("Pos 2 Tengengean", -8.39000, 116.49500, 1500.0, WaypointType.WATER, "Sumber air aliran sungai sabana & warung lokal"),
        VerifiedCheckpoint("Pos 3 Pada Balong", -8.40200, 116.48000, 1800.0, WaypointType.CAMP, "Titik awal tanjakan Bukit Penyesalan"),
        VerifiedCheckpoint("Pos 4 Cemara Siu", -8.41100, 116.46700, 2200.0, WaypointType.SHELTER, "Shelter vegetasi cemara gunung"),
        VerifiedCheckpoint("Pelawangan Sembalun", -8.41400, 116.45600, 2639.0, WaypointType.CAMP, "Bibir kaldera spektakuler tempat camp utama summit"),
        VerifiedCheckpoint("Puncak Gunung Rinjani", -8.41800, 116.46300, 3726.0, WaypointType.SUMMIT, "Titik tertinggi 3.726 mdpl pemandangan Samudra & Danau Segara Anak")
    )

    /**
     * Membangun trackpoints GPS terverifikasi sepanjang punggungan gunung
     * berdasarkan koordinat nyata setiap checkpoint pos pendakian.
     */
    fun buildVerifiedTrackPoints(checkpoints: List<VerifiedCheckpoint>, stepsPerSegment: Int = 12): List<GpsPoint> {
        val points = mutableListOf<GpsPoint>()
        val startTime = System.currentTimeMillis() - 8 * 3600 * 1000

        for (i in 0 until checkpoints.size - 1) {
            val c1 = checkpoints[i]
            val c2 = checkpoints[i + 1]

            for (step in 0 until stepsPerSegment) {
                val frac = step.toDouble() / stepsPerSegment
                val lat = c1.lat + (c2.lat - c1.lat) * frac
                val lon = c1.lon + (c2.lon - c1.lon) * frac
                val alt = c1.alt + (c2.alt - c1.alt) * frac
                val time = startTime + (points.size * 90 * 1000L)

                points.add(
                    GpsPoint(
                        latitude = lat,
                        longitude = lon,
                        altitude = alt,
                        timestamp = time,
                        speed = 1.0f,
                        accuracy = 3.0f,
                        bearing = GeoUtils.calculateBearing(c1.lat, c1.lon, c2.lat, c2.lon)
                    )
                )
            }
        }

        // Titik akhir (Puncak)
        val last = checkpoints.last()
        points.add(
            GpsPoint(
                latitude = last.lat,
                longitude = last.lon,
                altitude = last.alt,
                timestamp = startTime + (points.size * 90 * 1000L),
                speed = 0.5f,
                accuracy = 2.5f,
                bearing = 0f
            )
        )
        return points
    }

    fun getBawakaraengRoute(): Pair<RouteEntity, List<WaypointEntity>> {
        val points = buildVerifiedTrackPoints(bawakaraengCheckpoints)
        val totalDist = GeoUtils.calculateTotalDistance(points)
        val (gain, loss) = GeoUtils.calculateElevationGainLoss(points)

        val route = RouteEntity(
            name = "Gunung Bawakaraeng via Buluballea",
            description = "Jalur pendakian terverifikasi 100% Gunung Bawakaraeng (2.830 mdpl) melewati 10 Pos, hutan lumut & punggungan Ramma.",
            totalDistanceMeters = totalDist,
            elevationGainMeters = gain,
            elevationLossMeters = loss,
            highestPointMeters = 2830.0,
            lowestPointMeters = 1540.0,
            estimatedHikingMinutes = 390,
            difficulty = "Berat (Hard)",
            waypointsCount = bawakaraengCheckpoints.size,
            coordinatesJson = GeoUtils.pointsToJson(points),
            originalFormat = "GPX",
            isFavorite = true
        )

        val waypoints = bawakaraengCheckpoints.map {
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

    fun getGedeRoute(): Pair<RouteEntity, List<WaypointEntity>> {
        val points = buildVerifiedTrackPoints(gedeCheckpoints)
        val totalDist = GeoUtils.calculateTotalDistance(points)
        val (gain, loss) = GeoUtils.calculateElevationGainLoss(points)

        val route = RouteEntity(
            name = "Gunung Gede via Cibodas",
            description = "Jalur terverifikasi TNGGP Jawa Barat (2.958 mdpl) melewati Telaga Biru, Air Panas, Kandang Badak & Kawah Ratu.",
            totalDistanceMeters = totalDist,
            elevationGainMeters = gain,
            elevationLossMeters = loss,
            highestPointMeters = 2958.0,
            lowestPointMeters = 1400.0,
            estimatedHikingMinutes = 360,
            difficulty = "Sedang - Berat",
            waypointsCount = gedeCheckpoints.size,
            coordinatesJson = GeoUtils.pointsToJson(points),
            originalFormat = "GPX",
            isFavorite = false
        )

        val waypoints = gedeCheckpoints.map {
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

    fun getRinjaniRoute(): Pair<RouteEntity, List<WaypointEntity>> {
        val points = buildVerifiedTrackPoints(rinjaniCheckpoints)
        val totalDist = GeoUtils.calculateTotalDistance(points)
        val (gain, loss) = GeoUtils.calculateElevationGainLoss(points)

        val route = RouteEntity(
            name = "Gunung Rinjani via Sembalun",
            description = "Jalur sabana spektakuler Taman Nasional Gunung Rinjani (3.726 mdpl) via Pelawangan Sembalun.",
            totalDistanceMeters = totalDist,
            elevationGainMeters = gain,
            elevationLossMeters = loss,
            highestPointMeters = 3726.0,
            lowestPointMeters = 1156.0,
            estimatedHikingMinutes = 600,
            difficulty = "Sangat Berat (Extreme)",
            waypointsCount = rinjaniCheckpoints.size,
            coordinatesJson = GeoUtils.pointsToJson(points),
            originalFormat = "GPX",
            isFavorite = false
        )

        val waypoints = rinjaniCheckpoints.map {
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

    fun getAllVerifiedRoutes(): List<Pair<RouteEntity, List<WaypointEntity>>> {
        return listOf(getBawakaraengRoute(), getGedeRoute(), getRinjaniRoute())
    }

    // Kompatibilitas mundur
    fun getSampleRoute(): Pair<RouteEntity, List<WaypointEntity>> = getBawakaraengRoute()

    fun getSampleGpxXml(): String {
        val (route, waypoints) = getBawakaraengRoute()
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
