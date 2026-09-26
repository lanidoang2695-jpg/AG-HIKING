package com.example.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.WaypointEntity
import com.example.data.model.MapLayerType
import com.example.data.model.WaypointType
import com.example.ui.MainViewModel
import com.example.util.GeoUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MainViewModel,
    onNavigateToRoutes: () -> Unit
) {
    val gpsState by viewModel.gpsState.collectAsState()
    val activeHike by viewModel.activeHike.collectAsState()
    val allWaypoints by viewModel.allWaypoints.collectAsState()
    val selectedRoute by viewModel.selectedRouteForNavigation.collectAsState()
    val navigationState by viewModel.navigationState.collectAsState()
    val selectedLayer by viewModel.selectedLayer.collectAsState()
    val mapCenterLat by viewModel.mapCenterLat.collectAsState()
    val mapCenterLon by viewModel.mapCenterLon.collectAsState()
    val mapZoom by viewModel.mapZoom.collectAsState()
    val mapBearing by viewModel.mapBearing.collectAsState()
    val followGps by viewModel.followGps.collectAsState()
    val measureState by viewModel.measureState.collectAsState()

    val targetDest by viewModel.targetDestination.collectAsState()
    val directDistance by viewModel.directDistanceToTargetMeters.collectAsState()
    val targetBearing by viewModel.bearingToTargetDegrees.collectAsState()
    val elevDelta by viewModel.elevationDeltaToTarget.collectAsState()
    val etaMinutes by viewModel.etaToTargetMinutes.collectAsState()

    var showLayerDialog by remember { mutableStateOf(false) }
    var showAddWaypointDialog by remember { mutableStateOf(false) }
    var showSelectDestinationDialog by remember { mutableStateOf(false) }
    var pendingWaypointLat by remember { mutableDoubleStateOf(0.0) }
    var pendingWaypointLon by remember { mutableDoubleStateOf(0.0) }

    val routePoints = remember(selectedRoute) {
        selectedRoute?.coordinatesJson?.let { GeoUtils.jsonToPoints(it) } ?: emptyList()
    }

    Box(modifier = Modifier.fillMaxSize().testTag("map_screen")) {
        // Map Canvas Renderer with smooth zoom and focal-point gestures
        MapCanvas(
            tileEngine = viewModel.repository.tileEngine,
            layerType = selectedLayer,
            centerLat = mapCenterLat,
            centerLon = mapCenterLon,
            zoom = mapZoom,
            bearing = mapBearing,
            gpsState = gpsState,
            routePoints = routePoints,
            breadcrumbPoints = activeHike.recordedPoints,
            waypoints = allWaypoints,
            navigationState = navigationState,
            measureState = measureState,
            targetDestination = targetDest,
            onMove = { newLat, newLon ->
                viewModel.mapCenterLat.value = newLat
                viewModel.mapCenterLon.value = newLon
                if (followGps) viewModel.followGps.value = false
            },
            onZoomChange = { newZoom -> viewModel.mapZoom.value = newZoom },
            onBearingChange = { newBearing -> viewModel.mapBearing.value = newBearing },
            onMapClick = { lat, lon ->
                if (measureState.isMeasuringDistance || measureState.isMeasuringArea) {
                    viewModel.addMeasurementPoint(lat, lon)
                }
            },
            onMapLongClick = { lat, lon ->
                pendingWaypointLat = lat
                pendingWaypointLon = lon
                showAddWaypointDialog = true
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Navigation & Target Destination HUD
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Off Route Alert Card
            AnimatedVisibility(visible = navigationState.isNavigating && navigationState.offRouteAlert) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("off_route_alert")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚠ PERINGATAN: KELUAR DARI JALUR!",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Jarak ke jalur: ${navigationState.crossTrackDistanceMeters.toInt()} meter • Arah: ${GeoUtils.bearingToCardinal(navigationState.bearingToRouteDegrees)} (${navigationState.bearingToRouteDegrees.toInt()}°)",
                                color = Color(0xFFFFEBEE),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Target Destination HUD (Reading Distance to Destination)
            AnimatedVisibility(visible = targetDest != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth().testTag("destination_target_hud")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00838F)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Navigation,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(22.dp)
                                    .rotate(targetBearing ?: 0f)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "TUJUAN: ${targetDest?.name}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            val distKm = (directDistance ?: 0.0) / 1000.0
                            val bearingDeg = targetBearing?.toInt() ?: 0
                            val cardinal = GeoUtils.bearingToCardinal(targetBearing ?: 0f)
                            val elevText = if ((elevDelta ?: 0.0) >= 0) "+${elevDelta?.toInt()}m" else "${elevDelta?.toInt()}m"
                            val etaText = etaMinutes?.let { if (it >= 60) "${it / 60}j ${it % 60}m" else "$it menit" } ?: "-"
                            Text(
                                text = "Jarak: %.2f km • Arah: %s (%d°) • Elev: %s • ETA: %s".format(
                                    distKm, cardinal, bearingDeg, elevText, etaText
                                ),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(onClick = { viewModel.clearTargetDestination() }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup Target", tint = Color.Gray)
                        }
                    }
                }
            }

            // Active Route Navigation Bar
            AnimatedVisibility(visible = navigationState.isNavigating) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth().testTag("nav_route_hud")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E7D32)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Route, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Rute: ${navigationState.targetRouteName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = "Sisa: %.2f km • Sisa Elevasi: %d m • ETA: %d mnt".format(
                                    navigationState.remainingRouteDistanceMeters / 1000.0,
                                    navigationState.elevationDifferenceMeters.toInt(),
                                    navigationState.estimatedTimeRemainingSeconds / 60
                                ),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.stopNavigation() }) {
                            Icon(Icons.Default.Close, contentDescription = "Hentikan Rute")
                        }
                    }
                }
            }

            // Active Measurement Info Bar
            AnimatedVisibility(visible = measureState.isMeasuringDistance || measureState.isMeasuringArea) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF006064)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (measureState.isMeasuringDistance) "Ukur Jarak: %.2f km (%d m)".format(
                                    measureState.totalDistanceMeters / 1000.0,
                                    measureState.totalDistanceMeters.toInt()
                                ) else "Ukur Area: %.2f m² (%.2f ha)".format(
                                    measureState.totalAreaSquareMeters,
                                    measureState.totalAreaSquareMeters / 10000.0
                                ),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Sentuh peta untuk menambah titik ukur",
                                color = Color(0xFFE0F7FA),
                                fontSize = 11.sp
                            )
                        }
                        Row {
                            TextButton(onClick = { viewModel.clearMeasurement() }) {
                                Text("Reset", color = Color.White)
                            }
                            IconButton(onClick = {
                                if (measureState.isMeasuringDistance) viewModel.toggleDistanceMeasurement()
                                else viewModel.toggleAreaMeasurement()
                            }) {
                                Icon(Icons.Default.Check, contentDescription = "Selesai", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Map Scale & Elevation Overlay (Bottom Left)
        Card(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text(
                    text = "Zoom: %.1f • ${selectedLayer.title.substringBefore(" ")}".format(mapZoom),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                if (gpsState.point != null) {
                    val pt = gpsState.point!!
                    Text(
                        text = "Alt: ${pt.altitude.toInt()} mdpl • Akurasi: ±${pt.accuracy.toInt()}m",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Floating Action Buttons (Right Side)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Reset Compass North
            SmallFloatingActionButton(
                onClick = { viewModel.mapBearing.value = 0f },
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("compass_button")
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = "Arah Utara",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.rotate(-mapBearing)
                )
            }

            // Choose Target Destination
            SmallFloatingActionButton(
                onClick = { showSelectDestinationDialog = true },
                containerColor = if (targetDest != null) Color(0xFF00838F) else MaterialTheme.colorScheme.surface,
                contentColor = if (targetDest != null) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("choose_destination_button")
            ) {
                Icon(Icons.Default.Flag, contentDescription = "Pilih Tujuan")
            }

            // Layer Switcher
            SmallFloatingActionButton(
                onClick = { showLayerDialog = true },
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("layer_switcher_button")
            ) {
                Icon(Icons.Default.Layers, contentDescription = "Pilih Layer")
            }

            // Measure Tool
            SmallFloatingActionButton(
                onClick = { viewModel.toggleDistanceMeasurement() },
                containerColor = if (measureState.isMeasuringDistance) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (measureState.isMeasuringDistance) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("measure_distance_button")
            ) {
                Icon(Icons.Default.Straighten, contentDescription = "Ukur Jarak")
            }

            // Add Waypoint
            SmallFloatingActionButton(
                onClick = {
                    gpsState.point?.let {
                        pendingWaypointLat = it.latitude
                        pendingWaypointLon = it.longitude
                        showAddWaypointDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("add_waypoint_button")
            ) {
                Icon(Icons.Default.AddLocationAlt, contentDescription = "Tambah Titik")
            }

            // Zoom In (+)
            SmallFloatingActionButton(
                onClick = { viewModel.zoomIn() },
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("zoom_in_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Perbesar")
            }

            // Zoom Out (-)
            SmallFloatingActionButton(
                onClick = { viewModel.zoomOut() },
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("zoom_out_button")
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Perkecil")
            }

            // Center GPS Location
            FloatingActionButton(
                onClick = { viewModel.recenterGps() },
                containerColor = if (followGps) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (followGps) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("center_gps_button")
            ) {
                Icon(
                    imageVector = if (followGps) Icons.Default.MyLocation else Icons.Default.LocationSearching,
                    contentDescription = "Pusatkan Lokasi GPS"
                )
            }
        }

        // Destination Selection Dialog
        if (showSelectDestinationDialog) {
            AlertDialog(
                onDismissRequest = { showSelectDestinationDialog = false },
                title = { Text("Pilih Target Tujuan Navigasi") },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "Pilih Pos atau Puncak untuk memantau jarak dan arah:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Presets from verified routes
                        val presetTargets = listOf(
                            Triple("Puncak Gn. Bawakaraeng", Pair(-5.2853, 119.9688), 2830.0),
                            Triple("Pos 5 Camp Edelweiss (Bawakaraeng)", Pair(-5.2801, 119.9392), 2240.0),
                            Triple("Pos 2 Mata Air (Bawakaraeng)", Pair(-5.2891, 119.9174), 1840.0),
                            Triple("Puncak Gunung Gede", Pair(-6.7905, 106.9840), 2958.0),
                            Triple("Pos Kandang Badak (Gede)", Pair(-6.7865, 106.9760), 2390.0),
                            Triple("Puncak Gunung Rinjani", Pair(-8.4180, 116.4630), 3726.0),
                            Triple("Pelawangan Sembalun (Rinjani)", Pair(-8.4140, 116.4560), 2639.0)
                        )

                        items(presetTargets) { (name, coords, alt) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setTargetDestination(name, coords.first, coords.second, alt, "Tujuan")
                                        showSelectDestinationDialog = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(text = "Elevasi: ${alt.toInt()} mdpl", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        if (allWaypoints.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Waypoint Tersimpan:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            items(allWaypoints) { wpt ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setTargetFromWaypoint(wpt)
                                            showSelectDestinationDialog = false
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFE53935))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(text = wpt.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(text = "${wpt.elevationMeters.toInt()} mdpl • ${wpt.type}", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSelectDestinationDialog = false }) { Text("Tutup") }
                }
            )
        }

        // Layer Selection Dialog
        if (showLayerDialog) {
            AlertDialog(
                onDismissRequest = { showLayerDialog = false },
                title = { Text("Pilih Lapisan Peta Outdoor") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MapLayerType.entries.forEach { layer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedLayer == layer) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable {
                                        viewModel.selectedLayer.value = layer
                                        showLayerDialog = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedLayer == layer, onClick = null)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = layer.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = layer.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLayerDialog = false }) { Text("Tutup") }
                }
            )
        }

        // Add Waypoint Dialog
        if (showAddWaypointDialog) {
            var wptName by remember { mutableStateOf("") }
            var wptDesc by remember { mutableStateOf("") }
            var selectedType by remember { mutableStateOf(WaypointType.POS) }

            AlertDialog(
                onDismissRequest = { showAddWaypointDialog = false },
                title = { Text("Tambah Titik Waypoint Baru") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Koordinat: %.5f°, %.5f°".format(pendingWaypointLat, pendingWaypointLon),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = wptName,
                            onValueChange = { wptName = it },
                            label = { Text("Nama Titik (misal: Pos 3, Mata Air)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = wptDesc,
                            onValueChange = { wptDesc = it },
                            label = { Text("Keterangan Tambahan") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Tipe Waypoint:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(WaypointType.SUMMIT, WaypointType.CAMP, WaypointType.WATER, WaypointType.POS, WaypointType.DANGER).forEach { type ->
                                FilterChip(
                                    selected = selectedType == type,
                                    onClick = { selectedType = type },
                                    label = { Text(type.displayName.substringBefore(" /")) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (wptName.isNotBlank()) {
                                viewModel.addWaypointAtLocation(
                                    lat = pendingWaypointLat,
                                    lon = pendingWaypointLon,
                                    name = wptName,
                                    desc = wptDesc,
                                    type = selectedType
                                )
                                showAddWaypointDialog = false
                            }
                        }
                    ) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddWaypointDialog = false }) { Text("Batal") }
                }
            )
        }
    }
}
