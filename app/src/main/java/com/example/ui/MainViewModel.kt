package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.HikeTrackEntity
import com.example.data.local.entity.OfflineRegionEntity
import com.example.data.local.entity.RouteEntity
import com.example.data.local.entity.WaypointEntity
import com.example.data.model.GpsPoint
import com.example.data.model.MapLayerType
import com.example.data.model.NavigationState
import com.example.data.model.WaypointType
import com.example.data.parser.GeoJsonParser
import com.example.data.parser.GpxExporter
import com.example.data.parser.GpxParser
import com.example.data.parser.KmlParser
import com.example.data.repository.HikingRepository
import com.example.data.tile.MapTileEngine
import com.example.sensor.CompassData
import com.example.sensor.CompassSensorManager
import com.example.sensor.CurrentGpsState
import com.example.sensor.LocationClient
import com.example.service.ActiveHikeState
import com.example.service.TrackingService
import com.example.service.TrackingStatus
import com.example.util.GeoUtils
import com.example.util.SampleRouteData
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream

data class TargetDestination(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
    val typeName: String = "Puncak"
)

data class MapMeasureState(
    val isMeasuringDistance: Boolean = false,
    val isMeasuringArea: Boolean = false,
    val measuredPoints: List<Pair<Double, Double>> = emptyList(),
    val totalDistanceMeters: Double = 0.0,
    val totalAreaSquareMeters: Double = 0.0
)

