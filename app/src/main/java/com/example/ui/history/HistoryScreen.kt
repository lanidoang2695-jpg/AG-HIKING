package com.example.ui.history

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.HikeTrackEntity
import com.example.data.parser.GpxExporter
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onNavigateToMap: () -> Unit
) {
    val context = LocalContext.current
    val tracks by viewModel.allTracks.collectAsState()
    var selectedTrackForExport by remember { mutableStateOf<HikeTrackEntity?>(null) }

    fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID"))

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("history_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "RIWAYAT PENDAKIAN (${tracks.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (tracks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Hiking, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Belum ada riwayat pendakian tersimpan", fontWeight = FontWeight.Bold)
                        Text("Setiap aktivitas yang selesai direkam akan otomatis tersimpan di sini", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(tracks, key = { it.id }) { track ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("track_item_${track.id}"),
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
                            Text(text = track.title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Text(
                                text = dateFormat.format(Date(track.startTime)),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (track.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = track.note, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Metric Stats Row
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            HistoryMetric("JARAK", "%.2f km".format(track.totalDistanceMeters / 1000.0))
                            HistoryMetric("DURASI", formatDuration(track.durationSeconds))
                            HistoryMetric("ELEV GAIN", "+%d m".format(track.elevationGainMeters.toInt()))
                            HistoryMetric("PUNCAK", "%d mdpl".format(track.maxAltitudeMeters.toInt()))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.deleteTrack(track.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = Color(0xFFC62828))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedButton(
                                onClick = { selectedTrackForExport = track },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ekspor File")
                            }
                        }
                    }
                }
            }
        }
    }

    // Export Format Selection Dialog
    selectedTrackForExport?.let { track ->
        AlertDialog(
            onDismissRequest = { selectedTrackForExport = null },
            title = { Text("Ekspor Riwayat Pendakian") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pilih format file untuk dibagikan:")
                    Button(
                        onClick = {
                            val gpxData = GpxExporter.toGpx(track)
                            shareTextFile(context, GpxExporter.generateFileName(track.title, "gpx"), gpxData)
                            selectedTrackForExport = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Format GPX (.gpx)")
                    }
                    Button(
                        onClick = {
                            val kmlData = GpxExporter.toKml(track)
                            shareTextFile(context, GpxExporter.generateFileName(track.title, "kml"), kmlData)
                            selectedTrackForExport = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Format KML Google Earth (.kml)")
                    }
                    Button(
                        onClick = {
                            val geoJsonData = GpxExporter.toGeoJson(track)
                            shareTextFile(context, GpxExporter.generateFileName(track.title, "geojson"), geoJsonData)
                            selectedTrackForExport = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Format GeoJSON (.geojson)")
                    }
                    Button(
                        onClick = {
                            val csvData = GpxExporter.toCsv(track)
                            shareTextFile(context, GpxExporter.generateFileName(track.title, "csv"), csvData)
                            selectedTrackForExport = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Format CSV (.csv)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedTrackForExport = null }) { Text("Batal") }
            }
        )
    }
}

private fun shareTextFile(context: Context, filename: String, content: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, filename)
        putExtra(Intent.EXTRA_TEXT, content)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Ekspor $filename"))
}

@Composable
private fun HistoryMetric(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
