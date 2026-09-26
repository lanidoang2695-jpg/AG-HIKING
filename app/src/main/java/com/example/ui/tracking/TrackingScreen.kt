package com.example.ui.tracking

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun TrackingScreen(
    viewModel: MainViewModel,
    onNavigateToMap: () -> Unit
) {
    val activeHike by viewModel.activeHike.collectAsState()
    val gpsState by viewModel.gpsState.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }
    var hikeNote by remember { mutableStateOf("") }
    var hikeTitleInput by remember { mutableStateOf("Pendakian " + java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(java.util.Date())) }

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
        // Top Status Header
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
                                TrackingStatus.RECORDING -> "● SEDANG MEREKAM GPS"
                                TrackingStatus.PAUSED -> "❚❚ TRACKING DI-PAUSE"
                                TrackingStatus.STOPPED -> "STATUS: SIAP TRACKING"
                            },
                            color = if (activeHike.status == TrackingStatus.STOPPED) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (activeHike.status != TrackingStatus.STOPPED) activeHike.title else "Tekan Mulai untuk Merekam",
                            color = if (activeHike.status == TrackingStatus.STOPPED) MaterialTheme.colorScheme.onSurface else Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // GPS Accuracy Indicator
                    val acc = gpsState.point?.accuracy?.toInt() ?: 0
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

        // Primary HUD Display (Distance & Duration)
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("TOTAL JARAK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "%.2f".format(activeHike.totalDistanceMeters / 1000.0),
                            fontSize = 38.sp,
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
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text("JAM : MNT : DETIK", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Secondary Metrics Grid (Elevation, Speed, Gain/Loss)
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

        // Realtime Elevation Mini Chart
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

                            points.forEachIndexed { index, pt ->
                                val x = (index.toFloat() / (points.size - 1)) * w
                                val normalizedAlt = ((pt.altitude - minA) / (maxA - minA)).toFloat().coerceIn(0f, 1f)
                                val y = h - (normalizedAlt * (h - 20f)) - 10f
                                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }

                            drawPath(path, Color(0xFF2E7D32), style = Stroke(width = 4f))
                            // Draw horizontal min/max guide
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

        // Action Buttons Row (START / PAUSE / RESUME / STOP)
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
                        Text("START TRACKING", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                            Text("PAUSE", fontWeight = FontWeight.Bold)
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

        // View on Map Button
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

    // Save Hike Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Simpan Hasil Pendakian?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Total: %.2f km • Waktu: %s • Gain: +%d m".format(
                            activeHike.totalDistanceMeters / 1000.0,
                            formatDuration(activeHike.durationSeconds),
                            activeHike.elevationGainMeters.toInt()
                        ),
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = hikeNote,
                        onValueChange = { hikeNote = it },
                        label = { Text("Catatan / Cuaca / Rute (opsional)") },
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
