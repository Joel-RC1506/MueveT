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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.CapacityLow
import com.example.ui.theme.CreamBg
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.SageBorder
import com.example.ui.theme.SageTint
import com.example.ui.theme.SoftDivider
import com.example.ui.theme.Terracotta

@Composable
fun MueveTHeader(
    hasGpsFix: Boolean,
    placeName: String? = null,
    userInitials: String? = null,
    onAccountClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CreamBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = InkPrimary,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 23.sp,
                            letterSpacing = (-0.6).sp,
                        )
                    ) {
                        append("mueve")
                    }
                    withStyle(
                        SpanStyle(
                            color = Terracotta,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 23.sp,
                            letterSpacing = (-0.6).sp,
                        )
                    ) {
                        append("T.")
                    }
                }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(CapacityLow)
                )
                Spacer(modifier = Modifier.width(6.dp))
                val cityText = placeName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.city_cdmx)
                Text(
                    text = if (hasGpsFix) {
                        "$cityText · GPS"
                    } else {
                        cityText
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )

                if (onAccountClick != null) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = CircleShape,
                        color = SageTint,
                        border = BorderStroke(1.dp, SageBorder),
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onAccountClick() }
                            .testTag("header_account_button"),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (!userInitials.isNullOrBlank()) {
                                Text(
                                    text = userInitials,
                                    color = ForestGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "Cuenta de usuario",
                                    tint = ForestGreen,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = SoftDivider, thickness = 1.dp)
    }
}
