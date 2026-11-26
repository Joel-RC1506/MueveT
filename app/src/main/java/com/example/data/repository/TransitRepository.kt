package com.example.data.repository

import com.example.data.local.MueveTDao
import com.example.data.local.RouteReportEntity
import com.example.data.local.SeedTransitCatalog
import com.example.data.local.TransitJsonCodec
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.data.model.CapacityLevel
import com.example.data.model.GpsPoint
import com.example.data.model.MapBounds
import com.example.data.model.MapPoint
import com.example.data.model.RouteReport
import com.example.data.model.TransitRoute
import com.example.data.model.TransitStop
import com.example.data.model.TransportType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class TransitRepository(private val dao: MueveTDao) {

    val allRoutes: Flow<List<TransitRoute>> =
        dao.observeAllRoutes().map { list -> list.map { it.toDomain() } }

    val savedRoutes: Flow<List<TransitRoute>> =
        dao.observeSavedRoutes().map { list -> list.map { it.toDomain() } }

    val recentReports: Flow<List<RouteReport>> =
        dao.observeRecentReports().map { list -> list.map { it.toDomain() } }

    fun observeReportsForRoute(routeId: String): Flow<List<RouteReport>> =
        dao.observeReportsForRoute(routeId).map { list -> list.map { it.toDomain() } }

    suspend fun ensureSeeded() {
        if (dao.getRouteCount() == 0) {
            dao.insertRoutes(SeedTransitCatalog.initialRoutes().map { it.toEntity() })
        }
    }

    suspend fun toggleSavedRoute(routeId: String, currentlySaved: Boolean) {
        dao.setRouteSaved(routeId, !currentlySaved)
    }

    suspend fun syncSavedIdsFromCloud(savedIds: Set<String>) {
        savedIds.forEach { id ->
            dao.setRouteSaved(id, true)
        }
    }

    suspend fun recordRouteTraveled(routeId: String) {
        dao.markRouteTraveled(routeId, System.currentTimeMillis())
    }

    suspend fun submitReportForExistingRoute(
        routeId: String,
        capacity: CapacityLevel,
        priceMxn: Double,
        addedStopNames: List<String>,
        gpsTrail: List<GpsPoint>,
        currentLat: Double?,
        currentLng: Double?,
    ): String? {
        val existingEntity = dao.getRouteById(routeId) ?: return null
        val existing = existingEntity.toDomain()
        val now = System.currentTimeMillis()
        val reportId = "rep_${now}_${(100..999).random()}"

        val newStops = addedStopNames
            .map { it.trim() }
            .filter { name ->
                name.isNotBlank() && existing.stops.none { it.name.equals(name, ignoreCase = true) }
            }
            .mapIndexed { index, name ->
                val anchor = if (currentLat != null && currentLng != null) {
                    MapBounds.toNormalizedPoint(currentLat, currentLng)
                } else {
                    existing.stops.lastOrNull()?.let { MapPoint((it.x + 0.04f).coerceAtMost(0.92f), (it.y + 0.04f).coerceAtMost(0.92f), it.lat, it.lng) }
                        ?: MapPoint(0.5f, 0.5f, 19.2826, -99.6557)
                }
                TransitStop(
                    id = "stop_${now}_$index",
                    name = name,
                    x = anchor.x,
                    y = anchor.y,
                    lat = currentLat ?: anchor.lat,
                    lng = currentLng ?: anchor.lng,
                )
            }

        val trailMapPoints = gpsTrail
            .map { MapBounds.toNormalizedPoint(it.lat, it.lng) }

        val updatedPath = if (trailMapPoints.size >= 2) {
            (existing.pathPoints + trailMapPoints).distinctBy { "${(it.lat * 10000).toInt()}_${(it.lng * 10000).toInt()}" }
        } else {
            existing.pathPoints
        }

        val formattedFare = String.format("$%.2f MXN", priceMxn)
        val changeDetails = buildList {
            add(capacity.labelEs)
            add(formattedFare)
            if (newStops.isNotEmpty()) add("+${newStops.size} parada(s)")
            if (gpsTrail.isNotEmpty()) add("trazo GPS")
        }.joinToString(" · ")

        val updatedRoute = existing.copy(
            capacity = capacity,
            priceMxn = priceMxn,
            stops = existing.stops + newStops,
            pathPoints = updatedPath,
            recentChangeNote = "Actualizado por reporte: $changeDetails",
            reportCount = existing.reportCount + 1,
            lastUpdatedAt = now,
        )
        dao.updateRoute(updatedRoute.toEntity())

        dao.insertReport(
            RouteReportEntity(
                id = reportId,
                routeId = existing.id,
                routeCode = existing.code,
                routeTitle = existing.title,
                capacity = capacity.name,
                priceMxn = priceMxn,
                addedStopsJson = TransitJsonCodec.encodeStrings(addedStopNames),
                gpsPointsCount = gpsTrail.size,
                latitude = currentLat,
                longitude = currentLng,
                timestamp = now,
            )
        )
        return reportId
    }

    suspend fun createMappedRouteAndReport(
        routeCode: String,
        origin: String,
        destination: String,
        transportType: TransportType,
        capacity: CapacityLevel,
        priceMxn: Double,
        stopNames: List<String>,
        gpsTrail: List<GpsPoint>,
        currentLat: Double?,
        currentLng: Double?,
    ): Pair<TransitRoute, String> {
        val now = System.currentTimeMillis()
        val routeId = "ruta_user_$now"
        val reportId = "rep_${now}_${(100..999).random()}"
        val cleanCode = routeCode.trim().ifBlank { "R-${(10..99).random()}" }
        val cleanOrigin = origin.trim().ifBlank { "Punto inicial" }
        val cleanDest = destination.trim().ifBlank { "Destino GPS" }

        val mappedPoints = gpsTrail
            .map { MapBounds.toNormalizedPoint(it.lat, it.lng) }
            .ifEmpty {
                listOf(
                    MapPoint(0.42f, 0.36f, currentLat ?: 19.2925, currentLng ?: -99.6569),
                    MapPoint(0.54f, 0.48f, currentLat ?: 19.2826, currentLng ?: -99.6557),
                    MapPoint(0.68f, 0.62f, currentLat ?: 19.2775, currentLng ?: -99.6445),
                )
            }

        val stopsList = stopNames
            .ifEmpty { listOf(cleanOrigin, cleanDest) }
            .mapIndexed { idx, name ->
                val pt = mappedPoints.getOrElse(idx % mappedPoints.size) { mappedPoints.last() }
                TransitStop(
                    id = "stop_${now}_$idx",
                    name = name,
                    x = pt.x,
                    y = pt.y,
                    lat = pt.lat,
                    lng = pt.lng,
                )
            }

        val newRoute = TransitRoute(
            id = routeId,
            code = cleanCode,
            title = "$cleanOrigin · $cleanDest",
            origin = cleanOrigin,
            destination = cleanDest,
            transportType = transportType,
            transferInfo = "Mapeada por comunidad",
            frequencyMinutes = 9,
            durationMinutes = 22,
            priceMxn = priceMxn,
            capacity = capacity,
            stops = stopsList,
            pathPoints = mappedPoints,
            badgeColorHex = 0xFFEB6E3E,
            isDashed = false,
            highlightTag = "Nueva ruta GPS",
            isSaved = true,
            lastTraveledAt = now,
            recentChangeNote = "Mapeada en tiempo real (${gpsTrail.size} puntos GPS)",
            distanceMeters = 150,
            reportCount = 1,
            lastUpdatedAt = now,
        )

        dao.insertRoute(newRoute.toEntity())
        dao.insertReport(
            RouteReportEntity(
                id = reportId,
                routeId = routeId,
                routeCode = cleanCode,
                routeTitle = newRoute.title,
                capacity = capacity.name,
                priceMxn = priceMxn,
                addedStopsJson = TransitJsonCodec.encodeStrings(stopNames),
                gpsPointsCount = gpsTrail.size,
                latitude = currentLat,
                longitude = currentLng,
                timestamp = now,
            )
        )
        return newRoute to reportId
    }
}