data class OfflineDownloadProgress(
    val isDownloading: Boolean = false,
    val regionName: String = "",
    val downloadedTiles: Int = 0,
    val totalTiles: Int = 0,
    val bytesDownloaded: Long = 0L
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = HikingRepository(application)
    private val locationClient = LocationClient(application)
    private val compassSensorManager = CompassSensorManager(application)

    // Device Sensors
    val gpsState: StateFlow<CurrentGpsState> = locationClient.getLocationUpdates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CurrentGpsState())

    val compassState: StateFlow<CompassData> = compassSensorManager.compassData

    // Active Tracking
    val activeHike: StateFlow<ActiveHikeState> = TrackingService.trackingState

    // Database Flows
    val allRoutes: StateFlow<List<RouteEntity>> = repository.allRoutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTracks: StateFlow<List<HikeTrackEntity>> = repository.allTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWaypoints: StateFlow<List<WaypointEntity>> = repository.allWaypoints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val offlineRegions: StateFlow<List<OfflineRegionEntity>> = repository.allOfflineRegions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Map UI State
    val selectedLayer = MutableStateFlow(MapLayerType.OPEN_STREET_MAP)
    val mapCenterLat = MutableStateFlow(-5.2853) // Default around Bawakaraeng
    val mapCenterLon = MutableStateFlow(119.9688)
    val mapZoom = MutableStateFlow(14.0)
    val mapBearing = MutableStateFlow(0f)
    val followGps = MutableStateFlow(true)

    // Active Navigation & Target Destination
    val selectedRouteForNavigation = MutableStateFlow<RouteEntity?>(null)
    val navigationState = MutableStateFlow(NavigationState())
    val offRouteToleranceMeters = MutableStateFlow(30.0) // 10m, 25m, 50m, 100m

    // Target Destination (Distance to Destination)
    val targetDestination = MutableStateFlow<TargetDestination?>(
        TargetDestination(
            name = "Puncak Gn. Bawakaraeng",
            latitude = -5.2853,
            longitude = 119.9688,
            altitudeMeters = 2830.0,
            typeName = "Puncak"
        )
    )
    val directDistanceToTargetMeters = MutableStateFlow<Double?>(null)
    val bearingToTargetDegrees = MutableStateFlow<Float?>(null)
    val elevationDeltaToTarget = MutableStateFlow<Double?>(null)
    val etaToTargetMinutes = MutableStateFlow<Int?>(null)

    // Measurement Tool State
    val measureState = MutableStateFlow(MapMeasureState())

    // Offline Download State
    val downloadProgress = MutableStateFlow(OfflineDownloadProgress())
    private var downloadCancelFlag = false

    // App Preferences
    val useMetricUnits = MutableStateFlow(true)
    val gpsIntervalSeconds = MutableStateFlow(2) // 1, 2, 5, 10, 30
    val isBatterySaverEnabled = MutableStateFlow(false)

    // User Feedback Message
    val userMessage = MutableStateFlow<String?>(null)

    init {
        compassSensorManager.startListening()

        // Monitor GPS & Active Navigation for off-route calculation
        viewModelScope.launch {
            gpsState.collect { gps ->
                val point = gps.point ?: return@collect

                // Update follow GPS
                if (followGps.value) {
                    mapCenterLat.value = point.latitude
                    mapCenterLon.value = point.longitude
                }

                // Update destination distance, bearing, elev delta, and ETA
                val target = targetDestination.value
                if (target != null) {
                    val dist = GeoUtils.calculateDistanceMeters(point.latitude, point.longitude, target.latitude, target.longitude)
                    val bearing = GeoUtils.calculateBearing(point.latitude, point.longitude, target.latitude, target.longitude)
                    val elevDelta = target.altitudeMeters - point.altitude
                    val speedMps = point.speed.coerceAtLeast(0.8f)
                    val etaMin = ((dist / speedMps) / 60.0).toInt().coerceAtLeast(1)

                    directDistanceToTargetMeters.value = dist
                    bearingToTargetDegrees.value = bearing
                    elevationDeltaToTarget.value = elevDelta
                    etaToTargetMinutes.value = etaMin
                }

                // If currently following a route, calculate off-route and progress
                val activeRoute = selectedRouteForNavigation.value
                if (activeRoute != null && navigationState.value.isNavigating) {
                    val routePoints = GeoUtils.jsonToPoints(activeRoute.coordinatesJson)
                    val offInfo = GeoUtils.calculateOffRoute(point.latitude, point.longitude, routePoints)

                    if (offInfo != null) {
                        val tolerance = offRouteToleranceMeters.value + (point.accuracy * 0.5)
                        val isOff = offInfo.distanceToRouteMeters > tolerance

                        val destPoint = routePoints.lastOrNull()
                        val distToDest = if (destPoint != null) {
                            GeoUtils.calculateDistanceMeters(point.latitude, point.longitude, destPoint.latitude, destPoint.longitude)
                        } else 0.0

                        // Calculate remaining distance along route from nearest segment
                        var remainingDist = 0.0
                        for (i in offInfo.nearestSegmentIndex until routePoints.size - 1) {
                            remainingDist += GeoUtils.calculateDistanceMeters(
                                routePoints[i].latitude, routePoints[i].longitude,
                                routePoints[i + 1].latitude, routePoints[i + 1].longitude
                            )
                        }

                        val elevDiff = (destPoint?.altitude ?: 0.0) - point.altitude
                        val speedMps = point.speed.coerceAtLeast(0.8f)
                        val etaSec = (remainingDist / speedMps).toLong()

                        navigationState.value = NavigationState(
                            isNavigating = true,
                            targetRouteName = activeRoute.name,
                            isOnTrack = !isOff,
                            crossTrackDistanceMeters = offInfo.distanceToRouteMeters,
                            bearingToRouteDegrees = offInfo.bearingToRouteDegrees,
                            nearestRouteLat = offInfo.nearestPoint.latitude,
                            nearestRouteLon = offInfo.nearestPoint.longitude,
                            distanceToDestinationMeters = distToDest,
                            remainingRouteDistanceMeters = remainingDist,
                            elevationDifferenceMeters = elevDiff,
                            estimatedTimeRemainingSeconds = etaSec,
                            offRouteAlert = isOff
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        compassSensorManager.stopListening()
    }

    // Tracking Control
    fun startHike(title: String = "Pendakian") {
        TrackingService.startTracking(getApplication(), title)
    }

    fun pauseHike() {
        TrackingService.pauseTracking(getApplication())
    }

    fun resumeHike() {
        TrackingService.resumeTracking(getApplication())
    }

    fun finishAndSaveHike(note: String = "") {
        val current = activeHike.value
        if (current.recordedPoints.isNotEmpty()) {
            viewModelScope.launch {
                val track = HikeTrackEntity(
                    title = current.title,
                    startTime = current.startTime,
                    endTime = System.currentTimeMillis(),
                    totalDistanceMeters = current.totalDistanceMeters,
                    durationSeconds = current.durationSeconds,
                    elevationGainMeters = current.elevationGainMeters,
                    elevationLossMeters = current.elevationLossMeters,
                    maxAltitudeMeters = current.maxAltitude,
                    minAltitudeMeters = current.minAltitude,
                    avgSpeedKmh = current.avgSpeedKmh,
                    maxSpeedKmh = current.currentSpeedKmh,
                    pointsJson = GeoUtils.pointsToJson(current.recordedPoints),
                    note = note
                )
                repository.saveTrack(track)
                TrackingService.stopTracking(getApplication())
                userMessage.value = "Pendakian berhasil disimpan ke Riwayat!"
            }
        } else {
            TrackingService.stopTracking(getApplication())
        }
    }

    fun discardHike() {
        TrackingService.stopTracking(getApplication())
    }

    // Navigation & Routes
    fun startNavigatingRoute(route: RouteEntity) {
        selectedRouteForNavigation.value = route
        val points = GeoUtils.jsonToPoints(route.coordinatesJson)
        if (points.isNotEmpty()) {
            mapCenterLat.value = points.first().latitude
            mapCenterLon.value = points.first().longitude
            val last = points.last()
            setTargetDestination(
                name = "Puncak ${route.name.substringBefore(" via")}",
                lat = last.latitude,
                lon = last.longitude,
                alt = route.highestPointMeters,
                type = "Puncak"
            )
        }
        navigationState.value = NavigationState(
            isNavigating = true,
            targetRouteName = route.name,
            isOnTrack = true
        )
        userMessage.value = "Memulai navigasi: ${route.name}"
    }

    fun stopNavigation() {
        selectedRouteForNavigation.value = null
        navigationState.value = NavigationState(isNavigating = false)
    }

    // Target Destination Management
    fun setTargetDestination(name: String, lat: Double, lon: Double, alt: Double, type: String = "Tujuan") {
        targetDestination.value = TargetDestination(name, lat, lon, alt, type)
        gpsState.value.point?.let { pt ->
            val dist = GeoUtils.calculateDistanceMeters(pt.latitude, pt.longitude, lat, lon)
            val bearing = GeoUtils.calculateBearing(pt.latitude, pt.longitude, lat, lon)
            val elevDelta = alt - pt.altitude
            val speedMps = pt.speed.coerceAtLeast(0.8f)
            val etaMin = ((dist / speedMps) / 60.0).toInt().coerceAtLeast(1)

            directDistanceToTargetMeters.value = dist
            bearingToTargetDegrees.value = bearing
            elevationDeltaToTarget.value = elevDelta
            etaToTargetMinutes.value = etaMin
        }
        userMessage.value = "Target tujuan diset: $name"
    }

    fun setTargetFromWaypoint(wpt: WaypointEntity) {
        setTargetDestination(wpt.name, wpt.latitude, wpt.longitude, wpt.elevationMeters, wpt.type)
    }

    fun clearTargetDestination() {
        targetDestination.value = null
        directDistanceToTargetMeters.value = null
        bearingToTargetDegrees.value = null
        elevationDeltaToTarget.value = null
        etaToTargetMinutes.value = null
        userMessage.value = "Target tujuan dinonaktifkan"
    }

    fun zoomIn() {
        mapZoom.value = (mapZoom.value + 1.0).coerceAtMost(18.5)
    }

    fun zoomOut() {
        mapZoom.value = (mapZoom.value - 1.0).coerceAtLeast(3.0)
    }

    fun recenterGps() {
        gpsState.value.point?.let { pt ->
            mapCenterLat.value = pt.latitude
            mapCenterLon.value = pt.longitude
            followGps.value = true
        }
    }

    // Import GPX / KML / GeoJSON from Stream
    fun importRouteFile(inputStream: InputStream, filename: String) {
        viewModelScope.launch {
            try {
                val lowerName = filename.lowercase()
                val result = when {
                    lowerName.endsWith(".kml") -> KmlParser.parse(inputStream, filename.substringBeforeLast("."))
                    lowerName.endsWith(".geojson") || lowerName.endsWith(".json") -> GeoJsonParser.parse(inputStream, filename.substringBeforeLast("."))
                    else -> GpxParser.parse(inputStream, filename.substringBeforeLast("."))
                }
                repository.saveRoute(result.route, result.waypoints)
                userMessage.value = "Rute '${result.route.name}' berhasil diimpor (${result.waypoints.size} waypoints)!"
            } catch (e: Exception) {
                userMessage.value = "Gagal impor: ${e.localizedMessage ?: "Format file tidak valid"}"
            }
        }
    }

    fun loadSampleRoute() {
        viewModelScope.launch {
            val (route, waypoints) = SampleRouteData.getSampleRoute()
            repository.saveRoute(route, waypoints)
            userMessage.value = "Sampel Gunung Bawakaraeng berhasil dimuat!"
        }
    }

    fun deleteRoute(id: Long) {
        viewModelScope.launch {
            if (selectedRouteForNavigation.value?.id == id) {
                stopNavigation()
            }
            repository.deleteRoute(id)
            userMessage.value = "Rute dihapus."
        }
    }

    fun deleteTrack(id: Long) {
        viewModelScope.launch {
            repository.deleteTrack(id)
            userMessage.value = "Riwayat pendakian dihapus."
        }
    }

    // Waypoints
    fun addWaypointAtCurrentGps(name: String, desc: String, type: WaypointType) {
        val pt = gpsState.value.point ?: return
        viewModelScope.launch {
            val wpt = WaypointEntity(
                name = name,
                description = desc,
                latitude = pt.latitude,
                longitude = pt.longitude,
                elevationMeters = pt.altitude,
                type = type.name,
                colorHex = type.defaultColorHex
            )
            repository.saveWaypoint(wpt)
            userMessage.value = "Waypoint '$name' ditambahkan!"
        }
    }

    fun addWaypointAtLocation(lat: Double, lon: Double, name: String, desc: String, type: WaypointType) {
        viewModelScope.launch {
            val wpt = WaypointEntity(
                name = name,
                description = desc,
                latitude = lat,
                longitude = lon,
                elevationMeters = 0.0,
                type = type.name,
                colorHex = type.defaultColorHex
            )
            repository.saveWaypoint(wpt)
            userMessage.value = "Waypoint '$name' berhasil ditandai di peta!"
        }
    }

    fun deleteWaypoint(id: Long) {
        viewModelScope.launch {
            repository.deleteWaypoint(id)
        }
    }

    // Measurement Tools
    fun toggleDistanceMeasurement() {
        val current = measureState.value
        measureState.value = current.copy(
            isMeasuringDistance = !current.isMeasuringDistance,
            isMeasuringArea = false,
            measuredPoints = emptyList(),
            totalDistanceMeters = 0.0
        )
    }

    fun toggleAreaMeasurement() {
        val current = measureState.value
        measureState.value = current.copy(
            isMeasuringArea = !current.isMeasuringArea,
            isMeasuringDistance = false,
            measuredPoints = emptyList(),
            totalAreaSquareMeters = 0.0
        )
    }

    fun addMeasurementPoint(lat: Double, lon: Double) {
        val current = measureState.value
        val list = current.measuredPoints + Pair(lat, lon)
        if (current.isMeasuringDistance) {
            val gpsList = list.map { GpsPoint(it.first, it.second) }
            val dist = GeoUtils.calculateTotalDistance(gpsList)
            measureState.value = current.copy(measuredPoints = list, totalDistanceMeters = dist)
        } else if (current.isMeasuringArea) {
            val area = GeoUtils.calculatePolygonAreaMeters(list)
            measureState.value = current.copy(measuredPoints = list, totalAreaSquareMeters = area)
        }
    }

    fun clearMeasurement() {
        measureState.value = MapMeasureState()
    }

    // Offline Map Download
    fun downloadOfflineMapRegion(
        name: String,
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        minZoom: Int = 11,
        maxZoom: Int = 15,
        layer: MapLayerType = selectedLayer.value
    ) {
        downloadCancelFlag = false
        val total = MapTileEngine.estimateTilesCount(minLat, maxLat, minLon, maxLon, minZoom, maxZoom)
        downloadProgress.value = OfflineDownloadProgress(
            isDownloading = true,
            regionName = name,
            downloadedTiles = 0,
            totalTiles = total,
            bytesDownloaded = 0L
        )

        viewModelScope.launch {
            val size = repository.tileEngine.downloadRegion(
                layer = layer,
                minLat = minLat,
                maxLat = maxLat,
                minLon = minLon,
                maxLon = maxLon,
                minZoom = minZoom,
                maxZoom = maxZoom,
                onProgress = { down, tot, bytes ->
                    downloadProgress.value = downloadProgress.value.copy(
                        downloadedTiles = down,
                        totalTiles = tot,
                        bytesDownloaded = bytes
                    )
                },
                isCancelled = { downloadCancelFlag }
            )

            val region = OfflineRegionEntity(
                name = name,
                minLat = minLat,
                maxLat = maxLat,
                minLon = minLon,
                maxLon = maxLon,
                minZoom = minZoom,
                maxZoom = maxZoom,
                layerType = layer.name,
                totalTiles = total,
                downloadedTiles = downloadProgress.value.downloadedTiles,
                sizeBytes = size,
                status = if (downloadCancelFlag) "CANCELLED" else "COMPLETED"
            )
            repository.saveOfflineRegion(region)
            downloadProgress.value = OfflineDownloadProgress(isDownloading = false)
            userMessage.value = "Peta offline '$name' selesai diunduh (${size / (1024 * 1024)} MB)!"
        }
    }

    fun cancelOfflineDownload() {
        downloadCancelFlag = true
        downloadProgress.value = downloadProgress.value.copy(isDownloading = false)
    }

    fun deleteOfflineRegion(region: OfflineRegionEntity) {
        viewModelScope.launch {
            repository.deleteOfflineRegion(region)
            userMessage.value = "Area offline '${region.name}' dihapus."
        }
    }

    fun clearTileCache() {
        viewModelScope.launch {
            val bytes = repository.tileEngine.clearAllTilesCache()
            userMessage.value = "Cache peta dibersihkan (${bytes / (1024 * 1024)} MB dibebaskan)."
        }
    }

    fun clearUserMessage() {
        userMessage.value = null
    }
}
