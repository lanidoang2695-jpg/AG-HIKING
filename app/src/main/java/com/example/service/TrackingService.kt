package com.example.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.GpsPoint
import com.example.sensor.LocationClient
import com.example.util.GeoUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class TrackingStatus {
    STOPPED,
    RECORDING,
    PAUSED
}

data class ActiveHikeState(
    val status: TrackingStatus = TrackingStatus.STOPPED,
    val title: String = "Pendakian Baru",
    val startTime: Long = 0L,
    val durationSeconds: Long = 0L,
    val totalDistanceMeters: Double = 0.0,
    val currentAltitude: Double = 0.0,
    val elevationGainMeters: Double = 0.0,
    val elevationLossMeters: Double = 0.0,
    val maxAltitude: Double = 0.0,
    val minAltitude: Double = 0.0,
    val currentSpeedKmh: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
    val currentAccuracyMeters: Float = 0f,
    val recordedPoints: List<GpsPoint> = emptyList()
)

class TrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var locationJob: Job? = null
    private var timerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var locationClient: LocationClient

    companion object {
        const val CHANNEL_ID = "ag_hiking_tracking_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_HIKE_TITLE = "EXTRA_HIKE_TITLE"

        private val _trackingState = MutableStateFlow(ActiveHikeState())
        val trackingState: StateFlow<ActiveHikeState> = _trackingState.asStateFlow()

        fun startTracking(context: Context, title: String = "Pendakian") {
            val intent = Intent(context, TrackingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_HIKE_TITLE, title)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseTracking(context: Context) {
            val intent = Intent(context, TrackingService::class.java).apply { action = ACTION_PAUSE }
            context.startService(intent)
        }

        fun resumeTracking(context: Context) {
            val intent = Intent(context, TrackingService::class.java).apply { action = ACTION_RESUME }
            context.startService(intent)
        }

        fun stopTracking(context: Context) {
            val intent = Intent(context, TrackingService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationClient = LocationClient(this)
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AGHiking::TrackingWakeLock")?.apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val title = intent.getStringExtra(EXTRA_HIKE_TITLE) ?: "Pendakian"
                startForegroundTracking(title)
            }
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_STICKY
    }

    @SuppressLint("WakelockTimeout")
    private fun startForegroundTracking(title: String) {
        val now = System.currentTimeMillis()
        _trackingState.value = ActiveHikeState(
            status = TrackingStatus.RECORDING,
            title = title,
            startTime = now,
            durationSeconds = 0L,
            recordedPoints = emptyList()
        )

        wakeLock?.acquire()

        val notification = buildNotification("Merekam Pendakian...", "Jarak: 0.00 km | Waktu: 00:00:00")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startTimer()
        startLocationTracking()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                if (_trackingState.value.status == TrackingStatus.RECORDING) {
                    val newSec = _trackingState.value.durationSeconds + 1
                    val distKm = _trackingState.value.totalDistanceMeters / 1000.0
                    val avgSpeed = if (newSec > 0) (distKm / (newSec / 3600.0)) else 0.0

                    _trackingState.value = _trackingState.value.copy(
                        durationSeconds = newSec,
                        avgSpeedKmh = avgSpeed
                    )

                    if (newSec % 5 == 0L) {
                        updateNotification()
                    }
                }
            }
        }
    }

    private fun startLocationTracking() {
        locationJob?.cancel()
        locationJob = serviceScope.launch {
            locationClient.getLocationUpdates(intervalMs = 2000L).collect { state ->
                val point = state.point ?: return@collect
                if (_trackingState.value.status != TrackingStatus.RECORDING) return@collect

                // GPS noise filtering: reject points with poor accuracy > 40m for distance accumulation
                val current = _trackingState.value
                val points = current.recordedPoints.toMutableList()

                var newDist = current.totalDistanceMeters
                var gain = current.elevationGainMeters
                var loss = current.elevationLossMeters

                if (points.isNotEmpty()) {
                    val lastPoint = points.last()
                    val segDist = GeoUtils.calculateDistanceMeters(
                        lastPoint.latitude, lastPoint.longitude,
                        point.latitude, point.longitude
                    )

                    // Reject crazy teleports or stationary noise
                    if (segDist in 1.5..500.0) {
                        newDist += segDist
                        val altDiff = point.altitude - lastPoint.altitude
                        if (Math.abs(altDiff) >= 2.5) {
                            if (altDiff > 0) gain += altDiff else loss += Math.abs(altDiff)
                        }
                    }
                }

                points.add(point)
                val curSpeedKmh = (point.speed * 3.6).coerceAtLeast(0.0)
                val maxAlt = if (current.maxAltitude == 0.0) point.altitude else maxOf(current.maxAltitude, point.altitude)
                val minAlt = if (current.minAltitude == 0.0) point.altitude else minOf(current.minAltitude, point.altitude)

                _trackingState.value = current.copy(
                    totalDistanceMeters = newDist,
                    currentAltitude = point.altitude,
                    elevationGainMeters = gain,
                    elevationLossMeters = loss,
                    maxAltitude = maxAlt,
                    minAltitude = minAlt,
                    currentSpeedKmh = curSpeedKmh,
                    currentAccuracyMeters = point.accuracy,
                    recordedPoints = points
                )
            }
        }
    }

    private fun pauseRecording() {
        _trackingState.value = _trackingState.value.copy(status = TrackingStatus.PAUSED)
        updateNotification()
    }

    private fun resumeRecording() {
        _trackingState.value = _trackingState.value.copy(status = TrackingStatus.RECORDING)
        updateNotification()
    }

    private fun stopRecording() {
        _trackingState.value = _trackingState.value.copy(status = TrackingStatus.STOPPED)
        timerJob?.cancel()
        locationJob?.cancel()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    private fun updateNotification() {
        val state = _trackingState.value
        val distKm = state.totalDistanceMeters / 1000.0
        val content = "Jarak: %.2f km | Waktu: %s | Elev: %d m".format(
            distKm,
            formatDuration(state.durationSeconds),
            state.currentAltitude.toInt()
        )
        val statusText = if (state.status == TrackingStatus.PAUSED) "[PAUSED] AG HIKING Pro" else "AG HIKING Pro Tracking"
        val notification = buildNotification(statusText, content)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(title: String, content: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isPaused = _trackingState.value.status == TrackingStatus.PAUSED
        val toggleActionIntent = Intent(this, TrackingService::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val togglePendingIntent = PendingIntent.getService(
            this, 1, toggleActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopActionIntent = Intent(this, TrackingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (isPaused) "RESUME" else "PAUSE",
                togglePendingIntent
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AG HIKING GPS Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi status rekaman rute GPS dan pendakian outdoor"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        super.onDestroy()
    }
}
