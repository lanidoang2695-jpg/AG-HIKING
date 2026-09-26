package com.example.ui.map

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.data.local.entity.WaypointEntity
import com.example.data.model.GpsPoint
import com.example.data.model.MapLayerType
import com.example.data.model.NavigationState
import com.example.data.tile.MapTileEngine
import com.example.sensor.CurrentGpsState
import com.example.ui.MapMeasureState
import com.example.ui.TargetDestination
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
    targetDestination: TargetDestination? = null,
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

    // Use rememberUpdatedState to eliminate any stale closures during pointer input
    val currentCenterLat by rememberUpdatedState(centerLat)
    val currentCenterLon by rememberUpdatedState(centerLon)
    val currentZoom by rememberUpdatedState(zoom)
    val currentBearing by rememberUpdatedState(bearing)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnZoomChange by rememberUpdatedState(onZoomChange)
    val currentOnBearingChange by rememberUpdatedState(onBearingChange)
    val currentOnMapClick by rememberUpdatedState(onMapClick)
    val currentOnMapLongClick by rememberUpdatedState(onMapLongClick)

    // Spherical Mercator normalized projection formulas [0.0, 1.0]
    fun lonToNormX(lon: Double): Double = (lon + 180.0) / 360.0

    fun latToNormY(lat: Double): Double {
        val latRad = Math.toRadians(lat.coerceIn(-85.05112878, 85.05112878))
        return (1.0 - asinh(tan(latRad)) / Math.PI) / 2.0
    }

    fun normXToLon(x: Double): Double = x * 360.0 - 180.0

    fun normYToLat(y: Double): Double {
        val n = Math.PI - 2.0 * Math.PI * y
        return Math.toDegrees(atan(sinh(n))).coerceIn(-85.05112878, 85.05112878)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            // 1. Smooth Transform Gestures (Pinch to Zoom, Two-finger Pan, Rotate)
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, gestureZoom, gestureRotate ->
                    val w = size.width.toDouble()
                    val h = size.height.toDouble()
                    if (w <= 0.0 || h <= 0.0) return@detectTransformGestures

                    val oldZoom = currentZoom
                    // Smooth logarithmic zoom change (gestureZoom is frame ratio)
                    val zoomDelta = if (gestureZoom != 1.0f) {
                        (ln(gestureZoom.toDouble().coerceAtLeast(0.01)) / ln(2.0))
                    } else 0.0
                    val targetZoom = (oldZoom + zoomDelta).coerceIn(3.0, 18.5)

                    val oldWorldSize = 256.0 * 2.0.pow(oldZoom)
                    val centerNormX = lonToNormX(currentCenterLon)
                    val centerNormY = latToNormY(currentCenterLat)

                    // Normalized geographic point under touch centroid before gesture
                    val focalNormX = centerNormX + (centroid.x - w / 2.0) / oldWorldSize
                    val focalNormY = centerNormY + (centroid.y - h / 2.0) / oldWorldSize

                    // New world size after zoom
                    val newWorldSize = 256.0 * 2.0.pow(targetZoom)
                    val newCentroidX = centroid.x + pan.x
                    val newCentroidY = centroid.y + pan.y

                    // Anchor focal point under the new centroid position without drift or snapping
                    val newCenterNormX = focalNormX - (newCentroidX - w / 2.0) / newWorldSize
                    val newCenterNormY = focalNormY - (newCentroidY - h / 2.0) / newWorldSize

                    val newLat = normYToLat(newCenterNormY.coerceIn(0.00001, 0.99999))
                    val newLon = normXToLon(newCenterNormX.coerceIn(0.00001, 0.99999))

                    if (targetZoom != oldZoom) {
                        currentOnZoomChange(targetZoom)
                    }
                    currentOnMove(newLat, newLon)

                    if (abs(gestureRotate) > 2.0f) {
                        currentOnBearingChange((currentBearing + gestureRotate + 360f) % 360f)
                    }
                }
            }
            // 2. Smooth Tap & Double Tap (Double tap zooms in centered on tap point)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { screenOffset ->
                        val w = size.width.toDouble()
                        val h = size.height.toDouble()
                        val oldZoom = currentZoom
                        val targetZoom = (oldZoom + 1.0).coerceAtMost(18.5)
                        val oldWorldSize = 256.0 * 2.0.pow(oldZoom)
                        val newWorldSize = 256.0 * 2.0.pow(targetZoom)
                        val centerNormX = lonToNormX(currentCenterLon)
                        val centerNormY = latToNormY(currentCenterLat)

                        val focalNormX = centerNormX + (screenOffset.x - w / 2.0) / oldWorldSize
                        val focalNormY = centerNormY + (screenOffset.y - h / 2.0) / oldWorldSize

                        val newCenterNormX = focalNormX - (w / 2.0 - w / 2.0) / newWorldSize
                        val newCenterNormY = focalNormY - (h / 2.0 - h / 2.0) / newWorldSize

                        currentOnZoomChange(targetZoom)
                        currentOnMove(
                            normYToLat(newCenterNormY.coerceIn(0.00001, 0.99999)),
                            normXToLon(newCenterNormX.coerceIn(0.00001, 0.99999))
                        )
                    },
                    onTap = { screenOffset ->
                        val w = size.width.toDouble()
                        val h = size.height.toDouble()
                        val worldSize = 256.0 * 2.0.pow(currentZoom)
                        val centerNormX = lonToNormX(currentCenterLon)
                        val centerNormY = latToNormY(currentCenterLat)
                        val clickNormX = centerNormX + (screenOffset.x - w / 2.0) / worldSize
                        val clickNormY = centerNormY + (screenOffset.y - h / 2.0) / worldSize
                        currentOnMapClick(
                            normYToLat(clickNormY.coerceIn(0.00001, 0.99999)),
                            normXToLon(clickNormX.coerceIn(0.00001, 0.99999))
                        )
                    },
                    onLongPress = { screenOffset ->
                        val w = size.width.toDouble()
                        val h = size.height.toDouble()
                        val worldSize = 256.0 * 2.0.pow(currentZoom)
                        val centerNormX = lonToNormX(currentCenterLon)
                        val centerNormY = latToNormY(currentCenterLat)
                        val clickNormX = centerNormX + (screenOffset.x - w / 2.0) / worldSize
                        val clickNormY = centerNormY + (screenOffset.y - h / 2.0) / worldSize
                        currentOnMapLongClick(
                            normYToLat(clickNormY.coerceIn(0.00001, 0.99999)),
                            normXToLon(clickNormX.coerceIn(0.00001, 0.99999))
                        )
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val centerScreen = Offset(width / 2f, height / 2f)

        val baseZoomInt = zoom.toInt().coerceIn(2, layerType.maxZoom)
        val worldSize = 256.0 * 2.0.pow(zoom)
        val centerNormX = lonToNormX(centerLat) // longitude is x, latitude is y
        val actualCenterNormX = lonToNormX(centerLon)
        val actualCenterNormY = latToNormY(centerLat)

        // Unified, exact coordinate-to-screen transformation
        fun coordToScreen(lat: Double, lon: Double): Offset {
            val normX = lonToNormX(lon)
            val normY = latToNormY(lat)
            val dx = (normX - actualCenterNormX) * worldSize
            val dy = (normY - actualCenterNormY) * worldSize
            return Offset((centerScreen.x + dx).toFloat(), (centerScreen.y + dy).toFloat())
        }

        // Draw background terrain
        val bgColor = if (layerType.isDark) Color(0xFF141715) else Color(0xFFE8ECE5)
        drawRect(bgColor)

        // 1. Calculate and Draw Map Tiles
        val halfW = width / 2.0
        val halfH = height / 2.0
        val minNormX = (actualCenterNormX - halfW / worldSize).coerceAtLeast(0.0)
        val maxNormX = (actualCenterNormX + halfW / worldSize).coerceAtMost(1.0)
        val minNormY = (actualCenterNormY - halfH / worldSize).coerceAtLeast(0.0)
        val maxNormY = (actualCenterNormY + halfH / worldSize).coerceAtMost(1.0)

        val tilesAtZoom = 1 shl baseZoomInt
        val minTileX = floor(minNormX * tilesAtZoom).toInt().coerceIn(0, tilesAtZoom - 1)
        val maxTileX = floor(maxNormX * tilesAtZoom).toInt().coerceIn(0, tilesAtZoom - 1)
        val minTileY = floor(minNormY * tilesAtZoom).toInt().coerceIn(0, tilesAtZoom - 1)
        val maxTileY = floor(maxNormY * tilesAtZoom).toInt().coerceIn(0, tilesAtZoom - 1)

        for (tx in minTileX..maxTileX) {
            for (ty in minTileY..maxTileY) {
                val tileKey = "${layerType.name}_${baseZoomInt}_${tx}_$ty"
                var bmp = tileBitmaps[tileKey]

                if (bmp == null) {
                    // Try instant memory cache from engine
                    bmp = tileEngine.getFromMemoryCache(layerType, baseZoomInt, tx, ty)
                    if (bmp != null) {
                        tileBitmaps[tileKey] = bmp
                    } else if (!tileBitmaps.containsKey(tileKey)) {
                        // Request async fetch
                        tileBitmaps[tileKey] = null
                        coroutineScope.launch(Dispatchers.IO) {
                            val loaded = tileEngine.getTileBitmap(layerType, baseZoomInt, tx, ty)
                            if (loaded != null) {
                                tileBitmaps[tileKey] = loaded
                            }
                        }
                    }
                }

                // Tile bounds in normalized Mercator
                val tileNormMinX = tx.toDouble() / tilesAtZoom
                val tileNormMinY = ty.toDouble() / tilesAtZoom
                val tileNormMaxX = (tx + 1).toDouble() / tilesAtZoom
                val tileNormMaxY = (ty + 1).toDouble() / tilesAtZoom

                val screenLeft = (centerScreen.x + (tileNormMinX - actualCenterNormX) * worldSize).toFloat()
                val screenTop = (centerScreen.y + (tileNormMinY - actualCenterNormY) * worldSize).toFloat()
                val screenRight = (centerScreen.x + (tileNormMaxX - actualCenterNormX) * worldSize).toFloat()
                val screenBottom = (centerScreen.y + (tileNormMaxY - actualCenterNormY) * worldSize).toFloat()
                val drawW = (screenRight - screenLeft).coerceAtLeast(1f)
                val drawH = (screenBottom - screenTop).coerceAtLeast(1f)

                if (bmp != null) {
                    drawImage(
                        image = bmp.asImageBitmap(),
                        dstOffset = IntOffset(screenLeft.toInt(), screenTop.toInt()),
                        dstSize = IntSize(ceil(drawW).toInt() + 1, ceil(drawH).toInt() + 1)
                    )
                } else {
                    // Fallback to parent tile if available in memory cache to prevent flickering
                    val parentZ = baseZoomInt - 1
                    if (parentZ >= 2) {
                        val pTx = tx / 2
                        val pTy = ty / 2
                        val parentBmp = tileEngine.getFromMemoryCache(layerType, parentZ, pTx, pTy)
                        if (parentBmp != null) {
                            val subX = (tx % 2) * (parentBmp.width / 2)
                            val subY = (ty % 2) * (parentBmp.height / 2)
                            val subSize = parentBmp.width / 2
                            drawImage(
                                image = parentBmp.asImageBitmap(),
                                srcOffset = IntOffset(subX, subY),
                                srcSize = IntSize(subSize, subSize),
                                dstOffset = IntOffset(screenLeft.toInt(), screenTop.toInt()),
                                dstSize = IntSize(ceil(drawW).toInt() + 1, ceil(drawH).toInt() + 1)
                            )
                        }
                    }
                }
            }
        }

        // 2. Draw Measurement Tool Polyline/Area
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

        // 3. Draw Selected / Verified Hiking Route Polyline
        if (routePoints.size > 1) {
            val routePath = Path().apply {
                val first = coordToScreen(routePoints[0].latitude, routePoints[0].longitude)
                moveTo(first.x, first.y)
                for (i in 1 until routePoints.size) {
                    val pt = coordToScreen(routePoints[i].latitude, routePoints[i].longitude)
                    lineTo(pt.x, pt.y)
                }
            }
            // Shadow border
            drawPath(routePath, Color(0x992B1700), style = Stroke(width = 10f))
            // Route inner line
            drawPath(routePath, Color(0xFFFF9100), style = Stroke(width = 6f))
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

        // 5. Draw Direct Compass / Bearing Line to Target Destination
        val currentGps = gpsState.point
        if (targetDestination != null && currentGps != null) {
            val start = coordToScreen(currentGps.latitude, currentGps.longitude)
            val end = coordToScreen(targetDestination.latitude, targetDestination.longitude)

            drawLine(
                color = Color(0xFF00E5FF),
                start = start,
                end = end,
                strokeWidth = 4f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f)
            )

            // Destination marker pin
            drawCircle(Color.White, radius = 18f, center = end)
            drawCircle(Color(0xFF00B0FF), radius = 14f, center = end)
            drawCircle(Color.White, radius = 5f, center = end)

            drawContext.canvas.nativeCanvas.apply {
                val textPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 32f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                }
                val bgPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#00838F")
                    style = Paint.Style.FILL
                }
                val label = "★ ${targetDestination.name}"
                val textW = textPaint.measureText(label)
                drawRect(end.x - textW / 2 - 12, end.y - 60, end.x + textW / 2 + 12, end.y - 18, bgPaint)
                drawText(label, end.x, end.y - 28, textPaint)
            }
        }

        // 6. Draw Off-Route Connection Line
        if (navigationState.isNavigating && navigationState.offRouteAlert) {
            if (currentGps != null) {
                val start = coordToScreen(currentGps.latitude, currentGps.longitude)
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

        // 7. Draw Waypoints
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
                    textSize = 28f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                }
                val bgPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    style = Paint.Style.FILL
                }
                val label = w.name
                val textW = textPaint.measureText(label)
                drawRect(pos.x - textW / 2 - 8, pos.y + 18, pos.x + textW / 2 + 8, pos.y + 52, bgPaint)
                drawText(label, pos.x, pos.y + 44, textPaint)
            }
        }

        // 8. Draw Current GPS Location Marker with Accuracy & Heading
        if (currentGps != null) {
            val pos = coordToScreen(currentGps.latitude, currentGps.longitude)

            // Accuracy Circle
            val metersPerPixel = (156543.03392 * cos(Math.toRadians(currentGps.latitude)) / worldSize).toFloat()
            val accuracyRadius = (currentGps.accuracy / metersPerPixel).coerceIn(14f, 130f)
            drawCircle(Color(0x332979FF), radius = accuracyRadius, center = pos)
            drawCircle(Color(0x882979FF), radius = accuracyRadius, center = pos, style = Stroke(width = 2f))

            // Directional Heading Indicator
            rotate(degrees = currentGps.bearing, pivot = pos) {
                val conePath = Path().apply {
                    moveTo(pos.x, pos.y - 38f)
                    lineTo(pos.x - 14f, pos.y - 12f)
                    lineTo(pos.x + 14f, pos.y - 12f)
                    close()
                }
                drawPath(conePath, Color(0xEE00C853))
            }

            // Location Pin Dot
            drawCircle(Color.White, radius = 14f, center = pos)
            drawCircle(Color(0xFF00C853), radius = 10f, center = pos)
            drawCircle(Color.White, radius = 3.5f, center = pos)
        }
    }
}
