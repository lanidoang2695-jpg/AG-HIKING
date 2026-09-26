package com.example.ui.routes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.RouteEntity
import com.example.data.local.entity.WaypointEntity
import com.example.data.model.GpsPoint
import com.example.ui.MainViewModel
import com.example.util.GeoUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesScreen(
    viewModel: MainViewModel,
    onNavigateToMap: () -> Unit
) {
    val context = LocalContext.current
    val routes by viewModel.allRoutes.collectAsState()
    var selectedRouteForDetail by remember { mutableStateOf<RouteEntity?>(null) }
    var routeWaypoints by remember { mutableStateOf<List<WaypointEntity>>(emptyList()) }

    // SAF file picker for GPX/KML/GeoJSON
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val fileName = uri.lastPathSegment ?: "imported_route.gpx"
                if (inputStream != null) {
                    viewModel.importRouteFile(inputStream, fileName)
                }
            } catch (e: Exception) {
                viewModel.userMessage.value = "Gagal membaca file: ${e.localizedMessage}"
            }
        }
    }

    LaunchedEffect(selectedRouteForDetail) {
        selectedRouteForDetail?.let { route ->
            routeWaypoints = viewModel.repository.getWaypointsForRouteSync(route.id)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("routes_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Import & Sample Route Action Buttons
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("import_gpx_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Impor File GPX")
                }

                OutlinedButton(
                    onClick = { viewModel.loadSampleRoute() },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Hiking, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Contoh Bawakaraeng")
                }
            }
        }

        // Header Title
        item {
            Text(
                text = "DAFTAR RUTE TERSIMPAN (${routes.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (routes.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Belum ada rute pendakian", fontWeight = FontWeight.Bold)
                        Text(
                            "Tekan 'Impor File GPX' atau 'Contoh Bawakaraeng' untuk memulai analisis jalur",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(routes, key = { it.id }) { route ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedRouteForDetail = route }
                        .testTag("route_item_${route.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = route.name,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF2E7D32).copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = route.difficulty,
                                    color = Color(0xFF2E7D32),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (route.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = route.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Stats Grid
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MetricText("JARAK", "%.2f km".format(route.totalDistanceMeters / 1000.0))
                            MetricText("GAIN", "+%d m".format(route.elevationGainMeters.toInt()))
                            MetricText("PUNCAK", "%d mdpl".format(route.highestPointMeters.toInt()))
                            MetricText("ESTIMASI", "%d jam".format(route.estimatedHikingMinutes / 60))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.deleteRoute(route.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = Color(0xFFC62828))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    viewModel.startNavigatingRoute(route)
                                    onNavigateToMap()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("IKUTI RUTE")
                            }
                        }
                    }
                }
            }
        }
    }

    // Route Detail & Elevation Analysis BottomSheet
    selectedRouteForDetail?.let { route ->
        ModalBottomSheet(
            onDismissRequest = { selectedRouteForDetail = null },
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            RouteDetailAnalysisContent(
                route = route,
                waypoints = routeWaypoints,
                onStartNavigation = {
                    viewModel.startNavigatingRoute(route)
                    selectedRouteForDetail = null
                    onNavigateToMap()
                },
                onClose = { selectedRouteForDetail = null }
            )
        }
    }
}

@Composable
private fun RouteDetailAnalysisContent(
    route: RouteEntity,
    waypoints: List<WaypointEntity>,
    onStartNavigation: () -> Unit,
    onClose: () -> Unit
) {
    val points = remember(route) { GeoUtils.jsonToPoints(route.coordinatesJson) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }

    val scrubPoint = remember(scrubFraction, points) {
        if (points.isNotEmpty()) {
            val idx = (scrubFraction * (points.size - 1)).toInt().coerceIn(0, points.size - 1)
            points[idx]
        } else null
    }

    val scrubDistanceKm = remember(scrubFraction, route) {
        (scrubFraction * (route.totalDistanceMeters / 1000.0)).toDouble()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = route.name, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text(
                        text = "Format: ${route.originalFormat} • ${waypoints.size} Waypoints",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup")
                }
            }
        }

        // Elevation Profile Chart with Interactive Scrubber
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PROFIL ELEVASI INTERAKTIF", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        Text("Geser untuk melihat detail", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (points.size >= 2) {
                        val alts = points.map { it.altitude }
                        val minAlt = alts.minOrNull() ?: 0.0
                        val maxAlt = (alts.maxOrNull() ?: 1.0).coerceAtLeast(minAlt + 10.0)

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, _ ->
                                        val frac = (change.position.x / size.width).coerceIn(0f, 1f)
                                        scrubFraction = frac
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        val frac = (offset.x / size.width).coerceIn(0f, 1f)
                                        scrubFraction = frac
                                    }
                                }
                        ) {
                            val w = size.width
                            val h = size.height
                            val path = Path()

                            points.forEachIndexed { i, pt ->
                                val x = (i.toFloat() / (points.size - 1)) * w
                                val norm = ((pt.altitude - minAlt) / (maxAlt - minAlt)).toFloat().coerceIn(0f, 1f)
                                val y = h - (norm * (h - 30f)) - 15f
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }

                            // Draw shaded area under elevation line
                            val fillPath = Path().apply {
                                addPath(path)
                                lineTo(w, h)
                                lineTo(0f, h)
                                close()
                            }
                            drawPath(fillPath, Color(0x332E7D32))
                            drawPath(path, Color(0xFF2E7D32), style = Stroke(width = 4f))

                            // Draw Scrubber Indicator
                            val scrubX = scrubFraction * w
                            val scrubNorm = scrubPoint?.let { ((it.altitude - minAlt) / (maxAlt - minAlt)).toFloat().coerceIn(0f, 1f) } ?: 0f
                            val scrubY = h - (scrubNorm * (h - 30f)) - 15f

                            drawLine(Color(0xFFE65100), Offset(scrubX, 0f), Offset(scrubX, h), strokeWidth = 2f)
                            drawCircle(Color.White, radius = 7f, center = Offset(scrubX, scrubY))
                            drawCircle(Color(0xFFE65100), radius = 5f, center = Offset(scrubX, scrubY))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Current Scrubber Inspector Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Jarak: %.2f km".format(scrubDistanceKm), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Elevasi: ${scrubPoint?.altitude?.toInt() ?: 0} mdpl", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Route Statistics Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("STATISTIK JALUR", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricText("TOTAL JARAK", "%.2f km".format(route.totalDistanceMeters / 1000.0))
                        MetricText("ELEVATION GAIN", "+%d m".format(route.elevationGainMeters.toInt()))
                        MetricText("ELEVATION LOSS", "-%d m".format(route.elevationLossMeters.toInt()))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricText("TITIK TERTINGGI", "%d mdpl".format(route.highestPointMeters.toInt()))
                        MetricText("TITIK TERENDAH", "%d mdpl".format(route.lowestPointMeters.toInt()))
                        MetricText("TINGKAT KESULITAN", route.difficulty)
                    }
                }
            }
        }

        // Waypoints in Route
        if (waypoints.isNotEmpty()) {
            item {
                Text("WAYPOINT PADA RUTE (${waypoints.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            items(waypoints) { wpt ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = wpt.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            if (wpt.description.isNotBlank()) {
                                Text(text = wpt.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(text = "${wpt.elevationMeters.toInt()} mdpl", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Action Start Navigation Button
        item {
            Button(
                onClick = onStartNavigation,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("MULAI NAVIGASI & OFF-ROUTE ALERT", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun MetricText(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
