package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ai.TransitAiAssistant
import com.example.data.local.MueveTDatabase
import com.example.data.location.GpsLocationClient
import com.example.data.remote.UserCloudRepository
import com.example.data.repository.TransitRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutGoogle
import com.example.ui.components.MueveTHeader
import com.example.ui.components.RouteDetailSheet
import com.example.ui.components.TripSearchSheet
import com.example.ui.screens.AssistantChatScreen
import com.example.ui.screens.ContributeScreen
import com.example.ui.screens.ExploraScreen
import com.example.ui.screens.SavedRoutesScreen
import com.example.ui.theme.CapacityHigh
import com.example.ui.theme.CapacityLow
import com.example.ui.theme.CreamBg
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.MueveTTheme
import com.example.ui.theme.SageBorder
import com.example.ui.theme.SageTint
import com.example.ui.theme.SoftDivider
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.Terracotta
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MueveTViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Must run before setContent, ViewModels, or any Firebase operation
        configureAppCheck(this, intent)

        val database = MueveTDatabase.getInstance(applicationContext)
        val repository = TransitRepository(database.mueveTDao())
        val firestoreDb = FirebaseFirestore.getInstance(
            applicationContext.getString(R.string.firestore_database_id)
        )
        val cloudRepository = UserCloudRepository(firestoreDb)
        val gpsClient = GpsLocationClient(applicationContext)
        val aiAssistant = TransitAiAssistant()

        setContent {
            MueveTTheme {
                MueveTAuthRoot(
                    repository = repository,
                    cloudRepository = cloudRepository,
                    gpsClient = gpsClient,
                    aiAssistant = aiAssistant,
                )
            }
        }
    }
}

@Composable
private fun MueveTAuthRoot(
    repository: TransitRepository,
    cloudRepository: UserCloudRepository,
    gpsClient: GpsLocationClient,
    aiAssistant: TransitAiAssistant,
) {
    val currentUser by Firebase.auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { /* authStateFlow emits signed-in user automatically */ })
    } else {
        key(user.uid) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val credentialManager = remember(context) { CredentialManager.create(context) }
            val vm: MueveTViewModel = viewModel(
                key = user.uid,
                factory = MueveTViewModel.Factory(
                    repository = repository,
                    cloudRepository = cloudRepository,
                    gpsClient = gpsClient,
                    aiAssistant = aiAssistant,
                    currentUserId = user.uid,
                ),
            )
            MueveTApp(
                viewModel = vm,
                repository = repository,
                currentUser = user,
                onSignOut = {
                    signOutGoogle(
                        credentialManager = credentialManager,
                        scope = scope,
                        onBeforeSignOut = { vm.cancelCloudListeners() },
                    )
                },
            )
        }
    }
}

