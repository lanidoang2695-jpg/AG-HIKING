package com.example.ui.map

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.local.entity.WaypointEntity
import com.example.data.model.GpsPoint
import com.example.data.model.MapLayerType
import com.example.data.model.NavigationState
import com.example.data.tile.MapTileEngine
import com.example.sensor.CurrentGpsState
import com.example.ui.MapMeasureState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.*

@Composable
fun MapCanvas(
    tileEngine: MapTileEngine,
    layerType: MapLayerType,
    centerLat: Double,
    centerLon: Double,
    zoom: Double,
    bearing: Float,
    gpsState: CurrentGpsState,
    routePoints: List<GpsPoint>,
    breadcrumbPoints: List<GpsPoint>,
    waypoints: List<WaypointEntity>,
    navigationState: NavigationState,
    measureState: MapMeasureState,
    onMove: (newLat: Double, newLon: Double) -> Unit,
    onZoomChange: (newZoom: Double) -> Unit,
    onBearingChange: (newBearing: Float) -> Unit,
    onMapClick: (lat: Double, lon: Double) -> Unit,
    onMapLongClick: (lat: Double, lon: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    // Local bitmap cache for rendered tiles in current view
    val tileBitmaps = remember { mutableStateMapOf<String, Bitmap?>() }

    val baseZoomInt = zoom.toInt().coerceIn(2, layerType.maxZoom)
    val scaleFactor = 2.0.pow(zoom - baseZoomInt).toFloat()

    // Helper functions for coordinate projections
    fun latLonToWorld(lat: Double, lon: Double, z: Int): Offset {
        val x = (lon + 180.0) / 360.0 * (1 shl z) * 256.0
        val latRad = Math.toRadians(lat)
        val y = (1.0 - asinh(tan(latRad)) / Math.PI) / 2.0 * (1 shl z) * 256.0
        return Offset(x.toFloat(), y.toFloat())
    }

    fun worldToLatLon(worldX: Double, worldY: Double, z: Int): Pair<Double, Double> {
        val lon = (worldX / (256.0 * (1 shl z))) * 360.0 - 180.0
        val n = Math.PI - 2.0 * Math.PI * (worldY / (256.0 * (1 shl z)))
        val lat = Math.toDegrees(atan(sinh(n)))
        return Pair(lat, lon)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, gestureZoom, gestureRotate ->
                    val newZoom = (zoom + (gestureZoom - 1.0) * 1.5).coerceIn(3.0, 18.5)
                    if (gestureZoom != 1.0f) {
                        onZoomChange(newZoom)
                    }
                    if (abs(gestureRotate) > 1.0f) {
                        onBearingChange((bearing + gestureRotate + 360f) % 360f)
                    }

                    // Convert pan offset to lat/lon change
                    val zInt = newZoom.toInt()
                    val centerWorld = latLonToWorld(centerLat, centerLon, zInt)
                    val currentScale = 2.0.pow(newZoom - zInt)
                    val newWorldX = centerWorld.x - (pan.x / currentScale)
                    val newWorldY = centerWorld.y - (pan.y / currentScale)
                    val (newLat, newLon) = worldToLatLon(newWorldX, newWorldY, zInt)
                    onMove(newLat.coerceIn(-85.0, 85.0), newLon.coerceIn(-180.0, 180.0))
                }
            }
            .pointerInput(centerLat, centerLon, zoom) {
                detectTapGestures(
                    onTap = { screenOffset ->
                        val zInt = zoom.toInt()
                        val centerWorld = latLonToWorld(centerLat, centerLon, zInt)
                        val curScale = 2.0.pow(zoom - zInt)
                        val dx = (screenOffset.x - size.width / 2f) / curScale
                        val dy = (screenOffset.y - size.height / 2f) / curScale
                        val (clickLat, clickLon) = worldToLatLon(centerWorld.x + dx, centerWorld.y + dy, zInt)
                        onMapClick(clickLat, clickLon)
                    },
                    onLongPress = { screenOffset ->
                        val zInt = zoom.toInt()
                        val centerWorld = latLonToWorld(centerLat, centerLon, zInt)
                        val curScale = 2.0.pow(zoom - zInt)
                        val dx = (screenOffset.x - size.width / 2f) / curScale
                        val dy = (screenOffset.y - size.height / 2f) / curScale
                        val (clickLat, clickLon) = worldToLatLon(centerWorld.x + dx, centerWorld.y + dy, zInt)
                        onMapLongClick(clickLat, clickLon)
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val centerScreen = Offset(width / 2f, height / 2f)

        val centerWorld = latLonToWorld(centerLat, centerLon, baseZoomInt)

        fun coordToScreen(lat: Double, lon: Double): Offset {
            val ptWorld = latLonToWorld(lat, lon, baseZoomInt)
            val dx = (ptWorld.x - centerWorld.x) * scaleFactor
            val dy = (ptWorld.y - centerWorld.y) * scaleFactor
            return Offset(centerScreen.x + dx, centerScreen.y + dy)
        }

        // Draw background terrain color
        val bgColor = if (layerType.isDark) Color(0xFF141715) else Color(0xFFE8ECE5)
        drawRect(bgColor)

        // 1. Calculate Visible Tiles and Draw
        val minScreenWorldX = centerWorld.x - (width / 2f) / scaleFactor
        val maxScreenWorldX = centerWorld.x + (width / 2f) / scaleFactor
        val minScreenWorldY = centerWorld.y - (height / 2f) / scaleFactor
        val maxScreenWorldY = centerWorld.y + (height / 2f) / scaleFactor

        val minTileX = floor(minScreenWorldX / 256.0).toInt().coerceAtLeast(0)
        val maxTileX = floor(maxScreenWorldX / 256.0).toInt().coerceAtMost((1 shl baseZoomInt) - 1)
        val minTileY = floor(minScreenWorldY / 256.0).toInt().coerceAtLeast(0)
        val maxTileY = floor(maxScreenWorldY / 256.0).toInt().coerceAtMost((1 shl baseZoomInt) - 1)

        for (tx in minTileX..maxTileX) {
            for (ty in minTileY..maxTileY) {
                val tileKey = "${layerType.name}_${baseZoomInt}_${tx}_$ty"
                val bmp = tileBitmaps[tileKey]

                val tileOriginScreen = Offset(
                    centerScreen.x + (tx * 256.0f - centerWorld.x) * scaleFactor,
                    centerScreen.y + (ty * 256.0f - centerWorld.y) * scaleFactor
                )
                val tileDrawSize = 256.0f * scaleFactor

                if (bmp != null) {
                    drawImage(
                        image = bmp.asImageBitmap(),
                        dstOffset = androidx.compose.ui.unit.IntOffset(tileOriginScreen.x.toInt(), tileOriginScreen.y.toInt()),
                        dstSize = androidx.compose.ui.unit.IntSize(ceil(tileDrawSize).toInt(), ceil(tileDrawSize).toInt())
                    )
                } else {
                    // Trigger async fetch
                    if (!tileBitmaps.containsKey(tileKey)) {
                        tileBitmaps[tileKey] = null
                        coroutineScope.launch(Dispatchers.IO) {
                            val loaded = tileEngine.getTileBitmap(layerType, baseZoomInt, tx, ty)
                            if (loaded != null) {
                                tileBitmaps[tileKey] = loaded
                            }
                        }
                    }
                }
            }
        }

        // 2. Draw Measurement Polygon / Line
        if (measureState.measuredPoints.isNotEmpty()) {
            val measureScreenPts = measureState.measuredPoints.map { coordToScreen(it.first, it.second) }

            if (measureState.isMeasuringArea && measureScreenPts.size >= 3) {
                val polyPath = Path().apply {
                    moveTo(measureScreenPts[0].x, measureScreenPts[0].y)
                    for (i in 1 until measureScreenPts.size) {
                        lineTo(measureScreenPts[i].x, measureScreenPts[i].y)
                    }
                    close()
                }
                drawPath(polyPath, Color(0x5500ACC1))
                drawPath(polyPath, Color(0xFF00ACC1), style = Stroke(width = 4f))
            } else {
                val linePath = Path().apply {
                    moveTo(measureScreenPts[0].x, measureScreenPts[0].y)
                    for (i in 1 until measureScreenPts.size) {
                        lineTo(measureScreenPts[i].x, measureScreenPts[i].y)
                    }
                }
                drawPath(
                    linePath,
                    Color(0xFF00ACC1),
                    style = Stroke(width = 5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f))
                )
            }

            for (p in measureScreenPts) {
                drawCircle(Color.White, radius = 9f, center = p)
                drawCircle(Color(0xFF00ACC1), radius = 6f, center = p)
            }
        }

        // 3. Draw Selected / Imported Route Polyline
        if (routePoints.size > 1) {
            val routePath = Path().apply {
                val first = coordToScreen(routePoints[0].latitude, routePoints[0].longitude)
                moveTo(first.x, first.y)
                for (i in 1 until routePoints.size) {
                    val pt = coordToScreen(routePoints[i].latitude, routePoints[i].longitude)
                    lineTo(pt.x, pt.y)
                }
            }
            // Route shadow/border
            drawPath(routePath, Color(0x992B1700), style = Stroke(width = 11f))
            // Route inner line
            drawPath(routePath, Color(0xFFFF9100), style = Stroke(width = 7f))
        }

        // 4. Draw Active Hike Breadcrumb Trail
        if (breadcrumbPoints.size > 1) {
            val breadcrumbPath = Path().apply {
                val first = coordToScreen(breadcrumbPoints[0].latitude, breadcrumbPoints[0].longitude)
                moveTo(first.x, first.y)
                for (i in 1 until breadcrumbPoints.size) {
                    val pt = coordToScreen(breadcrumbPoints[i].latitude, breadcrumbPoints[i].longitude)
                    lineTo(pt.x, pt.y)
                }
            }
            drawPath(breadcrumbPath, Color(0xFF00E676), style = Stroke(width = 6f))
        }

        // 5. Draw Off-Route Connection Line
        if (navigationState.isNavigating && navigationState.offRouteAlert) {
            val gpsPt = gpsState.point
            if (gpsPt != null) {
                val start = coordToScreen(gpsPt.latitude, gpsPt.longitude)
                val end = coordToScreen(navigationState.nearestRouteLat, navigationState.nearestRouteLon)
                drawLine(
                    color = Color(0xFFFF1744),
                    start = start,
                    end = end,
                    strokeWidth = 5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f)
                )
            }
        }

        // 6. Draw Waypoints
        for (w in waypoints) {
            val pos = coordToScreen(w.latitude, w.longitude)
            val wptColor = try {
                Color(android.graphics.Color.parseColor(w.colorHex))
            } catch (_: Exception) {
                Color(0xFFE53935)
            }

            // Pin marker outer ring & shadow
            drawCircle(Color(0x66000000), radius = 18f, center = Offset(pos.x + 2f, pos.y + 2f))
            drawCircle(Color.White, radius = 16f, center = pos)
            drawCircle(wptColor, radius = 13f, center = pos)
            drawCircle(Color.White, radius = 4f, center = pos)

            // Waypoint Label
            drawContext.canvas.nativeCanvas.apply {
                val textPaint = Paint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = 30f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                }
                val bgPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    style = Paint.Style.FILL
                }
                val label = w.name
                val textW = textPaint.measureText(label)
                drawRect(pos.x - textW / 2 - 8, pos.y + 18, pos.x + textW / 2 + 8, pos.y + 54, bgPaint)
                drawText(label, pos.x, pos.y + 46, textPaint)
            }
        }

        // 7. Draw Current GPS Location Marker
        val currentGps = gpsState.point
        if (currentGps != null) {
            val pos = coordToScreen(currentGps.latitude, currentGps.longitude)

            // Accuracy Circle
            val metersPerPixel = (156543.03392 * cos(Math.toRadians(currentGps.latitude)) / 2.0.pow(zoom)).toFloat()
            val accuracyRadius = (currentGps.accuracy / metersPerPixel).coerceIn(12f, 120f)
            drawCircle(Color(0x332979FF), radius = accuracyRadius, center = pos)
            drawCircle(Color(0x882979FF), radius = accuracyRadius, center = pos, style = Stroke(width = 2f))

            // Directional Heading Indicator
            rotate(degrees = currentGps.bearing, pivot = pos) {
                val conePath = Path().apply {
                    moveTo(pos.x, pos.y - 36f)
                    lineTo(pos.x - 14f, pos.y - 12f)
                    lineTo(pos.x + 14f, pos.y - 12f)
                    close()
                }
                drawPath(conePath, Color(0xCC00C853))
            }

            // Location Pin
            drawCircle(Color.White, radius = 14f, center = pos)
            drawCircle(Color(0xFF00C853), radius = 10f, center = pos)
            drawCircle(Color.White, radius = 3f, center = pos)
        }
    }
}
