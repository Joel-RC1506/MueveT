package com.example.data.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.LatLng
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.ToolConfig
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.retrievalConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

enum class AiModelTier(
    val modelId: String,
    val labelEs: String,
    val descriptionEs: String,
) {
    GENERAL(
        modelId = "gemini-2.5-flash",
        labelEs = "Estándar",
        descriptionEs = "Equilibrado con Google Maps",
    ),
    FAST(
        modelId = "gemini-2.5-flash-lite",
        labelEs = "Rápido",
        descriptionEs = "Respuestas veloces",
    ),
    DEEP(
        modelId = "gemini-2.5-pro",
        labelEs = "Profundo",
        descriptionEs = "Razonamiento complejo",
    ),
}

data class MapsGroundingSource(
    val title: String,
    val uri: String,
    val placeId: String?,
)

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false,
    val mapsSources: List<MapsGroundingSource> = emptyList(),
    val modelLabel: String? = null,
)

sealed interface StreamChatEvent {
    data class PartialText(val text: String) : StreamChatEvent
    data class Completed(
        val fullText: String,
        val mapsSources: List<MapsGroundingSource>,
    ) : StreamChatEvent
}

class TransitAiAssistant {

    private val systemPrompt = content {
        text(
            "Eres el asistente inteligente de movilidad urbana de MueveT en Toluca, Estado de México. " +
                "Tu misión es ayudar a las personas a encontrar rutas de transporte público (camiones, combis, " +
                "taxis colectivos y el Tren Interurbano El Insurgente) en Toluca, Metepec, Zinacantepec y Lerma, " +
                "ubicar paradas reales cercanas, comparar tarifas en MXN, tiempos de traslado y niveles de saturación. " +
                "Usa Google Maps cuando la consulta involucre ubicaciones, paradas, estaciones o lugares reales. " +
                "Responde siempre en español claro, directo y práctico."
        )
    }

    fun sendMessageStream(
        userMessage: String,
        history: List<ChatMessage>,
        modelTier: AiModelTier,
        userLat: Double?,
        userLng: Double?,
        routesSummary: String,
    ): Flow<StreamChatEvent> = flow {
        val lat = userLat ?: 19.2826
        val lng = userLng ?: -99.6557

        val config = retrievalConfig {
            latLng = LatLng(latitude = lat, longitude = lng)
            languageCode = "es-MX"
        }
        val toolConfig = ToolConfig(retrievalConfig = config)

        val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = modelTier.modelId,
            systemInstruction = systemPrompt,
            tools = listOf(Tool.googleMaps()),
            toolConfig = toolConfig,
        )

        val chatHistory: List<Content> = buildList {
            if (routesSummary.isNotBlank()) {
                add(
                    content("user") {
                        text("Contexto actual del catálogo local de rutas MueveT:\n$routesSummary")
                    }
                )
                add(
                    content("model") {
                        text("Entendido. Tendré en cuenta estas rutas y sus tarifas actuales para ayudarte.")
                    }
                )
            }
            history
                .filter { it.text.isNotBlank() && !it.isStreaming }
                .takeLast(12)
                .forEach { msg ->
                    add(content(if (msg.isUser) "user" else "model") { text(msg.text) })
                }
        }

        val chat = model.startChat(history = chatHistory)
        val accumulated = StringBuilder()
        val collectedSources = linkedMapOf<String, MapsGroundingSource>()

        withTimeout(60_000L) {
            chat.sendMessageStream(userMessage).collect { chunk ->
                val chunkText = chunk.text.orEmpty()
                if (chunkText.isNotEmpty()) {
                    accumulated.append(chunkText)
                    emit(StreamChatEvent.PartialText(accumulated.toString()))
                }
                val groundingChunks = chunk.candidates.firstOrNull()?.groundingMetadata?.groundingChunks
                groundingChunks?.forEach { gChunk ->
                    val maps = gChunk.maps
                    if (maps != null && !maps.uri.isNullOrBlank()) {
                        val title = maps.title?.takeIf { it.isNotBlank() } ?: "Ver lugar"
                        val uri = maps.uri!!
                        collectedSources[uri] = MapsGroundingSource(
                            title = title,
                            uri = uri,
                            placeId = maps.placeId,
                        )
                    }
                }
            }
        }

        val finalText = accumulated.toString().trim()
        if (finalText.isBlank()) {
            throw IllegalStateException("El asistente no devolvió contenido. Intenta de nuevo.")
        }
        emit(
            StreamChatEvent.Completed(
                fullText = finalText,
                mapsSources = collectedSources.values.toList(),
            )
        )
    }
}
