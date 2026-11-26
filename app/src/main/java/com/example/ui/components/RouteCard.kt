package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.TransitRoute
import com.example.ui.theme.CapacityLow
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.SagePillBg
import com.example.ui.theme.SoftDivider
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.Terracotta
import com.example.ui.theme.WarmOrangeBorder
import com.example.ui.theme.WarmOrangeSurface

@Composable
fun RouteCard(
    route: TransitRoute,
    isHighlighted: Boolean,
    showChangeAlert: Boolean = false,
    onClick: () -> Unit,
    onToggleSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (isHighlighted) WarmOrangeSurface else SurfaceWhite
    val borderColor = if (isHighlighted) WarmOrangeBorder else CardBorder
    val badgeColor = Color(route.badgeColorHex)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(if (isHighlighted) 1.5.dp else 1.dp, borderColor),
        shadowElevation = if (isHighlighted) 2.dp else 0.5.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("route_card_${route.id}"),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top tag + frequency row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!route.highlightTag.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SagePillBg,
                    ) {
                        Text(
                            text = route.highlightTag,
                            style = MaterialTheme.typography.labelMedium,
                            color = ForestGreen,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SagePillBg,
                    ) {
                        Text(
                            text = "${route.stops.size} paradas",
                            style = MaterialTheme.typography.labelMedium,
                            color = InkSecondary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = InkSecondary,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Cada ${route.frequencyMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Route badge, title, subtitle & travel duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val badgeWidth = if (route.code.length > 4) 68.dp else 48.dp
                Box(
                    modifier = Modifier
                        .width(badgeWidth)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = route.code,
                        color = SurfaceWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (route.code.length > 4) 15.sp else 17.sp,
                        maxLines = 1,
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = route.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = InkPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = route.subtitleLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${route.durationMinutes}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = InkPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "min",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
            }

            if (showChangeAlert && !route.recentChangeNote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SagePillBg,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(CapacityLow)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = route.recentChangeNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = ForestGreen,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SoftDivider, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Bottom capacity, price, and save icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CapacitySignalBars(capacity = route.capacity)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = route.capacity.badgeLabelEs,
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = route.formattedPrice,
                        style = MaterialTheme.typography.titleSmall,
                        color = InkPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onToggleSave,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("bookmark_${route.id}"),
                    ) {
                        Icon(
                            imageVector = if (route.isSaved) {
                                Icons.Filled.Bookmark
                            } else {
                                Icons.Outlined.BookmarkBorder
                            },
                            contentDescription = stringResource(R.string.cd_bookmark_route),
                            tint = if (route.isSaved) Terracotta else InkSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
