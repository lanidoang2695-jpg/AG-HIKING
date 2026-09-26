package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.util.SampleRouteData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AgHikingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(this)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val routes = db.routeDao().getAllRoutes().first()
                if (routes.isEmpty()) {
                    val (sampleRoute, waypoints) = SampleRouteData.getSampleRoute()
                    val routeId = db.routeDao().insertRoute(sampleRoute)
                    val waypointsWithId = waypoints.map { it.copy(routeId = routeId) }
                    db.waypointDao().insertWaypoints(waypointsWithId)
                }
            } catch (_: Exception) {}
        }
    }
}
