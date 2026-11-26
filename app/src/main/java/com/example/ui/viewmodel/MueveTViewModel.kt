package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiModelTier
import com.example.data.ai.ChatMessage
import com.example.data.ai.StreamChatEvent
import com.example.data.ai.TransitAiAssistant
import com.example.data.location.GpsLocationClient
import com.example.data.model.CapacityLevel
import com.example.data.model.GpsPoint
import com.example.data.model.RouteFilter
import com.example.data.model.RouteReport
import com.example.data.model.TransitRoute
import com.example.data.model.TransportType
import com.example.data.remote.OperationType
import com.example.data.remote.UserCloudRepository
import com.example.data.remote.UserProfileRecord
import com.example.data.remote.handleFirestoreError
import com.example.data.repository.TransitRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs

enum class AppTab {
    EXPLORA,
    GUARDADAS,
    CONTRIBUIR,
    ASISTENTE,
}

data class ContributeFormState(
    val isNewRouteMode: Boolean = false,
    val selectedRouteId: String = "ruta_76",
    val newRouteCode: String = "",
    val newRouteOrigin: String = "",
    val newRouteDestination: String = "",
    val newRouteType: TransportType = TransportType.CAMION,
    val capacity: CapacityLevel = CapacityLevel.HAY_LUGAR,
    val fareText: String = "10",
    val addedStops: List<String> = emptyList(),
    val isSubmitting: Boolean = false,
)

data class MueveTUiState(
    val selectedTab: AppTab = AppTab.EXPLORA,
    val allRoutes: List<TransitRoute> = emptyList(),
    val filteredRoutes: List<TransitRoute> = emptyList(),
    val recentRoutes: List<TransitRoute> = emptyList(),
    val recommendedRoutes: List<TransitRoute> = emptyList(),
    val savedRoutes: List<TransitRoute> = emptyList(),
    val recentReports: List<RouteReport> = emptyList(),
    val originQuery: String = "",
    val destinationQuery: String = "",
    val selectedFilter: RouteFilter = RouteFilter.FASTEST,
    val selectedRouteId: String = "ruta_76",
    val detailSheetRouteId: String? = null,
    val isTripSearchOpen: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val currentGpsLocation: GpsPoint? = null,
    val currentPlaceName: String? = null,
    val currentDetailedAddress: String? = null,
    val isUsingLiveOrigin: Boolean = true,
    val isRecordingGpsPath: Boolean = false,
    val recordedGpsTrail: List<GpsPoint> = emptyList(),
    val contributeForm: ContributeFormState = ContributeFormState(),
    val snackbarMessage: String? = null,
    val chatMessages: List<ChatMessage> = emptyList(),
    val selectedAiModel: AiModelTier = AiModelTier.GENERAL,
    val isChatSending: Boolean = false,
    val chatErrorMessage: String? = null,
    val userProfile: UserProfileRecord? = null,
)

private data class LocalUiControls(
    val selectedTab: AppTab = AppTab.EXPLORA,
    val originQuery: String = "",
    val destinationQuery: String = "",
    val selectedFilter: RouteFilter = RouteFilter.FASTEST,
    val selectedRouteId: String = "ruta_76",
    val detailSheetRouteId: String? = null,
    val isTripSearchOpen: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val currentGpsLocation: GpsPoint? = null,
    val currentPlaceName: String? = null,
    val currentDetailedAddress: String? = null,
    val isUsingLiveOrigin: Boolean = true,
    val isRecordingGpsPath: Boolean = false,
    val recordedGpsTrail: List<GpsPoint> = emptyList(),
    val contributeForm: ContributeFormState = ContributeFormState(),
    val snackbarMessage: String? = null,
    val chatMessages: List<ChatMessage> = emptyList(),
    val selectedAiModel: AiModelTier = AiModelTier.GENERAL,
    val isChatSending: Boolean = false,
    val chatErrorMessage: String? = null,
    val userProfile: UserProfileRecord? = null,
)

