package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RouteReport
import com.example.data.model.TransitRoute
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CreamBg
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.SageTint
import com.example.ui.theme.SoftDivider
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.Terracotta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailSheet(
    route: TransitRoute,
    reports: List<RouteReport>,
    onDismiss: () -> Unit,
    onToggleSave: () -> Unit,
    onMarkTraveled: () -> Unit,
    onReportRoute: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val badgeColor = Color(route.badgeColorHex)
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CreamBg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(badgeColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = route.code,
                        color = SurfaceWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (route.code.length > 4) 14.sp else 18.sp,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = route.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = InkPrimary,
                    )
                    Text(
                        text = route.subtitleLine,
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick stats row
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceWhite,
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Duración", style = MaterialTheme.typography.labelSmall, color = InkSecondary)
                        Text("${route.durationMinutes} min", style = MaterialTheme.typography.titleMedium, color = InkPrimary)
                    }
                    Column {
                        Text("Frecuencia", style = MaterialTheme.typography.labelSmall, color = InkSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Schedule, null, tint = InkSecondary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Cada ${route.frequencyMinutes} min", style = MaterialTheme.typography.titleMedium, color = InkPrimary)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Tarifa", style = MaterialTheme.typography.labelSmall, color = InkSecondary)
                        Text(route.formattedPrice, style = MaterialTheme.typography.titleMedium, color = InkPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Capacity pill
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SageTint,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CapacitySignalBars(capacity = route.capacity)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Capacidad actual: ${route.capacity.labelEs}",
                                style = MaterialTheme.typography.titleSmall,
                                color = ForestGreen,
                            )
                            Text(
                                text = "${route.reportCount} reportes de la comunidad",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSecondary,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Paradas de la ruta (${route.stops.size})",
                style = MaterialTheme.typography.titleMedium,
                color = InkPrimary,
            )
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceWhite,
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    route.stops.forEachIndexed { idx, stop ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (idx == 0 || idx == route.stops.lastIndex) badgeColor else InkSecondary.copy(alpha = 0.5f))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stop.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkPrimary,
                                fontWeight = if (idx == 0 || idx == route.stops.lastIndex) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                        if (idx < route.stops.lastIndex) {
                            HorizontalDivider(
                                color = SoftDivider,
                                modifier = Modifier.padding(start = 22.dp),
                            )
                        }
                    }
                }
            }

            if (reports.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Reportes recientes en tiempo real",
                    style = MaterialTheme.typography.titleMedium,
                    color = InkPrimary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                reports.take(3).forEach { report ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CapacitySignalBars(capacity = report.capacity)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${report.capacity.labelEs} · $${String.format("%.2f", report.priceMxn)} MXN",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = InkPrimary,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    if (report.addedStops.isNotEmpty()) {
                                        Text(
                                            text = "Paradas: ${report.addedStops.joinToString(", ")}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = InkSecondary,
                                        )
                                    }
                                }
                            }
                            if (report.gpsPointsCount > 0) {
                                Text(
                                    text = "${report.gpsPointsCount} pts GPS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ForestGreen,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onToggleSave,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("sheet_save_button"),
                ) {
                    Icon(
                        imageVector = if (route.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        tint = if (route.isSaved) Terracotta else InkPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (route.isSaved) "Guardada" else "Guardar",
                        color = InkPrimary,
                    )
                }

                OutlinedButton(
                    onClick = onMarkTraveled,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("sheet_traveled_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.DirectionsBus,
                        contentDescription = null,
                        tint = ForestGreen,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(text = "Tomar ruta", color = ForestGreen)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onReportRoute,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Terracotta,
                    contentColor = InkPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sheet_report_button"),
            ) {
                Icon(Icons.Filled.AddLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Reportar unidad o tarifa",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    val start = route.stops.firstOrNull()
                    val end = route.stops.lastOrNull()
                    val uri = if (start != null && end != null) {
                        Uri.parse("https://www.google.com/maps/dir/?api=1&origin=${start.lat},${start.lng}&destination=${end.lat},${end.lng}&travelmode=transit")
                    } else {
                        Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode("${route.origin} ${route.destination}")}")
                    }
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    }
                },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sheet_open_google_maps"),
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ver trayecto en Google Maps", color = ForestGreen)
            }
        }
    }
}