@Composable
fun MueveTApp(
    viewModel: MueveTViewModel,
    repository: TransitRepository,
    currentUser: FirebaseUser,
    onSignOut: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAccountDialog by remember { mutableStateOf(false) }

    val displayName = uiState.userProfile?.displayName?.takeIf { it.isNotBlank() }
        ?: currentUser.displayName?.takeIf { it.isNotBlank() }
        ?: currentUser.email?.substringBefore("@")
        ?: "Usuario MueveT"
    val userEmail = uiState.userProfile?.email?.takeIf { it.isNotBlank() }
        ?: currentUser.email
        ?: ""
    val userInitials = displayName
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifBlank { "M" }

    BackHandler(enabled = uiState.selectedTab != AppTab.EXPLORA) {
        viewModel.selectTab(AppTab.EXPLORA)
    }

    var hasAutoRequestedLocation by rememberSaveable { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onLocationPermissionResult(granted)
        if (granted && uiState.selectedTab == AppTab.CONTRIBUIR) {
            viewModel.toggleGpsRouteRecording()
        }
    }

    LaunchedEffect(Unit) {
        if (!uiState.hasLocationPermission && !hasAutoRequestedLocation) {
            hasAutoRequestedLocation = true
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        } else if (uiState.hasLocationPermission) {
            viewModel.startLiveLocationUpdates()
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissSnackbar()
        }
    }

    val requestOrRefreshGps: () -> Unit = {
        if (uiState.hasLocationPermission) {
            viewModel.refreshGpsFix()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        }
    }

    val toggleContributeGps: () -> Unit = {
        if (uiState.hasLocationPermission) {
            viewModel.toggleGpsRouteRecording()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBg)
    ) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = CreamBg,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                MueveTHeader(
                    hasGpsFix = uiState.hasLocationPermission && uiState.currentGpsLocation != null,
                    placeName = uiState.currentPlaceName,
                    userInitials = userInitials,
                    onAccountClick = { showAccountDialog = true },
                    modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    MueveTBottomBar(
                        selectedTab = uiState.selectedTab,
                        onSelectTab = viewModel::selectTab,
                    )
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    MueveTNavRail(
                        selectedTab = uiState.selectedTab,
                        onSelectTab = viewModel::selectTab,
                    )
                }

                when (uiState.selectedTab) {
                    AppTab.EXPLORA -> ExploraScreen(
                        uiState = uiState,
                        onSelectRouteOnMap = viewModel::selectRouteOnMap,
                        onOpenRouteDetail = viewModel::openRouteDetail,
                        onToggleSaveRoute = viewModel::toggleSavedRoute,
                        onSelectFilter = viewModel::selectFilter,
                        onOpenTripSearch = viewModel::openTripSearch,
                        onClearTripSearch = viewModel::clearTripSearch,
                        onRequestLocationOrRecenter = requestOrRefreshGps,
                        onReportClick = { viewModel.openContributeForRoute() },
                        modifier = Modifier.weight(1f),
                    )

                    AppTab.GUARDADAS -> SavedRoutesScreen(
                        savedRoutes = uiState.savedRoutes,
                        onOpenRouteDetail = viewModel::openRouteDetail,
                        onToggleSaveRoute = viewModel::toggleSavedRoute,
                        onExploreClick = { viewModel.selectTab(AppTab.EXPLORA) },
                        modifier = Modifier.weight(1f),
                    )

                    AppTab.CONTRIBUIR -> ContributeScreen(
                        uiState = uiState,
                        onSelectRoute = viewModel::selectContributeRoute,
                        onToggleNewRouteMode = viewModel::setContributeRouteMode,
                        onUpdateNewRouteCode = viewModel::updateNewRouteCode,
                        onUpdateNewRouteOrigin = viewModel::updateNewRouteOrigin,
                        onUpdateNewRouteDestination = viewModel::updateNewRouteDestination,
                        onUpdateNewRouteType = viewModel::updateNewRouteType,
                        onSelectCapacity = viewModel::selectContributeCapacity,
                        onUpdateFare = viewModel::updateFareText,
                        onAddStop = viewModel::addStopToContribution,
                        onRemoveStop = viewModel::removeStopFromContribution,
                        onToggleGpsTracking = toggleContributeGps,
                        onCaptureGpsWaypoint = {
                            if (uiState.hasLocationPermission) {
                                viewModel.captureCurrentGpsWaypoint()
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    )
                                )
                            }
                        },
                        onSubmitReport = viewModel::submitContribution,
                        modifier = Modifier.weight(1f),
                    )

                    AppTab.ASISTENTE -> AssistantChatScreen(
                        messages = uiState.chatMessages,
                        selectedModel = uiState.selectedAiModel,
                        isSending = uiState.isChatSending,
                        errorMessage = uiState.chatErrorMessage,
                        onSelectModel = viewModel::selectAiModel,
                        onSendMessage = viewModel::sendChatMessage,
                        onClearChat = viewModel::clearChat,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    if (uiState.isTripSearchOpen) {
        TripSearchSheet(
            initialOrigin = uiState.originQuery,
            initialDestination = uiState.destinationQuery,
            initialIsLiveOrigin = uiState.isUsingLiveOrigin,
            liveAddress = uiState.currentDetailedAddress ?: uiState.currentPlaceName,
            liveCoordinates = uiState.currentGpsLocation,
            hasLocationPermission = uiState.hasLocationPermission,
            onRefreshLocation = requestOrRefreshGps,
            onDismiss = viewModel::closeTripSearch,
            onApplySearch = viewModel::applyTripSearch,
            onClearSearch = viewModel::clearTripSearch,
        )
    }

    val detailRoute = uiState.detailSheetRouteId?.let { id ->
        uiState.allRoutes.firstOrNull { it.id == id }
    }
    if (detailRoute != null) {
        val routeReports by repository.observeReportsForRoute(detailRoute.id)
            .collectAsState(initial = emptyList())
        RouteDetailSheet(
            route = detailRoute,
            reports = routeReports,
            onDismiss = viewModel::closeRouteDetail,
            onToggleSave = { viewModel.toggleSavedRoute(detailRoute) },
            onMarkTraveled = { viewModel.markRouteAsTraveled(detailRoute) },
            onReportRoute = { viewModel.openContributeForRoute(detailRoute.id) },
        )
    }

    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            containerColor = SurfaceWhite,
            title = {
                Text(
                    text = "Cuenta MueveT",
                    style = MaterialTheme.typography.titleLarge,
                    color = InkPrimary,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = InkPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (userEmail.isNotBlank()) {
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSecondary,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SageTint,
                        border = BorderStroke(1.dp, SageBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = CapacityLow,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cuenta registrada en servidores MueveT",
                                style = MaterialTheme.typography.bodySmall,
                                color = ForestGreen,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAccountDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = CapacityHigh),
                    modifier = Modifier.testTag("sign_out_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cerrar sesión", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAccountDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = InkSecondary),
                    modifier = Modifier.testTag("close_account_dialog_button"),
                ) {
                    Text("Cerrar")
                }
            },
        )
    }
}

