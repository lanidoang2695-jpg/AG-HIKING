package com.example.ui.waypoints

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.WaypointEntity
import com.example.data.model.WaypointType
import com.example.ui.MainViewModel
import com.example.util.GeoUtils

@Composable
fun WaypointsScreen(
    viewModel: MainViewModel,
    onNavigateToMap: () -> Unit
) {
    val allWaypoints by viewModel.allWaypoints.collectAsState()
    val gpsState by viewModel.gpsState.collectAsState()
    var selectedFilterType by remember { mutableStateOf<WaypointType?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredWaypoints = remember(allWaypoints, selectedFilterType) {
        if (selectedFilterType == null) allWaypoints
        else allWaypoints.filter { it.type.equals(selectedFilterType?.name, ignoreCase = true) }
    }

    Box(modifier = Modifier.fillMaxSize().testTag("waypoints_screen")) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Filter Types Carousel
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilterType == null,
                            onClick = { selectedFilterType = null },
                            label = { Text("Semua (${allWaypoints.size})") }
                        )
                    }
                    items(WaypointType.entries) { type ->
                        FilterChip(
                            selected = selectedFilterType == type,
                            onClick = { selectedFilterType = if (selectedFilterType == type) null else type },
                            label = { Text("${type.iconSymbol} ${type.displayName.substringBefore(" /")}") }
                        )
                    }
                }
            }

            if (filteredWaypoints.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Tidak ada waypoint yang cocok", fontWeight = FontWeight.Bold)
                            Text("Gunakan tombol '+' di bawah untuk menandai lokasi penting", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(filteredWaypoints, key = { it.id }) { wpt ->
                    val type = WaypointType.fromString(wpt.type)
                    val distFromGps = gpsState.point?.let {
                        GeoUtils.calculateDistanceMeters(it.latitude, it.longitude, wpt.latitude, wpt.longitude)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.mapCenterLat.value = wpt.latitude
                                viewModel.mapCenterLon.value = wpt.longitude
                                viewModel.followGps.value = false
                                onNavigateToMap()
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(type.defaultColorHex)).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(type.iconSymbol, fontSize = 20.sp)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = wpt.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                if (wpt.description.isNotBlank()) {
                                    Text(text = wpt.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = "%.5f°, %.5f° • %d mdpl".format(wpt.latitude, wpt.longitude, wpt.elevationMeters.toInt()),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (distFromGps != null) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (distFromGps > 1000) "%.1f km".format(distFromGps / 1000.0) else "${distFromGps.toInt()} m",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                    Text("Jarak", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            IconButton(onClick = { viewModel.deleteWaypoint(wpt.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = Color(0xFFC62828))
                            }
                        }
                    }
                }
            }
        }

        // Add Waypoint FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_waypoint_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Tambah Waypoint")
        }

        // Add Dialog
        if (showAddDialog) {
            var name by remember { mutableStateOf("") }
            var desc by remember { mutableStateOf("") }
            var selectedType by remember { mutableStateOf(WaypointType.POS) }

            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Tandai Waypoint Saat Ini") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val pt = gpsState.point
                        if (pt != null) {
                            Text("Koordinat GPS: %.5f°, %.5f° (%d mdpl)".format(pt.latitude, pt.longitude, pt.altitude.toInt()), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        } else {
                            Text("Menunggu sinyal GPS...", fontSize = 11.sp, color = Color(0xFFC62828))
                        }
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nama Waypoint") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = desc,
                            onValueChange = { desc = it },
                            label = { Text("Deskripsi") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Tipe Titik:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(WaypointType.SUMMIT, WaypointType.CAMP, WaypointType.WATER, WaypointType.POS).forEach { t ->
                                FilterChip(
                                    selected = selectedType == t,
                                    onClick = { selectedType = t },
                                    label = { Text(t.displayName.substringBefore(" /")) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                viewModel.addWaypointAtCurrentGps(name, desc, selectedType)
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Batal") }
                }
            )
        }
    }
}
