package com.example.ui.tracking

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.TrackingStatus
import com.example.ui.MainViewModel
import com.example.util.GeoUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    viewModel: MainViewModel,
    onNavigateToMap: () -> Unit
) {
    val activeHike by viewModel.activeHike.collectAsState()
    val gpsState by viewModel.gpsState.collectAsState()
    val compassState by viewModel.compassState.collectAsState()
    val targetDest by viewModel.targetDestination.collectAsState()
    val directDistance by viewModel.directDistanceToTargetMeters.collectAsState()
    val targetBearing by viewModel.bearingToTargetDegrees.collectAsState()
    val elevDelta by viewModel.elevationDeltaToTarget.collectAsState()
    val etaMinutes by viewModel.etaToTargetMinutes.collectAsState()
    val allWaypoints by viewModel.allWaypoints.collectAsState()

    var showSaveDialog by remember { mutableStateOf(false) }
    var showSelectDestinationDialog by remember { mutableStateOf(false) }
    var hikeNote by remember { mutableStateOf("") }
    var hikeTitleInput by remember { mutableStateOf("Pendakian " + java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id", "ID")).format(java.util.Date())) }

    val pt = gpsState.point

    fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tracking_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Perekaman Header
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when (activeHike.status) {
                        TrackingStatus.RECORDING -> Color(0xFF1B5E20)
                        TrackingStatus.PAUSED -> Color(0xFFE65100)
                        TrackingStatus.STOPPED -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = when (activeHike.status) {
                                TrackingStatus.RECORDING -> "● SEDANG MEREKAM TRACKING"
                                TrackingStatus.PAUSED -> "❚❚ TRACKING DIJEDA (PAUSE)"
                                TrackingStatus.STOPPED -> "STATUS: SIAP MEREKAM GPS"
                            },
                            color = if (activeHike.status == TrackingStatus.STOPPED) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (activeHike.status != TrackingStatus.STOPPED) activeHike.title else "Tekan Mulai untuk Merekam",
                            color = if (activeHike.status == TrackingStatus.STOPPED) MaterialTheme.colorScheme.onSurface else Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // GPS Accuracy Indicator
                    val acc = pt?.accuracy?.toInt() ?: 0
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (activeHike.status == TrackingStatus.STOPPED) MaterialTheme.colorScheme.primaryContainer else Color.Black.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "GPS ±${acc}m",
                            color = if (activeHike.status == TrackingStatus.STOPPED) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // TARGET & JARAK KE TUJUAN (Distance to Destination HUD)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("destination_tracking_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Flag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TARGET & TUJUAN PENDAKIAN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        TextButton(
                            onClick = { showSelectDestinationDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Ganti", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = targetDest?.name ?: "Belum Memilih Tujuan",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Jarak ke Tujuan
                        Column {
                            Text("JARAK KE TUJUAN", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
                            val distKm = (directDistance ?: 0.0) / 1000.0
                            Text(
                                text = if (directDistance != null) "%.2f km".format(distKm) else "-- km",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Arah Kompas ke Tujuan
                        Column {
                            Text("ARAH KE TUJUAN", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Navigation,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .rotate(targetBearing ?: 0f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (targetBearing != null) "${targetBearing?.toInt()}° ${GeoUtils.bearingToCardinal(targetBearing ?: 0f)}" else "--",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Sisa Elevasi & ETA
                        Column {
                            Text("SISA ELEVASI / ETA", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
                            val elevStr = if (elevDelta != null) (if (elevDelta!! >= 0) "+${elevDelta!!.toInt()}m" else "${elevDelta!!.toInt()}m") else "--"
                            val etaStr = etaMinutes?.let { if (it >= 60) "${it / 60}j ${it % 60}m" else "$it mnt" } ?: "--"
                            Text(
                                text = "$elevStr • $etaStr",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // BACA LOKASI GPS KITA (Current Real-time Location Reading)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("current_location_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BACA LOKASI GPS KITA",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = if (pt != null) "Akurat ±${pt.accuracy.toInt()}m" else "Mencari GPS...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (pt != null) {
                        Text(
                            text = "Koordinat: %.6f°, %.6f°".format(pt.latitude, pt.longitude),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Format DMS: ${GeoUtils.toDms(pt.latitude, pt.longitude)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Menunggu data satelit GPS perangkat...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // HUD Jarak Tempuh & Durasi Waktu
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("TOTAL JARAK TEMPUH", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "%.2f".format(activeHike.totalDistanceMeters / 1000.0),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text("KILOMETER", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("DURASI WAKTU", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatDuration(activeHike.durationSeconds),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text("JAM : MNT : DETIK", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Metrik Lengkap (Elevasi, Speed, Gain/Loss)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricItem("ELEVASI SAAT INI", "${activeHike.currentAltitude.toInt()} mdpl", Color(0xFF1B5E20))
                        MetricItem("ELEVATION GAIN", "+${activeHike.elevationGainMeters.toInt()} m", Color(0xFF2E7D32))
                        MetricItem("ELEVATION LOSS", "-${activeHike.elevationLossMeters.toInt()} m", Color(0xFFC62828))
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricItem("KECEPATAN", "%.1f km/h".format(activeHike.currentSpeedKmh), MaterialTheme.colorScheme.primary)
                        MetricItem("RATA-RATA", "%.1f km/h".format(activeHike.avgSpeedKmh), MaterialTheme.colorScheme.onSurface)
                        MetricItem("PUNCAK MAKS", "${activeHike.maxAltitude.toInt()} mdpl", Color(0xFFE65100))
                    }
                }
            }
        }

        // Grafik Profil Elevasi Realtime
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PROFIL ELEVASI REALTIME",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val points = activeHike.recordedPoints
                    if (points.size >= 2) {
                        val alts = points.map { it.altitude }
                        val minA = alts.minOrNull() ?: 0.0
                        val maxA = (alts.maxOrNull() ?: 1.0).coerceAtLeast(minA + 10.0)

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        ) {
                            val w = size.width
                            val h = size.height
                            val path = Path()

                            points.forEachIndexed { index, p ->
                                val x = (index.toFloat() / (points.size - 1)) * w
                                val normalizedAlt = ((p.altitude - minA) / (maxA - minA)).toFloat().coerceIn(0f, 1f)
                                val y = h - (normalizedAlt * (h - 20f)) - 10f
                                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }

                            drawPath(path, Color(0xFF2E7D32), style = Stroke(width = 4f))
                            drawLine(Color.Gray.copy(alpha = 0.3f), Offset(0f, 10f), Offset(w, 10f), strokeWidth = 1f)
                            drawLine(Color.Gray.copy(alpha = 0.3f), Offset(0f, h - 10f), Offset(w, h - 10f), strokeWidth = 1f)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Min: ${minA.toInt()} mdpl", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Max: ${maxA.toInt()} mdpl", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Grafik elevasi akan tergambar otomatis saat Anda berjalan",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Tombol Kontrol Tracking (START / PAUSE / RESUME / SELESAI)
        item {
            when (activeHike.status) {
                TrackingStatus.STOPPED -> {
                    Button(
                        onClick = { viewModel.startHike(hikeTitleInput) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("start_tracking_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MULAI TRACKING GPS", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                TrackingStatus.RECORDING -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.pauseHike() },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("pause_tracking_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("JEDA (PAUSE)", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showSaveDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("stop_tracking_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SELESAI", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                TrackingStatus.PAUSED -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.resumeHike() },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("resume_tracking_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("LANJUTKAN", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showSaveDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SELESAI", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Buka Peta
        item {
            OutlinedButton(
                onClick = onNavigateToMap,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lihat Jejak di Peta", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // Dialog Pilih Target Tujuan Navigasi
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

    // Dialog Simpan Pendakian
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Simpan Hasil Pendakian?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Total Jarak: %.2f km • Waktu: %s • Gain: +%d m".format(
                            activeHike.totalDistanceMeters / 1000.0,
                            formatDuration(activeHike.durationSeconds),
                            activeHike.elevationGainMeters.toInt()
                        ),
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = hikeNote,
                        onValueChange = { hikeNote = it },
                        label = { Text("Catatan / Cuaca / Jalur (opsional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.finishAndSaveHike(hikeNote)
                        showSaveDialog = false
                    }
                ) {
                    Text("Simpan ke Riwayat")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.discardHike()
                    showSaveDialog = false
                }) {
                    Text("Buang", color = Color(0xFFC62828))
                }
            }
        )
    }
}

@Composable
private fun MetricItem(label: String, value: String, color: Color) {
    Column {
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
    }
}
