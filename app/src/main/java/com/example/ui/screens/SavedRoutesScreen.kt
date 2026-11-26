package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.TransitRoute
import com.example.ui.components.RouteCard
import com.example.ui.theme.CardBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.InkTertiary
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.Terracotta

@Composable
fun SavedRoutesScreen(
    savedRoutes: List<TransitRoute>,
    onOpenRouteDetail: (String) -> Unit,
    onToggleSaveRoute: (TransitRoute) -> Unit,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                        text = stringResource(R.string.saved_eyebrow),
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSecondary,
                        letterSpacing = 1.1.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.saved_title),
                        style = MaterialTheme.typography.displaySmall,
                        color = InkPrimary,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.saved_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkSecondary,
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                }
            }

            if (savedRoutes.isEmpty()) {
                item(key = "empty_saved") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SurfaceWhite,
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BookmarkBorder,
                                contentDescription = null,
                                tint = Terracotta,
                                modifier = Modifier.size(36.dp),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Aún no tienes rutas guardadas",
                                style = MaterialTheme.typography.titleMedium,
                                color = InkPrimary,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Guarda tus rutas más usadas desde Explora para tenerlas a la mano y enterarte de cambios en tarifas o capacidad.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkSecondary,
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = onExploreClick,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Terracotta,
                                    contentColor = InkPrimary,
                                ),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("empty_saved_explore_btn"),
                            ) {
                                Text(
                                    text = "Explorar rutas",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            } else {
                items(savedRoutes, key = { it.id }) { route ->
                    RouteCard(
                        route = route,
                        isHighlighted = false,
                        showChangeAlert = true,
                        onClick = { onOpenRouteDetail(route.id) },
                        onToggleSave = { onToggleSaveRoute(route) },
                        modifier = Modifier.padding(bottom = 14.dp),
                    )
                }
            }

            item(key = "footer") {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.footer_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkTertiary,
                )
            }
        }
    }
}
