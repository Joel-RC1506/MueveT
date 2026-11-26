package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.GpsPoint
import com.example.ui.theme.CapacityLow
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CreamBg
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.SageTint
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.Terracotta
import java.util.Locale

private val PopularDestinations = listOf(
    "Centro",
    "Terminal Toluca",
    "Ciudad Universitaria",
    "Paseo Tollocan",
    "Metepec",
    "Zinacantepec",
    "Galerías Toluca",
    "Pilares",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TripSearchSheet(
    initialOrigin: String,
    initialDestination: String,
    initialIsLiveOrigin: Boolean,
    liveAddress: String?,
    liveCoordinates: GpsPoint?,
    hasLocationPermission: Boolean,
    onRefreshLocation: () -> Unit,
    onDismiss: () -> Unit,
    onApplySearch: (origin: String, destination: String, useLiveOrigin: Boolean) -> Unit,
    onClearSearch: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val resolvedLiveText = remember(liveAddress, liveCoordinates) {
        liveAddress?.takeIf { it.isNotBlank() }
            ?: liveCoordinates?.let { String.format(Locale.US, "GPS: %.4f, %.4f", it.lat, it.lng) }
    }

    var isFollowingLiveLocation by remember(initialOrigin, initialIsLiveOrigin) {
        mutableStateOf(
            initialIsLiveOrigin ||
                initialOrigin.isBlank() ||
                initialOrigin.equals("Mi ubicación actual", ignoreCase = true)
        )
    }
    var origin by remember(initialOrigin) {
        mutableStateOf(
            if (isFollowingLiveLocation) resolvedLiveText.orEmpty() else initialOrigin
        )
    }
    var destination by remember(initialDestination) { mutableStateOf(initialDestination) }

    LaunchedEffect(resolvedLiveText, isFollowingLiveLocation) {
        if (isFollowingLiveLocation && !resolvedLiveText.isNullOrBlank()) {
            origin = resolvedLiveText
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CreamBg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Planear viaje",
                    style = MaterialTheme.typography.headlineSmall,
                    color = InkPrimary,
                )
                TextButton(
                    onClick = {
                        onClearSearch()
                        onDismiss()
                    }
                ) {
                    Text("Limpiar", color = Terracotta)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = origin,
                        onValueChange = {
                            origin = it
                            isFollowingLiveLocation = false
                        },
                        label = {
                            Text(if (isFollowingLiveLocation) "Mi ubicación actual" else "Punto de partida")
                        },
                        placeholder = {
                            Text(
                                if (hasLocationPermission) {
                                    "Obteniendo tu ubicación en tiempo real..."
                                } else {
                                    "Activa GPS o escribe tu punto de partida"
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.MyLocation,
                                contentDescription = null,
                                tint = if (isFollowingLiveLocation) CapacityLow else ForestGreen,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    isFollowingLiveLocation = true
                                    onRefreshLocation()
                                    if (!resolvedLiveText.isNullOrBlank()) {
                                        origin = resolvedLiveText
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("use_live_location_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.GpsFixed,
                                    contentDescription = "Usar mi ubicación actual en tiempo real",
                                    tint = if (isFollowingLiveLocation) ForestGreen else InkSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                        supportingText = {
                            when {
                                isFollowingLiveLocation && !resolvedLiveText.isNullOrBlank() -> {
                                    Text(
                                        text = "Actualizándose en tiempo real con tu GPS",
                                        color = CapacityLow,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                                !hasLocationPermission -> {
                                    Text(
                                        text = "Toca el ícono de GPS para escribir tu ubicación en tiempo real",
                                        color = InkSecondary,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceWhite,
                            unfocusedContainerColor = SurfaceWhite,
                            focusedBorderColor = ForestGreen,
                            unfocusedBorderColor = CardBorder,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("trip_origin_input"),
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = { Text("¿A dónde vas?") },
                        placeholder = { Text("Ej. Terminal Toluca, Metepec, CU") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Place,
                                contentDescription = null,
                                tint = Terracotta,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceWhite,
                            unfocusedContainerColor = SurfaceWhite,
                            focusedBorderColor = Terracotta,
                            unfocusedBorderColor = CardBorder,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("trip_destination_input"),
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        val tmp = origin
                        origin = destination
                        destination = tmp
                        isFollowingLiveLocation = false
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.SwapVert,
                        contentDescription = "Intercambiar origen y destino",
                        tint = InkSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Zonas frecuentes",
                style = MaterialTheme.typography.labelLarge,
                color = InkSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PopularDestinations.forEach { place ->
                    val selected = destination.equals(place, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (selected) SageTint else SurfaceWhite,
                        border = BorderStroke(1.dp, if (selected) ForestGreen else CardBorder),
                        modifier = Modifier.clickable { destination = place },
                    ) {
                        Text(
                            text = place,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selected) ForestGreen else InkPrimary,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onApplySearch(origin, destination, isFollowingLiveLocation) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Terracotta,
                    contentColor = InkPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("trip_search_submit"),
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Mostrar rutas disponibles",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
