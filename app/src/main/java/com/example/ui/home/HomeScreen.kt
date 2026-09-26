package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensor.GpsAccuracyLevel
import com.example.service.TrackingStatus
import com.example.ui.MainViewModel
import com.example.util.GeoUtils

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToMap: () -> Unit,
    onNavigateToTracking: () -> Unit,
    onNavigateToRoutes: () -> Unit,
    onNavigateToWaypoints: () -> Unit,
    onNavigateToOfflineMaps: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToEmergency: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val gpsState by viewModel.gpsState.collectAsState()
    val compassState by viewModel.compassState.collectAsState()
    val activeHike by viewModel.activeHike.collectAsState()
    val navigationState by viewModel.navigationState.collectAsState()
    val tracks by viewModel.allTracks.collectAsState()

    val currentPoint = gpsState.point
    val targetDest by viewModel.targetDestination.collectAsState()
    val directDistance by viewModel.directDistanceToTargetMeters.collectAsState()
    val targetBearing by viewModel.bearingToTargetDegrees.collectAsState()
    val elevDelta by viewModel.elevationDeltaToTarget.collectAsState()
    val etaMinutes by viewModel.etaToTargetMinutes.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Hike Banner (if currently tracking)
        if (activeHike.status != TrackingStatus.STOPPED) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (activeHike.status == TrackingStatus.RECORDING) Color(0xFF1B5E20) else Color(0xFFE65100)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToTracking() }
                        .testTag("active_tracking_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (activeHike.status == TrackingStatus.RECORDING) "MEREKAM PENDAKIAN" else "PENDAKIAN DI-PAUSE",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "%.2f km • Elev %d m".format(activeHike.totalDistanceMeters / 1000.0, activeHike.currentAltitude.toInt()),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Button(
                            onClick = { onNavigateToTracking() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                        ) {
                            Text("Buka HUD")
                        }
                    }
                }
            }
        }

        // Active Navigation Banner (if following a route)
        if (navigationState.isNavigating) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (navigationState.offRouteAlert) Color(0xFFB71C1C) else MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToMap() }
                        .testTag("navigation_status_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (navigationState.offRouteAlert) Icons.Default.Warning else Icons.Default.Navigation,
                            contentDescription = "Navigasi",
                            tint = if (navigationState.offRouteAlert) Color.White else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (navigationState.offRouteAlert) "⚠ KELUAR DARI JALUR (${navigationState.crossTrackDistanceMeters.toInt()}m)" else "✓ ON TRACK: ${navigationState.targetRouteName}",
                                fontWeight = FontWeight.Bold,
                                color = if (navigationState.offRouteAlert) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Sisa jarak: %.1f km • ETA: %d mnt".format(
                                    navigationState.remainingRouteDistanceMeters / 1000.0,
                                    navigationState.estimatedTimeRemainingSeconds / 60
                                ),
                                fontSize = 12.sp,
                                color = if (navigationState.offRouteAlert) Color(0xFFFFCDD2) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        IconButton(onClick = { viewModel.stopNavigation() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Stop Navigasi",
                                tint = if (navigationState.offRouteAlert) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // GPS & Compass Status Dashboard Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gps_status_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = "GPS",
                                tint = when (gpsState.accuracyLevel) {
                                    GpsAccuracyLevel.EXCELLENT, GpsAccuracyLevel.GOOD -> Color(0xFF2E7D32)
                                    GpsAccuracyLevel.FAIR -> Color(0xFFF57F17)
                                    GpsAccuracyLevel.POOR -> Color(0xFFC62828)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GPS STATUS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                        }

                        // Accuracy Pill
                        val (accText, accBg) = when (gpsState.accuracyLevel) {
                            GpsAccuracyLevel.EXCELLENT -> "Akurat (±${currentPoint?.accuracy?.toInt() ?: 0}m)" to Color(0xFF2E7D32)
                            GpsAccuracyLevel.GOOD -> "Bagus (±${currentPoint?.accuracy?.toInt() ?: 0}m)" to Color(0xFF388E3C)
                            GpsAccuracyLevel.FAIR -> "Sedang (±${currentPoint?.accuracy?.toInt() ?: 0}m)" to Color(0xFFF57F17)
                            GpsAccuracyLevel.POOR -> "Mencari Sinyal..." to Color(0xFF757575)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(accBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = accText, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Coordinates & Altitude Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "ELEVASI", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${currentPoint?.altitude?.toInt() ?: 0} mdpl",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "KOMPAS", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${compassState.azimuthDegrees.toInt()}° ${compassState.cardinal}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "KECEPATAN", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "%.1f km/h".format((currentPoint?.speed ?: 0f) * 3.6),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Coordinates String (DD & DMS)
                    if (currentPoint != null) {
                        Text(
                            text = "DD: %.5f°, %.5f°".format(currentPoint.latitude, currentPoint.longitude),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "DMS: ${GeoUtils.toDms(currentPoint.latitude, currentPoint.longitude)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Menunggu koordinat satelit GPS pertama...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Target Destination & Live Distance HUD
        if (targetDest != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToMap() }
                        .testTag("home_target_destination_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Flag,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TUJUAN: ${targetDest?.name}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            val distKm = (directDistance ?: 0.0) / 1000.0
                            val bearingDeg = targetBearing?.toInt() ?: 0
                            val cardinal = GeoUtils.bearingToCardinal(targetBearing ?: 0f)
                            val elevStr = if (elevDelta != null) (if (elevDelta!! >= 0) "+${elevDelta!!.toInt()}m" else "${elevDelta!!.toInt()}m") else "-"
                            val etaStr = etaMinutes?.let { if (it >= 60) "${it / 60}j ${it % 60}m" else "$it mnt" } ?: "-"
                            Text(
                                text = "Jarak: %.2f km • Arah: %s (%d°) • Elev: %s • ETA: %s".format(
                                    distKm, cardinal, bearingDeg, elevStr, etaStr
                                ),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Buka Peta",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Quick Action Shortcuts Grid
        item {
            Text(
                text = "NAVIGASI & FITUR UTAMA",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionCard(
                        title = "Mulai Tracking",
                        subtitle = "Rekam rute & GPS",
                        icon = Icons.Default.PlayArrow,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToTracking
                    )
                    QuickActionCard(
                        title = "Peta Outdoor",
                        subtitle = "OSM & Topo layer",
                        icon = Icons.Default.Map,
                        color = Color(0xFF00796B),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToMap
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionCard(
                        title = "Daftar Rute",
                        subtitle = "Impor GPX / KML",
                        icon = Icons.Default.Route,
                        color = Color(0xFFE65100),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToRoutes
                    )
                    QuickActionCard(
                        title = "Peta Offline",
                        subtitle = "Unduh area gunung",
                        icon = Icons.Default.DownloadForOffline,
                        color = Color(0xFF1565C0),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToOfflineMaps
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionCard(
                        title = "Waypoints",
                        subtitle = "Pos, Camp & Sumber Air",
                        icon = Icons.Default.Place,
                        color = Color(0xFF6A1B9A),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToWaypoints
                    )
                    QuickActionCard(
                        title = "EMERGENCY / SOS",
                        subtitle = "Koordinat & Sinyal",
                        icon = Icons.Default.Sos,
                        color = Color(0xFFC62828),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToEmergency
                    )
                }
            }
        }

        // Recent Hiking History
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RIWAYAT PENDAKIAN TERAKHIR",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                if (tracks.isNotEmpty()) {
                    TextButton(onClick = onNavigateToHistory) {
                        Text("Lihat Semua")
                    }
                }
            }

            if (tracks.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Hiking, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Belum ada riwayat pendakian yang disimpan", fontWeight = FontWeight.Medium)
                        Text("Tekan 'Mulai Tracking' saat Anda mulai berjalan", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tracks.take(3).forEach { track ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToHistory() },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Terrain, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = track.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        text = "%.2f km • Gain: +%d m • Max: %d mdpl".format(
                                            track.totalDistanceMeters / 1000.0,
                                            track.elevationGainMeters.toInt(),
                                            track.maxAltitudeMeters.toInt()
                                        ),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("action_${title.lowercase().replace(" ", "_")}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
