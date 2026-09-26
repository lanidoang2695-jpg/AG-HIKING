package com.example.ui.emergency

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.util.GeoUtils

@Composable
fun EmergencySosScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val gpsState by viewModel.gpsState.collectAsState()
    val pt = gpsState.point

    val latStr = pt?.let { "%.6f".format(it.latitude) } ?: "Mencari GPS..."
    val lonStr = pt?.let { "%.6f".format(it.longitude) } ?: "Mencari GPS..."
    val altStr = pt?.let { "${it.altitude.toInt()} mdpl" } ?: "-"
    val accStr = pt?.let { "±${it.accuracy.toInt()} m" } ?: "-"
    val dmsStr = pt?.let { GeoUtils.toDms(it.latitude, it.longitude) } ?: "-"

    val fullCoordinatesMessage = """
        [DARURAT - AG HIKING PRO]
        Posisi Saya Saat Ini:
        Latitude: $latStr
        Longitude: $lonStr
        Elevasi: $altStr
        Akurasi GPS: $accStr
        Format DMS: $dmsStr
        Google Maps: https://maps.google.com/?q=$latStr,$lonStr
    """.trimIndent()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("emergency_sos_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Red Emergency Header Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Emergency, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "TITIK DARURAT / SOS",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Bagikan koordinat presisi Anda kepada tim SAR / Basarnas atau rekan terdekat.",
                        color = Color(0xFFFFCDD2),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Live Coordinates HUD Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("emergency_coordinates_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("LOKASI SAAT INI (REALTIME)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("LATITUDE", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(latStr, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Column {
                            Text("LONGITUDE", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(lonStr, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("ELEVASI", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(altStr, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                        Column {
                            Text("AKURASI GPS", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(accStr, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("DMS: $dmsStr", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Action Buttons: Copy, Share, Call
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Copy Coordinates Button
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Koordinat Darurat", fullCoordinatesMessage)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Koordinat disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("copy_coordinates_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SALIN KOORDINAT (COPY)", fontWeight = FontWeight.Bold)
                }

                // Share Location Button
                Button(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, fullCoordinatesMessage)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Bagikan Lokasi Darurat"))
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("share_location_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("BAGIKAN KOORDINAT (SMS / WHATSAPP)", fontWeight = FontWeight.Bold)
                }

                // Call Basarnas Button
                Button(
                    onClick = {
                        val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:115"))
                        context.startActivity(callIntent)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("call_emergency_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PANGGIL BASARNAS (115)", fontWeight = FontWeight.Bold)
                }

                // Call 112 Button
                OutlinedButton(
                    onClick = {
                        val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                        context.startActivity(callIntent)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PhoneInTalk, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Panggil Darurat Nasional (112)")
                }
            }
        }

        // Offline Transparency Notice Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "PENTING: Koordinat di atas diambil langsung dari satelit GPS perangkat dan berfungsi 100% tanpa internet. Namun, pengiriman pesan SMS / telepon darurat tetap memerlukan sinyal seluler perangkat Anda.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
