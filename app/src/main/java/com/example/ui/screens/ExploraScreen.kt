package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.RouteFilter
import com.example.data.model.TransitRoute
import com.example.ui.components.RouteCard
import com.example.ui.components.TransitMapCanvas
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.InkTertiary
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.Terracotta
import com.example.ui.viewmodel.MueveTUiState

@Composable
fun ExploraScreen(
    uiState: MueveTUiState,
    onSelectRouteOnMap: (String) -> Unit,
    onOpenRouteDetail: (String) -> Unit,
    onToggleSaveRoute: (TransitRoute) -> Unit,
    onSelectFilter: (RouteFilter) -> Unit,
    onOpenTripSearch: () -> Unit,
    onClearTripSearch: () -> Unit,
    onRequestLocationOrRecenter: () -> Unit,
    onReportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasActiveTripQuery = uiState.originQuery.isNotBlank() || uiState.destinationQuery.isNotBlank()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
        ) {
            item(key = "map") {
                TransitMapCanvas(
                    routes = uiState.allRoutes,
                    selectedRouteId = uiState.selectedRouteId,
                    hasLocationPermission = uiState.hasLocationPermission,
                    currentGpsLocation = uiState.currentGpsLocation,
                    currentPlaceName = uiState.currentPlaceName,
                    recordedGpsTrail = uiState.recordedGpsTrail,
                    onSelectRoute = onSelectRouteOnMap,
                    onRequestLocationOrRecenter = onRequestLocationOrRecenter,
                    onReportClick = onReportClick,
                )
            }

            item(key = "hero_and_search") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(
                            modifier = Modifier.width(20.dp),
                            thickness = 1.5.dp,
                            color = ForestGreen.copy(alpha = 0.65f),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.explora_eyebrow),
                            style = MaterialTheme.typography.labelSmall,
                            color = ForestGreen,
                            letterSpacing = 1.1.sp,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = InkPrimary,
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 31.sp,
                                    letterSpacing = (-0.7).sp,
                                )
                            ) {
                                append(stringResource(R.string.explora_headline_prefix))
                                append(" ")
                            }
                            withStyle(
                                SpanStyle(
                                    color = Terracotta,
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 31.sp,
                                    letterSpacing = (-0.7).sp,
                                )
                            ) {
                                append(stringResource(R.string.explora_headline_accent))
                            }
                        },
                        lineHeight = 36.sp,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.explora_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkSecondary,
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // "¿A dónde vas?" interactive trip planner bar
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SurfaceWhite,
                        border = BorderStroke(1.dp, CardBorder),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenTripSearch)
                            .testTag("where_to_search_bar"),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 15.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = Terracotta,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (hasActiveTripQuery) {
                                    val from = uiState.originQuery.ifBlank {
                                        uiState.currentDetailedAddress ?: uiState.currentPlaceName ?: "Mi ubicación"
                                    }
                                    val to = uiState.destinationQuery.ifBlank { "Cualquier destino" }
                                    "$from → $to"
                                } else {
                                    stringResource(R.string.search_where_to)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (hasActiveTripQuery) InkPrimary else InkSecondary,
                                fontWeight = if (hasActiveTripQuery) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (hasActiveTripQuery) {
                                IconButton(
                                    onClick = onClearTripSearch,
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Limpiar búsqueda",
                                        tint = InkSecondary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = InkSecondary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Filter chips row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        FilterChipPill(
                            label = RouteFilter.FASTEST.labelEs,
                            selected = uiState.selectedFilter == RouteFilter.FASTEST,
                            icon = { tint ->
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            onClick = { onSelectFilter(RouteFilter.FASTEST) },
                            testTag = "filter_fastest",
                        )
                        FilterChipPill(
                            label = RouteFilter.CHEAPEST.labelEs,
                            selected = uiState.selectedFilter == RouteFilter.CHEAPEST,
                            icon = { tint ->
                                Icon(
                                    imageVector = Icons.Filled.AttachMoney,
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            onClick = { onSelectFilter(RouteFilter.CHEAPEST) },
                            testTag = "filter_cheapest",
                        )
                        FilterChipPill(
                            label = RouteFilter.LEAST_CROWDED.labelEs,
                            selected = uiState.selectedFilter == RouteFilter.LEAST_CROWDED,
                            icon = { tint ->
                                Icon(
                                    imageVector = Icons.Filled.Waves,
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            onClick = { onSelectFilter(RouteFilter.LEAST_CROWDED) },
                            testTag = "filter_least_crowded",
                        )
                    }
                }
            }

            // Main routes section header
            item(key = "nearby_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (hasActiveTripQuery) {
                            "Rutas para tu viaje (${uiState.filteredRoutes.size})"
                        } else {
                            "Rutas cerca de ti"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = InkPrimary,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.NearMe,
                            contentDescription = null,
                            tint = InkSecondary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                uiState.hasLocationPermission && uiState.currentGpsLocation != null ->
                                    uiState.currentPlaceName ?: "GPS en vivo"
                                else -> "Toluca"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary,
                        )
                    }
                }
            }

            if (uiState.filteredRoutes.isEmpty()) {
                item(key = "empty_search") {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SurfaceWhite,
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "No encontramos rutas directas para ese trayecto",
                                style = MaterialTheme.typography.titleMedium,
                                color = InkPrimary,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Prueba otro destino o mapea esta ruta en Contribuir para ayudar a la comunidad.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkSecondary,
                            )
                        }
                    }
                }
            } else {
                items(uiState.filteredRoutes, key = { "main_${it.id}" }) { route ->
                    RouteCard(
                        route = route,
                        isHighlighted = route.id == uiState.selectedRouteId,
                        onClick = { onOpenRouteDetail(route.id) },
                        onToggleSave = { onToggleSaveRoute(route) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 7.dp),
                    )
                }
            }

            // Recent activity section
            if (!hasActiveTripQuery && uiState.recentRoutes.isNotEmpty()) {
                item(key = "recent_header") {
                    Text(
                        text = "Transitado recientemente",
                        style = MaterialTheme.typography.titleMedium,
                        color = InkPrimary,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp),
                    )
                }
                items(uiState.recentRoutes.take(2), key = { "recent_${it.id}" }) { route ->
                    RouteCard(
                        route = route,
                        isHighlighted = false,
                        onClick = { onOpenRouteDetail(route.id) },
                        onToggleSave = { onToggleSaveRoute(route) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                }
            }

            // Recommendations based on activity
            if (!hasActiveTripQuery && uiState.recommendedRoutes.isNotEmpty()) {
                item(key = "recommended_header") {
                    Text(
                        text = "Recomendadas según tu actividad",
                        style = MaterialTheme.typography.titleMedium,
                        color = InkPrimary,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp),
                    )
                }
                items(uiState.recommendedRoutes.take(2), key = { "rec_${it.id}" }) { route ->
                    RouteCard(
                        route = route,
                        isHighlighted = false,
                        onClick = { onOpenRouteDetail(route.id) },
                        onToggleSave = { onToggleSaveRoute(route) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                }
            }

            item(key = "footer") {
                Text(
                    text = stringResource(R.string.footer_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkTertiary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                )
            }
        }
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    selected: Boolean,
    icon: @Composable (tint: androidx.compose.ui.graphics.Color) -> Unit,
    onClick: () -> Unit,
    testTag: String,
) {
    val bgColor = if (selected) ForestGreen else SurfaceWhite
    val contentColor = if (selected) SurfaceWhite else InkPrimary
    val borderColor = if (selected) ForestGreen else CardBorder

    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .height(42.dp)
            .clickable(onClick = onClick)
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon(contentColor)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
