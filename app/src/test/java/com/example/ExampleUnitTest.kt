package com.example

import com.example.data.local.entity.HikeTrackEntity
import com.example.data.model.GpsPoint
import com.example.data.parser.GpxExporter
import com.example.data.parser.GpxParser
import com.example.util.GeoUtils
import com.example.util.SampleRouteData
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

    @Test
    fun testDistanceCalculation() {
        // Distance from Buluballea (-5.2974, 119.9021) to Pos 1 (-5.2932, 119.9102)
        val dist = GeoUtils.calculateDistanceMeters(-5.2974, 119.9021, -5.2932, 119.9102)
        assertTrue("Distance should be around ~1000m", dist in 800.0..1200.0)
    }

    @Test
    fun testBearingCalculation() {
        // Direct North: (0.0, 0.0) -> (1.0, 0.0)
        val northBearing = GeoUtils.calculateBearing(0.0, 0.0, 1.0, 0.0)
        assertEquals(0f, northBearing, 0.5f)
        assertEquals("U", GeoUtils.bearingToCardinal(northBearing))

        // Direct East: (0.0, 0.0) -> (0.0, 1.0)
        val eastBearing = GeoUtils.calculateBearing(0.0, 0.0, 0.0, 1.0)
        assertEquals(90f, eastBearing, 0.5f)
        assertEquals("T", GeoUtils.bearingToCardinal(eastBearing))
    }

    @Test
    fun testElevationGainLoss() {
        val points = listOf(
            GpsPoint(0.0, 0.0, altitude = 1000.0),
            GpsPoint(0.0, 0.0, altitude = 1100.0), // +100
            GpsPoint(0.0, 0.0, altitude = 1060.0), // -40
            GpsPoint(0.0, 0.0, altitude = 1200.0)  // +140
        )
        val (gain, loss) = GeoUtils.calculateElevationGainLoss(points, thresholdMeters = 2.0)
        assertEquals(240.0, gain, 1.0)
        assertEquals(40.0, loss, 1.0)
    }

    @Test
    fun testOffRouteCalculation() {
        val route = listOf(
            GpsPoint(-5.0, 119.0),
            GpsPoint(-5.0, 119.01),
            GpsPoint(-5.0, 119.02)
        )
        // Point directly on segment
        val onPoint = GeoUtils.calculateOffRoute(-5.0, 119.005, route)
        assertNotNull(onPoint)
        assertTrue("Point on segment should be close to 0 distance", onPoint!!.distanceToRouteMeters < 5.0)

        // Point shifted North (~1110m away for 0.01 deg lat)
        val offPoint = GeoUtils.calculateOffRoute(-4.99, 119.005, route)
        assertNotNull(offPoint)
        assertTrue("Off route distance should be detected around 1100m", offPoint!!.distanceToRouteMeters > 1000.0)
    }

    @Test
    fun testCoordinateValidation() {
        assertTrue(GpxParser.isValidCoordinate(-5.2853, 119.9688))
        assertFalse(GpxParser.isValidCoordinate(95.0, 119.0))
        assertFalse(GpxParser.isValidCoordinate(0.0, 0.0))
    }

    @Test
    fun testDmsAndUtmConversion() {
        val dms = GeoUtils.toDms(-5.2853, 119.9688)
        assertTrue(dms.contains("S"))
        assertTrue(dms.contains("E"))

        val utm = GeoUtils.toUtmString(-5.2853, 119.9688)
        assertTrue(utm.contains("Zone"))
    }

    @Test
    fun testPolygonArea() {
        // ~100m x 100m square: area should be ~10,000 m²
        val coords = listOf(
            Pair(0.0, 0.0),
            Pair(0.0009, 0.0),
            Pair(0.0009, 0.0009),
            Pair(0.0, 0.0009)
        )
        val area = GeoUtils.calculatePolygonAreaMeters(coords)
        assertTrue("Polygon area should be positive and reasonably estimated", area > 5000.0)
    }

    @Test
    fun testSampleBawakaraengRoute() {
        val (route, waypoints) = SampleRouteData.getSampleRoute()
        assertTrue(route.name.contains("Bawakaraeng"))
        assertTrue("Total distance should be around 10-18km", route.totalDistanceMeters in 8000.0..20000.0)
        assertEquals(2830.0, route.highestPointMeters, 5.0)
        assertEquals(12, waypoints.size)

        // Test GPX Export
        val track = HikeTrackEntity(
            title = route.name,
            startTime = System.currentTimeMillis() - 3600000,
            endTime = System.currentTimeMillis(),
            totalDistanceMeters = route.totalDistanceMeters,
            durationSeconds = 3600,
            elevationGainMeters = route.elevationGainMeters,
            elevationLossMeters = route.elevationLossMeters,
            maxAltitudeMeters = route.highestPointMeters,
            minAltitudeMeters = route.lowestPointMeters,
            avgSpeedKmh = 3.5,
            maxSpeedKmh = 5.0,
            pointsJson = route.coordinatesJson
        )
        val gpx = GpxExporter.toGpx(track, waypoints)
        assertTrue(gpx.contains("<gpx version=\"1.1\""))
        assertTrue(gpx.contains("<trkpt"))
        assertTrue(gpx.contains("<wpt"))
    }
}
