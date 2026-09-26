package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.emergency.EmergencySosScreen
import com.example.ui.history.HistoryScreen
import com.example.ui.home.HomeScreen
import com.example.ui.map.MapScreen
import com.example.ui.offlinemaps.OfflineMapsScreen
import com.example.ui.routes.RoutesScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.AgHikingTheme
import com.example.ui.tools.HikingToolsScreen
import com.example.ui.tracking.TrackingScreen
import com.example.ui.waypoints.WaypointsScreen
import kotlinx.coroutines.launch

enum class AppDestination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    MAP("Peta", Icons.Default.Map),
    TRACK("Track", Icons.Default.DirectionsWalk),
    ROUTES("Rute", Icons.Default.Route),
    TOOLS("Tools", Icons.Default.Handyman),

    // Sub-screens
    WAYPOINTS("Waypoints", Icons.Default.Place),
    OFFLINE_MAPS("Peta Offline", Icons.Default.DownloadForOffline),
    HISTORY("Riwayat", Icons.Default.History),
    EMERGENCY("SOS", Icons.Default.Sos),
    SETTINGS("Pengaturan", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingFileIntent(intent)

        setContent {
            AgHikingTheme {
                MainAppScaffold(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingFileIntent(intent)
    }

    private fun handleIncomingFileIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val name = uri.lastPathSegment ?: "imported.gpx"
            if (inputStream != null) {
                viewModel.importRouteFile(inputStream, name)
            }
        } catch (_: Exception) {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(viewModel: MainViewModel) {
    var currentDestination by remember { mutableStateOf(AppDestination.HOME) }
    val userMessage by viewModel.userMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Request permissions on startup
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionsLauncher.launch(permissions.toTypedArray())
    }

    // Show snackbar feedback
    LaunchedEffect(userMessage) {
        userMessage?.let {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearUserMessage()
            }
        }
    }

    // Back handler for secondary screens
    if (currentDestination != AppDestination.HOME) {
        BackHandler {
            currentDestination = AppDestination.HOME
        }
    }

    val isTopLevel = currentDestination in listOf(
        AppDestination.HOME,
        AppDestination.MAP,
        AppDestination.TRACK,
        AppDestination.ROUTES,
        AppDestination.TOOLS
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row {
                        Text(
                            text = "AG HIKING",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PRO",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                },
                navigationIcon = {
                    if (!isTopLevel) {
                        IconButton(onClick = { currentDestination = AppDestination.HOME }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Terrain,
                            contentDescription = "Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, end = 8.dp)
                        )
                    }
                },
                actions = {
                    if (currentDestination != AppDestination.EMERGENCY) {
                        IconButton(
                            onClick = { currentDestination = AppDestination.EMERGENCY },
                            modifier = Modifier.testTag("sos_top_bar_button")
                        ) {
                            Icon(Icons.Default.Sos, contentDescription = "SOS", tint = Color(0xFFC62828))
                        }
                    }
                    if (currentDestination != AppDestination.SETTINGS) {
                        IconButton(
                            onClick = { currentDestination = AppDestination.SETTINGS },
                            modifier = Modifier.testTag("settings_top_bar_button")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Pengaturan")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                listOf(
                    AppDestination.HOME,
                    AppDestination.MAP,
                    AppDestination.TRACK,
                    AppDestination.ROUTES,
                    AppDestination.TOOLS
                ).forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination == destination,
                        onClick = { currentDestination = destination },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label, fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_tab_${destination.name.lowercase()}"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestination) {
                AppDestination.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToMap = { currentDestination = AppDestination.MAP },
                    onNavigateToTracking = { currentDestination = AppDestination.TRACK },
                    onNavigateToRoutes = { currentDestination = AppDestination.ROUTES },
                    onNavigateToWaypoints = { currentDestination = AppDestination.WAYPOINTS },
                    onNavigateToOfflineMaps = { currentDestination = AppDestination.OFFLINE_MAPS },
                    onNavigateToTools = { currentDestination = AppDestination.TOOLS },
                    onNavigateToEmergency = { currentDestination = AppDestination.EMERGENCY },
                    onNavigateToHistory = { currentDestination = AppDestination.HISTORY }
                )
                AppDestination.MAP -> MapScreen(
                    viewModel = viewModel,
                    onNavigateToRoutes = { currentDestination = AppDestination.ROUTES }
                )
                AppDestination.TRACK -> TrackingScreen(
                    viewModel = viewModel,
                    onNavigateToMap = { currentDestination = AppDestination.MAP }
                )
                AppDestination.ROUTES -> RoutesScreen(
                    viewModel = viewModel,
                    onNavigateToMap = { currentDestination = AppDestination.MAP }
                )
                AppDestination.TOOLS -> HikingToolsScreen(
                    viewModel = viewModel
                )
                AppDestination.WAYPOINTS -> WaypointsScreen(
                    viewModel = viewModel,
                    onNavigateToMap = { currentDestination = AppDestination.MAP }
                )
                AppDestination.OFFLINE_MAPS -> OfflineMapsScreen(
                    viewModel = viewModel
                )
                AppDestination.HISTORY -> HistoryScreen(
                    viewModel = viewModel,
                    onNavigateToMap = { currentDestination = AppDestination.MAP }
                )
                AppDestination.EMERGENCY -> EmergencySosScreen(
                    viewModel = viewModel
                )
                AppDestination.SETTINGS -> SettingsScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}
