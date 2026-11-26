package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CapacityLevel
import com.example.data.model.TransitRoute
import com.example.data.model.TransportType
import com.example.ui.components.CapacitySignalBars
import com.example.ui.theme.CapacityLow
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CreamBg
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.InkTertiary
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.SageBorder
import com.example.ui.theme.SageTint
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.Terracotta
import com.example.ui.viewmodel.MueveTUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ContributeScreen(
    uiState: MueveTUiState,
    onSelectRoute: (TransitRoute) -> Unit,
    onToggleNewRouteMode: (Boolean) -> Unit,
    onUpdateNewRouteCode: (String) -> Unit,
    onUpdateNewRouteOrigin: (String) -> Unit,
    onUpdateNewRouteDestination: (String) -> Unit,
    onUpdateNewRouteType: (TransportType) -> Unit,
    onSelectCapacity: (CapacityLevel) -> Unit,
    onUpdateFare: (String) -> Unit,
    onAddStop: (String) -> Unit,
    onRemoveStop: (String) -> Unit,
    onToggleGpsTracking: () -> Unit,
    onCaptureGpsWaypoint: () -> Unit,
    onSubmitReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val form = uiState.contributeForm
    val selectedRoute = uiState.allRoutes.firstOrNull { it.id == form.selectedRouteId }
        ?: uiState.allRoutes.firstOrNull()

    var showRoutePicker by remember { mutableStateOf(false) }
    var showAddStopDialog by remember { mutableStateOf(false) }
    var stopDraftName by remember { mutableStateOf("") }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        ) {
            item(key = "header") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.contribute_eyebrow),
                        style = MaterialTheme.typography.labelSmall,
                        color = Terracotta,
                        letterSpacing = 1.1.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.contribute_title),
                        style = MaterialTheme.typography.displaySmall,
                        color = InkPrimary,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.contribute_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkSecondary,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Selected route card OR New route form
            item(key = "route_selector") {
                if (!form.isNewRouteMode) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SageTint,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRoutePicker = true }
                            .testTag("contribute_route_selector"),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DirectionsBus,
                                contentDescription = null,
                                tint = InkPrimary,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (selectedRoute != null) {
                                        "Ruta ${selectedRoute.code} · ${selectedRoute.transportType.labelEs}"
                                    } else {
                                        "Seleccionar ruta"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = InkPrimary,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = if (selectedRoute != null) {
                                        "${selectedRoute.origin} → ${selectedRoute.destination}"
                                    } else {
                                        "Toca para elegir ruta"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkSecondary,
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Cambiar ruta",
                                tint = InkSecondary,
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SageTint,
                        border = BorderStroke(1.dp, SageBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Nueva ruta con GPS",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ForestGreen,
                                    fontWeight = FontWeight.Bold,
                                )
                                TextButton(onClick = { onToggleNewRouteMode(false) }) {
                                    Text("Elegir existente", color = Terracotta)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = form.newRouteCode,
                                onValueChange = onUpdateNewRouteCode,
                                label = { Text("Número o clave (ej. R-42)") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SurfaceWhite,
                                    unfocusedContainerColor = SurfaceWhite,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = form.newRouteOrigin,
                                    onValueChange = onUpdateNewRouteOrigin,
                                    label = { Text("Origen") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = SurfaceWhite,
                                        unfocusedContainerColor = SurfaceWhite,
                                    ),
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedTextField(
                                    value = form.newRouteDestination,
                                    onValueChange = onUpdateNewRouteDestination,
                                    label = { Text("Destino") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = SurfaceWhite,
                                        unfocusedContainerColor = SurfaceWhite,
                                    ),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                listOf(
                                    TransportType.CAMION,
                                    TransportType.COMBI,
                                    TransportType.TAXI_COLECTIVO,
                                    TransportType.TROLEBUS,
                                ).forEach { type ->
                                    val chosen = form.newRouteType == type
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (chosen) ForestGreen else SurfaceWhite,
                                        border = BorderStroke(1.dp, if (chosen) ForestGreen else CardBorder),
                                        modifier = Modifier.clickable { onUpdateNewRouteType(type) },
                                    ) {
                                        Text(
                                            text = type.labelEs,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (chosen) SurfaceWhite else InkPrimary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
            }

            // Capacity question
            item(key = "capacity_selector") {
                Text(
                    text = "¿Qué tan llena va?",
                    style = MaterialTheme.typography.titleMedium,
                    color = InkPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CapacityLevel.entries.forEach { level ->
                        val selected = form.capacity == level
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) SageTint else SurfaceWhite,
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) SageBorder else CardBorder,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(98.dp)
                                .clickable { onSelectCapacity(level) }
                                .testTag("capacity_${level.name.lowercase()}"),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                CapacitySignalBars(
                                    capacity = level,
                                    barWidth = 5.dp,
                                    maxBarHeight = 16.dp,
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = level.labelEs,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = InkPrimary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
            }

            // Fare question
            item(key = "fare_input") {
                Text(
                    text = "¿Cuánto pagaste?",
                    style = MaterialTheme.typography.titleMedium,
                    color = InkPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "$",
                            style = MaterialTheme.typography.titleMedium,
                            color = InkSecondary,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        BasicTextField(
                            value = form.fareText,
                            onValueChange = onUpdateFare,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = TextStyle(
                                fontFamily = PlusJakartaSans,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = InkPrimary,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("fare_input"),
                        )
                        Text(
                            text = "MXN",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSecondary,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Mark stop card
            item(key = "mark_stop") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAddStopDialog = true }
                        .testTag("mark_stop_button"),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Navigation,
                            contentDescription = null,
                            tint = InkPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Marcar una parada",
                            style = MaterialTheme.typography.titleMedium,
                            color = InkPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = InkSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                if (form.addedStops.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        form.addedStops.forEach { stop ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = SageTint,
                                border = BorderStroke(1.dp, SageBorder),
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = stop,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ForestGreen,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Quitar parada $stop",
                                        tint = ForestGreen,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { onRemoveStop(stop) },
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // GPS route mapping card
            item(key = "gps_mapping") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (uiState.isRecordingGpsPath) SageTint else SurfaceWhite,
                    border = BorderStroke(
                        width = if (uiState.isRecordingGpsPath) 1.5.dp else 1.dp,
                        color = if (uiState.isRecordingGpsPath) SageBorder else CardBorder,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleGpsTracking() }
                        .testTag("gps_map_route_card"),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Map,
                                contentDescription = null,
                                tint = if (uiState.isRecordingGpsPath) ForestGreen else InkPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Mapear una nueva ruta",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = InkPrimary,
                                    fontWeight = FontWeight.Medium,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when {
                                        uiState.isRecordingGpsPath ->
                                            "Grabando recorrido GPS · ${uiState.recordedGpsTrail.size} puntos"
                                        uiState.hasLocationPermission && uiState.currentGpsLocation != null ->
                                            "GPS listo (${String.format("%.4f, %.4f", uiState.currentGpsLocation.lat, uiState.currentGpsLocation.lng)})"
                                        uiState.hasLocationPermission ->
                                            "GPS conectado · Toca para grabar trazo"
                                        else ->
                                            "Toca para activar GPS y marcar el camino"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (uiState.isRecordingGpsPath) ForestGreen else InkSecondary,
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = if (uiState.isRecordingGpsPath) CapacityLow else Terracotta,
                                modifier = Modifier.size(12.dp),
                            ) {}
                        }

                        if (uiState.isRecordingGpsPath || !form.isNewRouteMode) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                OutlinedButton(
                                    onClick = onCaptureGpsWaypoint,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, CardBorder),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Filled.MyLocation, null, tint = ForestGreen, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Fijar punto GPS", color = InkPrimary, fontSize = 12.sp)
                                }
                                if (!form.isNewRouteMode) {
                                    OutlinedButton(
                                        onClick = { onToggleNewRouteMode(true) },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, CardBorder),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("Crear nueva ruta", color = Terracotta, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Submit CTA
            item(key = "submit_button") {
                Button(
                    onClick = onSubmitReport,
                    enabled = !form.isSubmitting,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Terracotta,
                        contentColor = InkPrimary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("submit_report_button"),
                ) {
                    Text(
                        text = if (form.isSubmitting) "Enviando..." else "Enviar reporte",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.local_storage_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (uiState.recentReports.isNotEmpty()) {
                item(key = "recent_reports_title") {
                    Spacer(modifier = Modifier.height(22.dp))
                    Text(
                        text = "Reportes recientes en tiempo real",
                        style = MaterialTheme.typography.titleMedium,
                        color = InkPrimary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(uiState.recentReports.take(5), key = { it.id }) { report ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceWhite,
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                CapacitySignalBars(capacity = report.capacity)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Ruta ${report.routeCode} · ${report.capacity.labelEs}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = InkPrimary,
                                    )
                                    Text(
                                        text = buildString {
                                            append("$${String.format("%.2f", report.priceMxn)} MXN")
                                            if (report.addedStops.isNotEmpty()) {
                                                append(" · Paradas: ${report.addedStops.joinToString(", ")}")
                                            }
                                            if (report.gpsPointsCount > 0) {
                                                append(" · ${report.gpsPointsCount} pts GPS")
                                            }
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkSecondary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "footer") {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.footer_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkTertiary,
                )
            }
        }
    }

    if (showRoutePicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showRoutePicker = false },
            sheetState = sheetState,
            containerColor = CreamBg,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Selecciona tu ruta actual",
                    style = MaterialTheme.typography.titleLarge,
                    color = InkPrimary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                uiState.allRoutes.forEach { route ->
                    val chosen = route.id == form.selectedRouteId
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (chosen) SageTint else SurfaceWhite,
                        border = BorderStroke(1.dp, if (chosen) SageBorder else CardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                onSelectRoute(route)
                                showRoutePicker = false
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = androidx.compose.ui.graphics.Color(route.badgeColorHex),
                            ) {
                                Text(
                                    text = route.code,
                                    color = SurfaceWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = route.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = InkPrimary,
                                )
                                Text(
                                    text = "${route.transportType.labelEs} · ${route.formattedPrice}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSecondary,
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        onToggleNewRouteMode(true)
                        showRoutePicker = false
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Add, null, tint = Terracotta)
                    Spacer(Modifier.width(6.dp))
                    Text("No está en la lista · Mapear nueva ruta", color = Terracotta)
                }
            }
        }
    }

    if (showAddStopDialog) {
        AlertDialog(
            onDismissRequest = { showAddStopDialog = false },
            containerColor = SurfaceWhite,
            title = {
                Text("Marcar parada en el camino", style = MaterialTheme.typography.titleLarge, color = InkPrimary)
            },
            text = {
                Column {
                    Text(
                        text = "Escribe el nombre de la parada o cruce que hace esta unidad.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = stopDraftName,
                        onValueChange = { stopDraftName = it },
                        label = { Text("Nombre de parada (ej. Los Portales)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_stop_dialog_input"),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddStop(stopDraftName)
                        stopDraftName = ""
                        showAddStopDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta, contentColor = InkPrimary),
                ) {
                    Text("Agregar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStopDialog = false }) {
                    Text("Cancelar", color = InkSecondary)
                }
            },
        )
    }
}
