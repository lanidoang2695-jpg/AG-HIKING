package com.example.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val useMetric by viewModel.useMetricUnits.collectAsState()
    val gpsInterval by viewModel.gpsIntervalSeconds.collectAsState()
    val offRouteTolerance by viewModel.offRouteToleranceMeters.collectAsState()
    val isBatterySaver by viewModel.isBatterySaverEnabled.collectAsState()
    val gpsState by viewModel.gpsState.collectAsState()

    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    val diagnosticsReport = """
        === DIAGNOSTIK SISTEM AG HIKING PRO ===
        Aplikasi: AG HIKING Pro v1.0 (Build 1)
        Android OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
        Device: ${Build.MANUFACTURER} ${Build.MODEL}
        Device ABI: ${Build.SUPPORTED_ABIS.joinToString(", ")}
        Status GPS: ${if (gpsState.isGpsEnabled) "AKTIF" else "NONAKTIF"}
        Provider Terakhir: ${gpsState.provider}
        Akurasi GPS Terakhir: ±${gpsState.point?.accuracy?.toInt() ?: 0} m
        Map Engine: AG Outdoor Canvas Spherical Mercator Engine
        Database: SQLite / Room (Offline Local Storage)
        Fitur Inti: 100% Offline-First
    """.trimIndent()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // GPS & Tracking Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("PENGATURAN GPS & TRACKING", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Interval Pembaruan GPS:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 2, 5, 10, 30).forEach { sec ->
                            FilterChip(
                                selected = gpsInterval == sec,
                                onClick = { viewModel.gpsIntervalSeconds.value = sec },
                                label = { Text("${sec}s") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Toleransi Deteksi Keluar Jalur (Off-Route):", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(10.0, 25.0, 50.0, 100.0).forEach { dist ->
                            FilterChip(
                                selected = offRouteTolerance == dist,
                                onClick = { viewModel.offRouteToleranceMeters.value = dist },
                                label = { Text("${dist.toInt()}m") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Mode Hemat Baterai Outdoor", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Menyesuaikan interval GPS otomatis", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isBatterySaver,
                            onCheckedChange = {
                                viewModel.isBatterySaverEnabled.value = it
                                if (it) viewModel.gpsIntervalSeconds.value = 10 else viewModel.gpsIntervalSeconds.value = 2
                            }
                        )
                    }
                }
            }
        }

        // Unit System Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SATUAN PENGUKURAN", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (useMetric) "Metrik (km, m, km/h, mdpl)" else "Imperial (mil, ft, mph)")
                        Switch(
                            checked = useMetric,
                            onCheckedChange = { viewModel.useMetricUnits.value = it }
                        )
                    }
                }
            }
        }

        // Storage & Cache Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("MANAJEMEN PENYIMPANAN", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { viewModel.clearTileCache() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bersihkan Cache Peta")
                    }
                }
            }
        }

        // Diagnostics & About Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SISTEM & DIAGNOSTIK", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { showDiagnosticsDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lihat & Ekspor Diagnostik")
                    }
                }
            }
        }

        // Privacy Guarantee Notice
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Jaminan Privasi & Offline-First", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = "AG HIKING Pro tidak pernah mengunggah lokasi GPS, data rute, atau riwayat pendakian ke server mana pun. Seluruh data disimpan 100% secara lokal pada penyimpanan internal perangkat Anda.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showDiagnosticsDialog) {
        AlertDialog(
            onDismissRequest = { showDiagnosticsDialog = false },
            title = { Text("Diagnostik Perangkat") },
            text = {
                Text(diagnosticsReport, fontSize = 11.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Diagnostik AG HIKING Pro")
                            putExtra(Intent.EXTRA_TEXT, diagnosticsReport)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Ekspor Diagnostik"))
                        showDiagnosticsDialog = false
                    }
                ) {
                    Text("Ekspor / Bagikan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiagnosticsDialog = false }) { Text("Tutup") }
            }
        )
    }
}
