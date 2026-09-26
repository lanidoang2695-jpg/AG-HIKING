package com.example.ui.tools

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.util.GeoUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HikingToolsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val compassState by viewModel.compassState.collectAsState()
    val gpsState by viewModel.gpsState.collectAsState()

    // Flashlight State
    var isFlashlightOn by remember { mutableStateOf(false) }
    var isSosStrobeActive by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Coordinate Converter State
    var inputLat by remember { mutableStateOf("") }
    var inputLon by remember { mutableStateOf("") }
    var convertedDms by remember { mutableStateOf("") }
    var convertedUtm by remember { mutableStateOf("") }

    // Stopwatch State
    var stopwatchRunning by remember { mutableStateOf(false) }
    var stopwatchSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(stopwatchRunning) {
        while (stopwatchRunning) {
            delay(1000L)
            stopwatchSeconds++
        }
    }

    // Battery Manager
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    val batteryPct = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1

    // Torch helper
    fun toggleTorch(on: Boolean) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, on)
                isFlashlightOn = on
            }
        } catch (_: Exception) {}
    }

    // SOS Strobe helper
    LaunchedEffect(isSosStrobeActive) {
        if (isSosStrobeActive) {
            // Morse SOS: ... --- ...
            val timings = listOf(
                100L, 100L, 100L, 100L, 100L, 200L, // S
                300L, 100L, 300L, 100L, 300L, 200L, // O
                100L, 100L, 100L, 100L, 100L, 600L  // S
            )
            while (isSosStrobeActive) {
                for (i in timings.indices) {
                    if (!isSosStrobeActive) break
                    val shouldLight = (i % 2 == 0)
                    toggleTorch(shouldLight)
                    delay(timings[i])
                }
            }
            toggleTorch(false)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tools_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Compass Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("compass_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("KOMPAS OUTDOOR", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${compassState.azimuthDegrees.toInt()}° ${compassState.cardinal}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Rotating Compass Dial Canvas
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val center = Offset(w / 2f, h / 2f)
                            val radius = (w / 2f) - 16f

                            // Dial marks
                            rotate(degrees = -compassState.azimuthDegrees, pivot = center) {
                                for (deg in 0 until 360 step 30) {
                                    val rad = Math.toRadians(deg.toDouble())
                                    val isCardinal = deg % 90 == 0
                                    val lineLen = if (isCardinal) 16f else 8f
                                    val startX = center.x + ((radius - lineLen) * sin(rad)).toFloat()
                                    val startY = center.y - ((radius - lineLen) * cos(rad)).toFloat()
                                    val endX = center.x + (radius * sin(rad)).toFloat()
                                    val endY = center.y - (radius * cos(rad)).toFloat()

                                    drawLine(
                                        color = if (deg == 0) Color(0xFFD32F2F) else Color.Gray,
                                        start = Offset(startX, startY),
                                        end = Offset(endX, endY),
                                        strokeWidth = if (isCardinal) 3f else 1.5f
                                    )
                                }
                            }

                            // Needle
                            rotate(degrees = -compassState.azimuthDegrees, pivot = center) {
                                val northPath = Path().apply {
                                    moveTo(center.x, center.y - radius + 10f)
                                    lineTo(center.x - 12f, center.y)
                                    lineTo(center.x + 12f, center.y)
                                    close()
                                }
                                val southPath = Path().apply {
                                    moveTo(center.x, center.y + radius - 10f)
                                    lineTo(center.x - 12f, center.y)
                                    lineTo(center.x + 12f, center.y)
                                    close()
                                }
                                drawPath(northPath, Color(0xFFD32F2F))
                                drawPath(southPath, Color(0xFF90A4AE))
                            }

                            drawCircle(Color.White, radius = 6f, center = center)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Pitch: ${compassState.pitch.toInt()}° • Roll: ${compassState.roll.toInt()}°",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Coordinate Converter Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("KONVERTER KOORDINAT", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        TextButton(onClick = {
                            gpsState.point?.let {
                                inputLat = "%.6f".format(it.latitude)
                                inputLon = "%.6f".format(it.longitude)
                                convertedDms = GeoUtils.toDms(it.latitude, it.longitude)
                                convertedUtm = GeoUtils.toUtmString(it.latitude, it.longitude)
                            }
                        }) {
                            Text("Gunakan GPS")
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = inputLat,
                            onValueChange = { inputLat = it },
                            label = { Text("Latitude") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = inputLon,
                            onValueChange = { inputLon = it },
                            label = { Text("Longitude") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val lat = inputLat.toDoubleOrNull()
                            val lon = inputLon.toDoubleOrNull()
                            if (lat != null && lon != null) {
                                convertedDms = GeoUtils.toDms(lat, lon)
                                convertedUtm = GeoUtils.toUtmString(lat, lon)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Konversi ke DMS & UTM")
                    }

                    if (convertedDms.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("DMS: $convertedDms", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("UTM: $convertedUtm", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Flashlight & SOS Strobe Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SENTER & SINYAL DARURAT", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                isSosStrobeActive = false
                                toggleTorch(!isFlashlightOn)
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFlashlightOn) Color(0xFFFBC02D) else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FlashlightOn, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isFlashlightOn) "MATIKAN" else "SENTER")
                        }

                        Button(
                            onClick = {
                                if (isSosStrobeActive) {
                                    isSosStrobeActive = false
                                    toggleTorch(false)
                                } else {
                                    isSosStrobeActive = true
                                }
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSosStrobeActive) Color(0xFFD32F2F) else MaterialTheme.colorScheme.secondary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Sos, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isSosStrobeActive) "STOP SOS" else "STROBE SOS")
                        }
                    }
                }
            }
        }

        // Hiking Stopwatch & Device Status Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("STOPWATCH PENDAKIAN", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))

                    val h = stopwatchSeconds / 3600
                    val m = (stopwatchSeconds % 3600) / 60
                    val s = stopwatchSeconds % 60
                    Text(
                        text = "%02d:%02d:%02d".format(h, m, s),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { stopwatchRunning = !stopwatchRunning },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (stopwatchRunning) Color(0xFFE65100) else Color(0xFF2E7D32)
                            )
                        ) {
                            Text(if (stopwatchRunning) "PAUSE" else "MULAI")
                        }
                        OutlinedButton(onClick = {
                            stopwatchRunning = false
                            stopwatchSeconds = 0L
                        }) {
                            Text("RESET")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Baterai Perangkat: $batteryPct%", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("Android: ${Build.VERSION.RELEASE}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