@Composable
private fun MueveTBottomBar(
    selectedTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
) {
    Column(modifier = Modifier.background(SurfaceWhite)) {
        HorizontalDivider(color = SoftDivider, thickness = 1.dp)
        NavigationBar(
            containerColor = SurfaceWhite,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets.navigationBars,
        ) {
            NavigationBarItem(
                selected = selectedTab == AppTab.EXPLORA,
                onClick = { onSelectTab(AppTab.EXPLORA) },
                icon = {
                    Icon(
                        imageVector = if (selectedTab == AppTab.EXPLORA) Icons.Filled.Map else Icons.Outlined.Map,
                        contentDescription = null,
                    )
                },
                label = {
                    Text(
                        text = stringResource(R.string.tab_explora),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selectedTab == AppTab.EXPLORA) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Terracotta,
                    selectedTextColor = Terracotta,
                    unselectedIconColor = InkSecondary,
                    unselectedTextColor = InkSecondary,
                    indicatorColor = Color.Transparent,
                ),
                modifier = Modifier.testTag("nav_explora"),
            )

            NavigationBarItem(
                selected = selectedTab == AppTab.GUARDADAS,
                onClick = { onSelectTab(AppTab.GUARDADAS) },
                icon = {
                    Icon(
                        imageVector = if (selectedTab == AppTab.GUARDADAS) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                    )
                },
                label = {
                    Text(
                        text = stringResource(R.string.tab_saved),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selectedTab == AppTab.GUARDADAS) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Terracotta,
                    selectedTextColor = Terracotta,
                    unselectedIconColor = InkSecondary,
                    unselectedTextColor = InkSecondary,
                    indicatorColor = Color.Transparent,
                ),
                modifier = Modifier.testTag("nav_guardadas"),
            )

            NavigationBarItem(
                selected = selectedTab == AppTab.CONTRIBUIR,
                onClick = { onSelectTab(AppTab.CONTRIBUIR) },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                    )
                },
                label = {
                    Text(
                        text = stringResource(R.string.tab_contribute),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selectedTab == AppTab.CONTRIBUIR) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Terracotta,
                    selectedTextColor = Terracotta,
                    unselectedIconColor = InkSecondary,
                    unselectedTextColor = InkSecondary,
                    indicatorColor = Color.Transparent,
                ),
                modifier = Modifier.testTag("nav_contribuir"),
            )

            NavigationBarItem(
                selected = selectedTab == AppTab.ASISTENTE,
                onClick = { onSelectTab(AppTab.ASISTENTE) },
                icon = {
                    Icon(
                        imageVector = if (selectedTab == AppTab.ASISTENTE) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                    )
                },
                label = {
                    Text(
                        text = stringResource(R.string.tab_assistant),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selectedTab == AppTab.ASISTENTE) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Terracotta,
                    selectedTextColor = Terracotta,
                    unselectedIconColor = InkSecondary,
                    unselectedTextColor = InkSecondary,
                    indicatorColor = Color.Transparent,
                ),
                modifier = Modifier.testTag("nav_asistente"),
            )
        }
    }
}

@Composable
private fun MueveTNavRail(
    selectedTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
) {
    NavigationRail(
        containerColor = SurfaceWhite,
        modifier = Modifier
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        NavigationRailItem(
            selected = selectedTab == AppTab.EXPLORA,
            onClick = { onSelectTab(AppTab.EXPLORA) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == AppTab.EXPLORA) Icons.Filled.Map else Icons.Outlined.Map,
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.tab_explora)) },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = Terracotta,
                selectedTextColor = Terracotta,
                unselectedIconColor = InkSecondary,
                unselectedTextColor = InkSecondary,
                indicatorColor = Color.Transparent,
            ),
        )
        NavigationRailItem(
            selected = selectedTab == AppTab.GUARDADAS,
            onClick = { onSelectTab(AppTab.GUARDADAS) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == AppTab.GUARDADAS) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.tab_saved)) },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = Terracotta,
                selectedTextColor = Terracotta,
                unselectedIconColor = InkSecondary,
                unselectedTextColor = InkSecondary,
                indicatorColor = Color.Transparent,
            ),
        )
        NavigationRailItem(
            selected = selectedTab == AppTab.CONTRIBUIR,
            onClick = { onSelectTab(AppTab.CONTRIBUIR) },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            label = { Text(stringResource(R.string.tab_contribute)) },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = Terracotta,
                selectedTextColor = Terracotta,
                unselectedIconColor = InkSecondary,
                unselectedTextColor = InkSecondary,
                indicatorColor = Color.Transparent,
            ),
        )
        NavigationRailItem(
            selected = selectedTab == AppTab.ASISTENTE,
            onClick = { onSelectTab(AppTab.ASISTENTE) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == AppTab.ASISTENTE) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.tab_assistant)) },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = Terracotta,
                selectedTextColor = Terracotta,
                unselectedIconColor = InkSecondary,
                unselectedTextColor = InkSecondary,
                indicatorColor = Color.Transparent,
            ),
        )
    }
}