class MueveTViewModel(
    private val repository: TransitRepository,
    private val cloudRepository: UserCloudRepository,
    private val gpsClient: GpsLocationClient,
    private val aiAssistant: TransitAiAssistant,
    private val currentUserId: String,
) : ViewModel() {

    private var chatJob: Job? = null

    private val controls = MutableStateFlow(
        LocalUiControls(hasLocationPermission = gpsClient.hasLocationPermission())
    )
    private var liveLocationJob: Job? = null
    private var geocodeJob: Job? = null
    private var cloudProfileJob: Job? = null
    private var cloudSavedJob: Job? = null
    private var lastGeocodedPoint: GpsPoint? = null

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            val registerResult = cloudRepository.registerOrUpdateCurrentUser(
                city = controls.value.currentPlaceName ?: "Toluca"
            )
            registerResult.getOrNull()?.let { profile ->
                controls.update { it.copy(userProfile = profile) }
            }
            if (gpsClient.hasLocationPermission()) {
                startLiveLocationUpdates()
            }
        }
        startCloudListeners()
    }

    private fun startCloudListeners() {
        cloudProfileJob?.cancel()
        cloudProfileJob = viewModelScope.launch {
            cloudRepository.observeUserProfile(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (firestoreError != null) {
                        handleFirestoreError(firestoreError, OperationType.GET, "users/$currentUserId")
                    } else {
                        Log.e("MueveTCloud", "Error observing user profile", error)
                    }
                }
                .collect { profile ->
                    if (profile != null) {
                        controls.update { it.copy(userProfile = profile) }
                    }
                }
        }

        cloudSavedJob?.cancel()
        cloudSavedJob = viewModelScope.launch {
            cloudRepository.observeSavedRouteIds(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (firestoreError != null) {
                        handleFirestoreError(firestoreError, OperationType.LIST, "users/$currentUserId/saved_routes")
                    } else {
                        Log.e("MueveTCloud", "Error observing saved routes", error)
                    }
                }
                .collect { savedIds ->
                    repository.syncSavedIdsFromCloud(savedIds)
                }
        }
    }

    fun cancelCloudListeners() {
        cloudProfileJob?.cancel()
        cloudProfileJob = null
        cloudSavedJob?.cancel()
        cloudSavedJob = null
        liveLocationJob?.cancel()
        liveLocationJob = null
        geocodeJob?.cancel()
        geocodeJob = null
    }

    val uiState: StateFlow<MueveTUiState> = combine(
        repository.allRoutes,
        repository.savedRoutes,
        repository.recentReports,
        controls,
    ) { routes, saved, reports, ctrl ->
        val hasQuery = ctrl.originQuery.isNotBlank() || ctrl.destinationQuery.isNotBlank()
        val matching = if (hasQuery) {
            routes.filter { route ->
                val oMatch = ctrl.originQuery.isBlank() ||
                    ctrl.isUsingLiveOrigin ||
                    ctrl.originQuery.equals("Mi ubicación actual", ignoreCase = true) ||
                    (ctrl.currentDetailedAddress != null && ctrl.originQuery.equals(ctrl.currentDetailedAddress, ignoreCase = true)) ||
                    (ctrl.currentPlaceName != null && ctrl.originQuery.equals(ctrl.currentPlaceName, ignoreCase = true)) ||
                    route.matchesKeyword(ctrl.originQuery)
                val dMatch = ctrl.destinationQuery.isBlank() || route.matchesKeyword(ctrl.destinationQuery)
                oMatch && dMatch
            }
        } else {
            routes
        }

        val sorted = when (ctrl.selectedFilter) {
            RouteFilter.FASTEST -> matching.sortedBy { it.durationMinutes }
            RouteFilter.CHEAPEST -> matching.sortedBy { it.priceMxn }
            RouteFilter.LEAST_CROWDED -> matching.sortedWith(
                compareBy<TransitRoute> { it.capacity.activeBars }.thenBy { it.durationMinutes }
            )
        }

        val recent = routes
            .filter { it.lastTraveledAt != null }
            .sortedByDescending { it.lastTraveledAt }

        val preferredDestinations = recent.map { it.destination.lowercase() }.toSet()
        val recommended = routes
            .filter { route ->
                recent.none { it.id == route.id } &&
                    (preferredDestinations.contains(route.destination.lowercase()) ||
                        route.capacity == CapacityLevel.HAY_LUGAR)
            }
            .sortedBy { it.durationMinutes }
            .take(3)

        val validSelectedId = if (routes.any { it.id == ctrl.selectedRouteId }) {
            ctrl.selectedRouteId
        } else {
            routes.firstOrNull()?.id ?: "ruta_76"
        }

        val validContributeRouteId = if (routes.any { it.id == ctrl.contributeForm.selectedRouteId }) {
            ctrl.contributeForm.selectedRouteId
        } else {
            routes.firstOrNull()?.id ?: "ruta_76"
        }

        MueveTUiState(
            selectedTab = ctrl.selectedTab,
            allRoutes = routes,
            filteredRoutes = sorted,
            recentRoutes = recent,
            recommendedRoutes = recommended,
            savedRoutes = saved,
            recentReports = reports,
            originQuery = ctrl.originQuery,
            destinationQuery = ctrl.destinationQuery,
            selectedFilter = ctrl.selectedFilter,
            selectedRouteId = validSelectedId,
            detailSheetRouteId = ctrl.detailSheetRouteId,
            isTripSearchOpen = ctrl.isTripSearchOpen,
            hasLocationPermission = ctrl.hasLocationPermission,
            currentGpsLocation = ctrl.currentGpsLocation,
            currentPlaceName = ctrl.currentPlaceName,
            currentDetailedAddress = ctrl.currentDetailedAddress,
            isUsingLiveOrigin = ctrl.isUsingLiveOrigin,
            isRecordingGpsPath = ctrl.isRecordingGpsPath,
            recordedGpsTrail = ctrl.recordedGpsTrail,
            contributeForm = ctrl.contributeForm.copy(selectedRouteId = validContributeRouteId),
            snackbarMessage = ctrl.snackbarMessage,
            chatMessages = ctrl.chatMessages,
            selectedAiModel = ctrl.selectedAiModel,
            isChatSending = ctrl.isChatSending,
            chatErrorMessage = ctrl.chatErrorMessage,
            userProfile = ctrl.userProfile,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MueveTUiState(),
    )

    private fun TransitRoute.matchesKeyword(query: String): Boolean {
        val q = query.trim().lowercase()
        return title.lowercase().contains(q) ||
            origin.lowercase().contains(q) ||
            destination.lowercase().contains(q) ||
            code.lowercase().contains(q) ||
            stops.any { it.name.lowercase().contains(q) }
    }

    fun selectTab(tab: AppTab) {
        controls.update { it.copy(selectedTab = tab) }
    }

    fun openContributeForRoute(routeId: String? = null) {
        controls.update { ctrl ->
            val targetId = routeId ?: ctrl.selectedRouteId
            val routePrice = uiState.value.allRoutes.firstOrNull { it.id == targetId }?.priceMxn
            ctrl.copy(
                selectedTab = AppTab.CONTRIBUIR,
                detailSheetRouteId = null,
                contributeForm = ctrl.contributeForm.copy(
                    isNewRouteMode = false,
                    selectedRouteId = targetId,
                    fareText = routePrice?.toInt()?.toString() ?: ctrl.contributeForm.fareText,
                ),
            )
        }
    }

    fun selectFilter(filter: RouteFilter) {
        controls.update { it.copy(selectedFilter = filter) }
    }

    fun selectRouteOnMap(routeId: String) {
        controls.update { it.copy(selectedRouteId = routeId) }
    }

    fun openRouteDetail(routeId: String) {
        controls.update { it.copy(selectedRouteId = routeId, detailSheetRouteId = routeId) }
    }

    fun closeRouteDetail() {
        controls.update { it.copy(detailSheetRouteId = null) }
    }

    fun openTripSearch() {
        controls.update { it.copy(isTripSearchOpen = true) }
        if (gpsClient.hasLocationPermission()) {
            refreshGpsFix()
        }
    }

    fun closeTripSearch() {
        controls.update { it.copy(isTripSearchOpen = false) }
    }

    fun applyTripSearch(origin: String, destination: String, useLiveOrigin: Boolean = false) {
        val cleanOrigin = origin.trim()
        val liveMatch = useLiveOrigin ||
            cleanOrigin.isBlank() ||
            cleanOrigin.equals("Mi ubicación actual", ignoreCase = true) ||
            (controls.value.currentDetailedAddress != null && cleanOrigin.equals(controls.value.currentDetailedAddress, ignoreCase = true))
        controls.update {
            it.copy(
                originQuery = cleanOrigin,
                destinationQuery = destination.trim(),
                isUsingLiveOrigin = liveMatch,
                isTripSearchOpen = false,
            )
        }
    }

    fun clearTripSearch() {
        controls.update {
            it.copy(
                originQuery = "",
                destinationQuery = "",
                isUsingLiveOrigin = true,
            )
        }
    }

    fun toggleSavedRoute(route: TransitRoute) {
        viewModelScope.launch {
            val willSave = !route.isSaved
            repository.toggleSavedRoute(route.id, route.isSaved)
            val cloudResult = cloudRepository.syncSavedRoute(currentUserId, route, save = willSave)
            val msg = if (cloudResult.isFailure) {
                if (willSave) "Ruta ${route.code} guardada localmente"
                else "Ruta ${route.code} quitada localmente"
            } else if (willSave) {
                "Ruta ${route.code} guardada y sincronizada en tu cuenta"
            } else {
                "Ruta ${route.code} quitada de guardadas"
            }
            controls.update { it.copy(snackbarMessage = msg) }
        }
    }

    fun markRouteAsTraveled(route: TransitRoute) {
        viewModelScope.launch {
            repository.recordRouteTraveled(route.id)
            controls.update {
                it.copy(
                    detailSheetRouteId = null,
                    snackbarMessage = "Viaje registrado en Ruta ${route.code}",
                )
            }
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        controls.update { it.copy(hasLocationPermission = granted) }
        if (granted) {
            startLiveLocationUpdates()
        } else {
            liveLocationJob?.cancel()
            liveLocationJob = null
            controls.update {
                it.copy(snackbarMessage = "Permiso de ubicación denegado. Puedes usar el mapa manualmente.")
            }
        }
    }

    fun startLiveLocationUpdates() {
        if (!gpsClient.hasLocationPermission()) return
        if (liveLocationJob?.isActive == true) return
        liveLocationJob = viewModelScope.launch {
            val initialFix = gpsClient.getLastKnownOrCurrentLocation()
            if (initialFix != null) {
                applyLiveGpsPoint(initialFix)
            }
            gpsClient.observeLocationUpdates(intervalMillis = 2500L).collect { point ->
                applyLiveGpsPoint(point)
            }
        }
    }

    private fun applyLiveGpsPoint(point: GpsPoint) {
        val coordFallback = String.format(java.util.Locale.US, "GPS: %.4f, %.4f", point.lat, point.lng)
        controls.update { ctrl ->
            val updatedDetailed = ctrl.currentDetailedAddress ?: coordFallback
            ctrl.copy(
                hasLocationPermission = true,
                currentGpsLocation = point,
                currentDetailedAddress = updatedDetailed,
                originQuery = if (ctrl.isUsingLiveOrigin && ctrl.originQuery.isNotBlank()) updatedDetailed else ctrl.originQuery,
                recordedGpsTrail = if (ctrl.isRecordingGpsPath) {
                    (ctrl.recordedGpsTrail + point).takeLast(50)
                } else {
                    ctrl.recordedGpsTrail
                },
            )
        }
        val prev = lastGeocodedPoint
        val shouldResolve = controls.value.currentPlaceName == null ||
            controls.value.currentDetailedAddress == null ||
            prev == null ||
            abs(point.lat - prev.lat) > 0.0005 ||
            abs(point.lng - prev.lng) > 0.0005
        if (shouldResolve) {
            lastGeocodedPoint = point
            geocodeJob?.cancel()
            geocodeJob = viewModelScope.launch {
                val resolved = gpsClient.resolvePlaceDetails(point.lat, point.lng)
                if (resolved != null) {
                    controls.update { ctrl ->
                        ctrl.copy(
                            currentPlaceName = resolved.shortLabel,
                            currentDetailedAddress = resolved.detailedAddress,
                            originQuery = if (ctrl.isUsingLiveOrigin && ctrl.originQuery.isNotBlank()) {
                                resolved.detailedAddress
                            } else {
                                ctrl.originQuery
                            },
                        )
                    }
                }
            }
        }
    }

    fun refreshGpsFix() {
        startLiveLocationUpdates()
        viewModelScope.launch {
            val point = gpsClient.getLastKnownOrCurrentLocation()
            if (point != null) {
                applyLiveGpsPoint(point)
            } else {
                controls.update { ctrl ->
                    ctrl.copy(hasLocationPermission = gpsClient.hasLocationPermission())
                }
            }
        }
    }

    fun toggleGpsRouteRecording() {
        val currentlyRecording = controls.value.isRecordingGpsPath
        if (currentlyRecording) {
            controls.update { it.copy(isRecordingGpsPath = false) }
            return
        }

        if (!gpsClient.hasLocationPermission()) {
            controls.update {
                it.copy(snackbarMessage = "Activa el permiso de ubicación para trazar la ruta con GPS.")
            }
            return
        }

        controls.update { ctrl ->
            val currentFix = ctrl.currentGpsLocation
            val updatedTrail = if (currentFix != null && ctrl.recordedGpsTrail.lastOrNull() != currentFix) {
                (ctrl.recordedGpsTrail + currentFix).takeLast(50)
            } else {
                ctrl.recordedGpsTrail
            }
            ctrl.copy(
                isRecordingGpsPath = true,
                recordedGpsTrail = updatedTrail,
            )
        }
        startLiveLocationUpdates()
    }

    fun captureCurrentGpsWaypoint() {
        viewModelScope.launch {
            val fix = gpsClient.getLastKnownOrCurrentLocation() ?: controls.value.currentGpsLocation
            if (fix != null) {
                controls.update { ctrl ->
                    ctrl.copy(
                        currentGpsLocation = fix,
                        recordedGpsTrail = ctrl.recordedGpsTrail + fix,
                        snackbarMessage = "Punto GPS capturado (${String.format("%.4f, %.4f", fix.lat, fix.lng)})",
                    )
                }
            } else {
                // Fallback synthetic waypoint in Toluca Centro when device GPS has no fix yet
                val fallback = GpsPoint(19.2826, -99.6557)
                controls.update { ctrl ->
                    ctrl.copy(
                        recordedGpsTrail = ctrl.recordedGpsTrail + fallback,
                        snackbarMessage = "Punto registrado en Toluca Centro (esperando señal GPS)",
                    )
                }
            }
        }
    }

    fun setContributeRouteMode(isNewRoute: Boolean) {
        controls.update { ctrl ->
            ctrl.copy(contributeForm = ctrl.contributeForm.copy(isNewRouteMode = isNewRoute))
        }
    }

    fun selectContributeRoute(route: TransitRoute) {
        controls.update { ctrl ->
            ctrl.copy(
                contributeForm = ctrl.contributeForm.copy(
                    isNewRouteMode = false,
                    selectedRouteId = route.id,
                    fareText = if (route.priceMxn % 1.0 == 0.0) {
                        route.priceMxn.toInt().toString()
                    } else {
                        route.priceMxn.toString()
                    },
                )
            )
        }
    }

    fun updateNewRouteCode(value: String) {
        controls.update { it.copy(contributeForm = it.contributeForm.copy(newRouteCode = value)) }
    }

    fun updateNewRouteOrigin(value: String) {
        controls.update { it.copy(contributeForm = it.contributeForm.copy(newRouteOrigin = value)) }
    }

    fun updateNewRouteDestination(value: String) {
        controls.update { it.copy(contributeForm = it.contributeForm.copy(newRouteDestination = value)) }
    }

    fun updateNewRouteType(type: TransportType) {
        controls.update { it.copy(contributeForm = it.contributeForm.copy(newRouteType = type)) }
    }

    fun selectContributeCapacity(capacity: CapacityLevel) {
        controls.update { it.copy(contributeForm = it.contributeForm.copy(capacity = capacity)) }
    }

    fun updateFareText(fare: String) {
        val sanitized = fare.filter { it.isDigit() || it == '.' }
        controls.update { it.copy(contributeForm = it.contributeForm.copy(fareText = sanitized)) }
    }

    fun addStopToContribution(stopName: String) {
        val clean = stopName.trim()
        if (clean.isBlank()) return
        controls.update { ctrl ->
            if (ctrl.contributeForm.addedStops.any { it.equals(clean, ignoreCase = true) }) {
                ctrl
            } else {
                ctrl.copy(
                    contributeForm = ctrl.contributeForm.copy(
                        addedStops = ctrl.contributeForm.addedStops + clean
                    )
                )
            }
        }
    }

    fun removeStopFromContribution(stopName: String) {
        controls.update { ctrl ->
            ctrl.copy(
                contributeForm = ctrl.contributeForm.copy(
                    addedStops = ctrl.contributeForm.addedStops - stopName
                )
            )
        }
    }

    fun submitContribution() {
        val state = controls.value
        val form = state.contributeForm
        val fare = form.fareText.toDoubleOrNull()?.coerceIn(1.0, 250.0) ?: 10.0

        viewModelScope.launch {
            controls.update { it.copy(contributeForm = it.contributeForm.copy(isSubmitting = true)) }
            val fix = state.currentGpsLocation
            if (form.isNewRouteMode) {
                val (created, reportId) = repository.createMappedRouteAndReport(
                    routeCode = form.newRouteCode,
                    origin = form.newRouteOrigin,
                    destination = form.newRouteDestination,
                    transportType = form.newRouteType,
                    capacity = form.capacity,
                    priceMxn = fare,
                    stopNames = form.addedStops,
                    gpsTrail = state.recordedGpsTrail,
                    currentLat = fix?.lat,
                    currentLng = fix?.lng,
                )
                cloudRepository.syncSavedRoute(currentUserId, created, save = true)
                cloudRepository.recordCloudReport(
                    userId = currentUserId,
                    reportId = reportId,
                    routeId = created.id,
                    routeCode = created.code,
                    routeTitle = created.title,
                    capacity = form.capacity,
                    priceMxn = fare,
                    addedStops = form.addedStops,
                    gpsPointsCount = state.recordedGpsTrail.size,
                    latitude = fix?.lat,
                    longitude = fix?.lng,
                )
                controls.update {
                    it.copy(
                        selectedRouteId = created.id,
                        isRecordingGpsPath = false,
                        recordedGpsTrail = emptyList(),
                        contributeForm = ContributeFormState(selectedRouteId = created.id),
                        snackbarMessage = "¡Nueva ruta ${created.code} mapeada y sincronizada!",
                    )
                }
            } else {
                val reportId = repository.submitReportForExistingRoute(
                    routeId = form.selectedRouteId,
                    capacity = form.capacity,
                    priceMxn = fare,
                    addedStopNames = form.addedStops,
                    gpsTrail = state.recordedGpsTrail,
                    currentLat = fix?.lat,
                    currentLng = fix?.lng,
                )
                if (reportId != null) {
                    val targetRoute = uiState.value.allRoutes.firstOrNull { it.id == form.selectedRouteId }
                    cloudRepository.recordCloudReport(
                        userId = currentUserId,
                        reportId = reportId,
                        routeId = form.selectedRouteId,
                        routeCode = targetRoute?.code ?: form.selectedRouteId,
                        routeTitle = targetRoute?.title ?: "Ruta",
                        capacity = form.capacity,
                        priceMxn = fare,
                        addedStops = form.addedStops,
                        gpsPointsCount = state.recordedGpsTrail.size,
                        latitude = fix?.lat,
                        longitude = fix?.lng,
                    )
                }
                controls.update {
                    it.copy(
                        isRecordingGpsPath = false,
                        recordedGpsTrail = emptyList(),
                        contributeForm = form.copy(
                            addedStops = emptyList(),
                            isSubmitting = false,
                        ),
                        snackbarMessage = "¡Reporte enviado y guardado en tu cuenta!",
                    )
                }
            }
        }
    }

    fun selectAiModel(tier: AiModelTier) {
        controls.update { it.copy(selectedAiModel = tier) }
    }

    fun clearChat() {
        chatJob?.cancel()
        controls.update {
            it.copy(
                chatMessages = emptyList(),
                isChatSending = false,
                chatErrorMessage = null,
            )
        }
    }

    fun sendChatMessage(prompt: String) {
        val clean = prompt.trim()
        if (clean.isBlank() || controls.value.isChatSending) return

        val priorHistory = controls.value.chatMessages
        val selectedTier = controls.value.selectedAiModel
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = clean,
        )
        val assistantMsgId = UUID.randomUUID().toString()
        val placeholderMsg = ChatMessage(
            id = assistantMsgId,
            isUser = false,
            text = "",
            isStreaming = true,
            modelLabel = "MueveT · ${selectedTier.labelEs}",
        )

        controls.update {
            it.copy(
                chatMessages = priorHistory + userMsg + placeholderMsg,
                isChatSending = true,
                chatErrorMessage = null,
            )
        }

        val routesSummary = uiState.value.allRoutes.joinToString("\n") { r ->
            "- Ruta ${r.code} (${r.title}, ${r.transportType.labelEs}, ${r.durationMinutes} min, ${r.formattedPrice}, ${r.capacity.labelEs})"
        }
        val gps = controls.value.currentGpsLocation

        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            try {
                aiAssistant.sendMessageStream(
                    userMessage = clean,
                    history = priorHistory,
                    modelTier = selectedTier,
                    userLat = gps?.lat,
                    userLng = gps?.lng,
                    routesSummary = routesSummary,
                ).collect { event ->
                    when (event) {
                        is StreamChatEvent.PartialText -> {
                            controls.update { ctrl ->
                                ctrl.copy(
                                    chatMessages = ctrl.chatMessages.map { m ->
                                        if (m.id == assistantMsgId) m.copy(text = event.text) else m
                                    }
                                )
                            }
                        }

                        is StreamChatEvent.Completed -> {
                            controls.update { ctrl ->
                                ctrl.copy(
                                    chatMessages = ctrl.chatMessages.map { m ->
                                        if (m.id == assistantMsgId) {
                                            m.copy(
                                                text = event.fullText,
                                                isStreaming = false,
                                                mapsSources = event.mapsSources,
                                            )
                                        } else {
                                            m
                                        }
                                    },
                                    isChatSending = false,
                                )
                            }
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e("MueveTChat", "Gemini chat timed out", e)
                controls.update { ctrl ->
                    ctrl.copy(
                        chatMessages = ctrl.chatMessages.filterNot { it.id == assistantMsgId && it.text.isBlank() },
                        isChatSending = false,
                        chatErrorMessage = "La consulta tardó más de 60 segundos. Intenta de nuevo.",
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MueveTChat", "Gemini chat failed", e)
                controls.update { ctrl ->
                    ctrl.copy(
                        chatMessages = ctrl.chatMessages.filterNot { it.id == assistantMsgId && it.text.isBlank() },
                        isChatSending = false,
                        chatErrorMessage = e.localizedMessage ?: "No se pudo conectar con Gemini.",
                    )
                }
            }
        }
    }

    fun dismissSnackbar() {
        controls.update { it.copy(snackbarMessage = null) }
    }

    override fun onCleared() {
        cancelCloudListeners()
        chatJob?.cancel()
        super.onCleared()
    }

    class Factory(
        private val repository: TransitRepository,
        private val cloudRepository: UserCloudRepository,
        private val gpsClient: GpsLocationClient,
        private val aiAssistant: TransitAiAssistant,
        private val currentUserId: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MueveTViewModel(
                repository = repository,
                cloudRepository = cloudRepository,
                gpsClient = gpsClient,
                aiAssistant = aiAssistant,
                currentUserId = currentUserId,
            ) as T
        }
    }
}
