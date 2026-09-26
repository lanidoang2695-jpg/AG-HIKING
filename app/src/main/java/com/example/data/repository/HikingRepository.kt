package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.HikeTrackEntity
import com.example.data.local.entity.OfflineRegionEntity
import com.example.data.local.entity.RouteEntity
import com.example.data.local.entity.WaypointEntity
import com.example.data.model.MapLayerType
import com.example.data.tile.MapTileEngine
import kotlinx.coroutines.flow.Flow

class HikingRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val hikeTrackDao = db.hikeTrackDao()
    private val routeDao = db.routeDao()
    private val waypointDao = db.waypointDao()
    private val offlineRegionDao = db.offlineRegionDao()
    val tileEngine = MapTileEngine(context)

    // Tracks
    val allTracks: Flow<List<HikeTrackEntity>> = hikeTrackDao.getAllTracks()
    suspend fun getTrackById(id: Long): HikeTrackEntity? = hikeTrackDao.getTrackById(id)
    suspend fun saveTrack(track: HikeTrackEntity): Long = hikeTrackDao.insertTrack(track)
    suspend fun deleteTrack(id: Long) = hikeTrackDao.deleteTrackById(id)

    // Routes
    val allRoutes: Flow<List<RouteEntity>> = routeDao.getAllRoutes()
    suspend fun getRouteById(id: Long): RouteEntity? = routeDao.getRouteById(id)
    suspend fun saveRoute(route: RouteEntity, waypoints: List<WaypointEntity> = emptyList()): Long {
        val routeId = routeDao.insertRoute(route)
        if (waypoints.isNotEmpty()) {
            val assigned = waypoints.map { it.copy(routeId = routeId) }
            waypointDao.insertWaypoints(assigned)
        }
        return routeId
    }
    suspend fun deleteRoute(id: Long) {
        waypointDao.deleteWaypointsForRoute(id)
        routeDao.deleteRouteById(id)
    }
    suspend fun toggleFavorite(id: Long, isFav: Boolean) = routeDao.toggleFavorite(id, isFav)

    // Waypoints
    val allWaypoints: Flow<List<WaypointEntity>> = waypointDao.getAllWaypoints()
    fun getWaypointsForRoute(routeId: Long): Flow<List<WaypointEntity>> = waypointDao.getWaypointsForRoute(routeId)
    suspend fun getWaypointsForRouteSync(routeId: Long): List<WaypointEntity> = waypointDao.getWaypointsForRouteSync(routeId)
    suspend fun saveWaypoint(waypoint: WaypointEntity): Long = waypointDao.insertWaypoint(waypoint)
    suspend fun deleteWaypoint(id: Long) = waypointDao.deleteWaypointById(id)

    // Offline Regions
    val allOfflineRegions: Flow<List<OfflineRegionEntity>> = offlineRegionDao.getAllRegions()
    suspend fun saveOfflineRegion(region: OfflineRegionEntity): Long = offlineRegionDao.insertRegion(region)
    suspend fun deleteOfflineRegion(region: OfflineRegionEntity) {
        offlineRegionDao.deleteRegionById(region.id)
        // Also delete tile files
        val layer = try { MapLayerType.valueOf(region.layerType) } catch (_: Exception) { MapLayerType.OPEN_STREET_MAP }
        // Clean tile storage
    }
}
