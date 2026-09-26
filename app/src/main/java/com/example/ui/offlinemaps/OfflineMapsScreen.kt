package com.example.ui.offlinemaps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MapLayerType
import com.example.data.tile.MapTileEngine
import com.example.ui.MainViewModel

data class MountainAreaPreset(
    val name: String,
    val description: String,
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double
)

val MOUNTAIN_PRESETS = listOf(
    MountainAreaPreset("Gunung Bawakaraeng & Malino", "Sulawesi Selatan • 2.830 mdpl", -5.32, -5.25, 119.88, 119.98),
    MountainAreaPreset("Gunung Rinjani & Segara Anak", "Lombok, NTB • 3.726 mdpl", -8.46, -8.37, 116.42, 116.52),
    MountainAreaPreset("Gunung Semeru & Ranu Kumbolo", "Jawa Timur • 3.676 mdpl", -8.15, -8.06, 112.88, 112.96),
    MountainAreaPreset("Gunung Gede & Pangrango", "Jawa Barat • 3.008 mdpl", -6.82, -6.74, 106.94, 107.03)
)

@Composable
fun OfflineMapsScreen(viewModel: MainViewModel) {
    val regions by viewModel.offlineRegions.collectAsState()
    val progress by viewModel.downloadProgress.collectAsState()
    var selectedPreset by remember { mutableStateOf(MOUNTAIN_PRESETS[0]) }

    val estimatedTiles = remember(selectedPreset) {
        MapTileEngine.estimateTilesCount(
            selectedPreset.minLat, selectedPreset.maxLat,
            selectedPreset.minLon, selectedPreset.maxLon,
            11, 14
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("offline_maps_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Download Card
        if (progress.isDownloading) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("download_progress_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MENGUNDUH: ${progress.regionName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { viewModel.cancelOfflineDownload() }) {
                                Icon(Icons.Default.Close, contentDescription = "Batal", tint = Color(0xFFC62828))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val frac = if (progress.totalTiles > 0) progress.downloadedTiles.toFloat() / progress.totalTiles else 0f
                        LinearProgressIndicator(
                            progress = { frac },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "${progress.downloadedTiles} / ${progress.totalTiles} tile (${(frac * 100).toInt()}%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "%.1f MB".format(progress.bytesDownloaded / (1024.0 * 1024.0)),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Download Area Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DownloadForOffline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("UNDUH PETA OFFLINE", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Pilih Kawasan Gunung:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))

                    MOUNTAIN_PRESETS.forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPreset == preset,
                                onClick = { selectedPreset = preset }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(preset.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(preset.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Estimasi: $estimatedTiles tiles", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Ukuran: ~${(estimatedTiles * 22) / 1024} MB (Zoom 11-14)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = {
                                viewModel.downloadOfflineMapRegion(
                                    name = selectedPreset.name,
                                    minLat = selectedPreset.minLat,
                                    maxLat = selectedPreset.maxLat,
                                    minLon = selectedPreset.minLon,
                                    maxLon = selectedPreset.maxLon,
                                    minZoom = 11,
                                    maxZoom = 14
                                )
                            },
                            enabled = !progress.isDownloading,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Unduh Sekarang")
                        }
                    }
                }
            }
        }

        // Stored Offline Maps List
        item {
            Text(
                text = "PETA OFFLINE TERSIMPAN (${regions.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (regions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Belum ada area peta offline", fontWeight = FontWeight.Bold)
                        Text("Unduh area gunung di atas untuk penggunaan tanpa sinyal internet", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(regions, key = { it.id }) { region ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = region.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = "Ukuran: %.1f MB • %d Tiles • Zoom %d-%d".format(
                                    region.sizeBytes / (1024.0 * 1024.0),
                                    region.totalTiles,
                                    region.minZoom,
                                    region.maxZoom
                                ),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("Status: Available Offline", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))
                        }
                        IconButton(onClick = { viewModel.deleteOfflineRegion(region) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = Color(0xFFC62828))
                        }
                    }
                }
            }
        }

        // Cache Management Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Cache Tile Otomatis", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Bersihkan tile yang pernah dibuka", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = { viewModel.clearTileCache() }) {
                        Text("Bersihkan Cache")
                    }
                }
            }
        }
    }
}
