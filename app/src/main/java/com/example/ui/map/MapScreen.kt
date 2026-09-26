package com.example.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

    var showLayerDialog by remember { mutableStateOf(false) }
    var showAddWaypointDialog by remember { mutableStateOf(false) }
    var pendingWaypointLat by remember { mutableDoubleStateOf(0.0) }
    var pendingWaypointLon by remember { mutableDoubleStateOf(0.0) }

    val routePoints = remember(selectedRoute) {
        selectedRoute?.coordinatesJson?.let { GeoUtils.jsonToPoints(it) } ?: emptyList()
    }

    Box(modifier = Modifier.fillMaxSize().testTag("map_screen")) {
        // Map Canvas Renderer
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

        // Top Navigation & Route HUD
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
                                text = "⚠ ANDA KELUAR DARI JALUR!",
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

            // On Route Active Navigation Bar
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
                            Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = navigationState.targetRouteName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = "Sisa: %.2f km • Elev: %d m • ETA: %d mnt".format(
                                    navigationState.remainingRouteDistanceMeters / 1000.0,
                                    navigationState.elevationDifferenceMeters.toInt(),
                                    navigationState.estimatedTimeRemainingSeconds / 60
                                ),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.stopNavigation() }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup")
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
                                text = "Tap pada peta untuk menambah titik",
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(
                    text = "Zoom: %.1f • ${selectedLayer.title.substringBefore(" ")}".format(mapZoom),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                if (gpsState.point != null) {
                    Text(
                        text = "Alt: ${gpsState.point?.altitude?.toInt()} mdpl • Acc: ±${gpsState.point?.accuracy?.toInt()}m",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary
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
            // Compass Needle (Tap to reset North)
            SmallFloatingActionButton(
                onClick = { viewModel.mapBearing.value = 0f },
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("compass_button")
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = "Reset Utara",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.rotate(-mapBearing)
                )
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

            // Area Tool
            SmallFloatingActionButton(
                onClick = { viewModel.toggleAreaMeasurement() },
                containerColor = if (measureState.isMeasuringArea) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (measureState.isMeasuringArea) Color.White else MaterialTheme.colorScheme.onSurface
            ) {
                Icon(Icons.Default.SquareFoot, contentDescription = "Ukur Area")
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
                Icon(Icons.Default.AddLocationAlt, contentDescription = "Tambah Waypoint")
            }

            // Zoom In
            SmallFloatingActionButton(
                onClick = { viewModel.mapZoom.value = (mapZoom + 1.0).coerceAtMost(18.5) },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }

            // Zoom Out
            SmallFloatingActionButton(
                onClick = { viewModel.mapZoom.value = (mapZoom - 1.0).coerceAtLeast(3.0) },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
            }

            // Follow GPS / Center Location
            FloatingActionButton(
                onClick = {
                    val pt = gpsState.point
                    if (pt != null) {
                        viewModel.mapCenterLat.value = pt.latitude
                        viewModel.mapCenterLon.value = pt.longitude
                        viewModel.followGps.value = true
                    }
                },
                containerColor = if (followGps) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (followGps) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("center_gps_button")
            ) {
                Icon(
                    imageVector = if (followGps) Icons.Default.MyLocation else Icons.Default.LocationSearching,
                    contentDescription = "Pusatkan Lokasi"
                )
            }
        }

        // Layer Selection Dialog
        if (showLayerDialog) {
            AlertDialog(
                onDismissRequest = { showLayerDialog = false },
                title = { Text("Pilih Layer Peta Outdoor") },
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
                title = { Text("Tambah Waypoint") },
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
