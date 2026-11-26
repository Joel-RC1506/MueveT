package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiModelTier
import com.example.data.ai.ChatMessage
import com.example.data.ai.MapsGroundingSource
import com.example.ui.theme.CapacityHigh
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CreamBg
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.SagePillBg
import com.example.ui.theme.SageTint
import com.example.ui.theme.SoftDivider
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.Terracotta

private val SuggestedPrompts = listOf(
    "¿Qué ruta conviene del Centro a Ciudad Universitaria?",
    "Paradas de camión cerca de Paseo Tollocan",
    "Opciones más baratas hacia Metepec",
    "Rutas cercanas a la Terminal de Toluca",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssistantChatScreen(
    messages: List<ChatMessage>,
    selectedModel: AiModelTier,
    isSending: Boolean,
    errorMessage: String?,
    onSelectModel: (AiModelTier) -> Unit,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, messages.lastOrNull()?.text) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
        ) {
            // Top intro & model selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ASISTENTE INTELIGENTE · MAPS GROUNDING",
                            style = MaterialTheme.typography.labelSmall,
                            color = Terracotta,
                            letterSpacing = 0.9.sp,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pregunta por tu ruta",
                            style = MaterialTheme.typography.headlineSmall,
                            color = InkPrimary,
                        )
                    }
                    if (messages.isNotEmpty()) {
                        TextButton(onClick = onClearChat) {
                            Icon(Icons.Filled.Refresh, null, tint = InkSecondary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Nuevo", color = InkSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AiModelTier.entries.forEach { tier ->
                        val active = tier == selectedModel
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (active) ForestGreen else SurfaceWhite,
                            border = BorderStroke(1.dp, if (active) ForestGreen else CardBorder),
                            modifier = Modifier
                                .clickable { onSelectModel(tier) }
                                .testTag("ai_model_${tier.name.lowercase()}"),
                        ) {
                            Text(
                                text = "${tier.labelEs} · ${tier.descriptionEs}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) SurfaceWhite else InkPrimary,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = SoftDivider, thickness = 1.dp)

            // Scrollable message thread
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (messages.isEmpty()) {
                    item(key = "welcome_card") {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = SageTint,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = ForestGreen,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Copiloto de transporte MueveT",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ForestGreen,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Consulta combinaciones de rutas, tarifas estimadas, paradas cercanas y lugares reales conectados con Google Maps.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkPrimary,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Sugerencias rápidas",
                            style = MaterialTheme.typography.labelLarge,
                            color = InkSecondary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SuggestedPrompts.forEach { prompt ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = SurfaceWhite,
                                    border = BorderStroke(1.dp, CardBorder),
                                    modifier = Modifier.clickable { onSendMessage(prompt) },
                                ) {
                                    Text(
                                        text = prompt,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(messages, key = { it.id }) { msg ->
                        ChatBubble(message = msg)
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    item(key = "chat_error") {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = CapacityHigh.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, CapacityHigh.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = CapacityHigh,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                }
            }

            // Composer bar
            Surface(
                color = SurfaceWhite,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    HorizontalDivider(color = SoftDivider, thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Escribe tu destino o pregunta de ruta...") },
                            maxLines = 3,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = CreamBg,
                                unfocusedContainerColor = CreamBg,
                                focusedBorderColor = Terracotta,
                                unfocusedBorderColor = CardBorder,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input"),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = if (inputText.isNotBlank() && !isSending) Terracotta else CardBorder,
                            modifier = Modifier.size(48.dp),
                        ) {
                            IconButton(
                                enabled = inputText.isNotBlank() && !isSending,
                                onClick = {
                                    val query = inputText.trim()
                                    inputText = ""
                                    onSendMessage(query)
                                },
                                modifier = Modifier.testTag("chat_send_button"),
                            ) {
                                if (isSending) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = InkPrimary,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Enviar mensaje",
                                        tint = InkPrimary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.isUser) 18.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 18.dp,
            ),
            color = if (message.isUser) ForestGreen else SurfaceWhite,
            border = if (message.isUser) null else BorderStroke(1.dp, CardBorder),
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (!message.isUser && !message.modelLabel.isNullOrBlank()) {
                    Text(
                        text = message.modelLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Terracotta,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = message.text.ifBlank { "..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isUser) SurfaceWhite else InkPrimary,
                )
            }
        }

        if (!message.isUser && message.mapsSources.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            GoogleMapsSourcesCard(
                sources = message.mapsSources,
                onOpenSource = { uri ->
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
                    }
                },
            )
        }
    }
}

@Composable
private fun GoogleMapsSourcesCard(
    sources: List<MapsGroundingSource>,
    onOpenSource: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SagePillBg,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.widthIn(max = 320.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Place,
                        contentDescription = null,
                        tint = ForestGreen,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Attribution strictly follows Grounding with Google Maps guidelines
                    Text(
                        text = "Google Maps",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = Color(0xFF5E5E5E),
                        maxLines = 1,
                    )
                }
                Text(
                    text = if (expanded) "Ocultar (${sources.size})" else "Ver fuentes (${sources.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = ForestGreen,
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                sources.forEach { src ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenSource(src.uri) }
                            .padding(vertical = 5.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = src.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = InkPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Abrir en Google Maps",
                            tint = Terracotta,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        }
    }
}
