package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.data.model.CapacityLevel
import com.example.data.model.MapPoint
import com.example.data.model.RouteReport
import com.example.data.model.TransitRoute
import com.example.data.model.TransitStop
import com.example.data.model.TransportType
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "transit_routes")
data class TransitRouteEntity(
    @PrimaryKey val id: String,
    val code: String,
    val title: String,
    val origin: String,
    val destination: String,
    val transportType: String,
    val transferInfo: String,
    val frequencyMinutes: Int,
    val durationMinutes: Int,
    val priceMxn: Double,
    val capacity: String,
    val stopsJson: String,
    val pathPointsJson: String,
    val badgeColorHex: Long,
    val isDashed: Boolean,
    val highlightTag: String?,
    val isSaved: Boolean,
    val lastTraveledAt: Long?,
    val recentChangeNote: String?,
    val distanceMeters: Int,
    val reportCount: Int,
    val lastUpdatedAt: Long,
)

@Entity(tableName = "route_reports")
data class RouteReportEntity(
    @PrimaryKey val id: String,
    val routeId: String,
    val routeCode: String,
    val routeTitle: String,
    val capacity: String,
    val priceMxn: Double,
    val addedStopsJson: String,
    val gpsPointsCount: Int,
    val latitude: Double?,
    val longitude: Double?,
    val timestamp: Long,
)

object TransitJsonCodec {
    fun encodeStops(stops: List<TransitStop>): String {
        val arr = JSONArray()
        stops.forEach { stop ->
            arr.put(
                JSONObject().apply {
                    put("id", stop.id)
                    put("name", stop.name)
                    put("x", stop.x.toDouble())
                    put("y", stop.y.toDouble())
                    put("lat", stop.lat)
                    put("lng", stop.lng)
                }
            )
        }
        return arr.toString()
    }

    fun decodeStops(json: String): List<TransitStop> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            List(arr.length()) { idx ->
                val obj = arr.getJSONObject(idx)
                TransitStop(
                    id = obj.optString("id", "stop_$idx"),
                    name = obj.optString("name", "Parada ${idx + 1}"),
                    x = obj.optDouble("x", 0.5).toFloat(),
                    y = obj.optDouble("y", 0.5).toFloat(),
                    lat = obj.optDouble("lat", 19.2826),
                    lng = obj.optDouble("lng", -99.6557),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun encodePathPoints(points: List<MapPoint>): String {
        val arr = JSONArray()
        points.forEach { pt ->
            arr.put(
                JSONObject().apply {
                    put("x", pt.x.toDouble())
                    put("y", pt.y.toDouble())
                    put("lat", pt.lat)
                    put("lng", pt.lng)
                }
            )
        }
        return arr.toString()
    }

    fun decodePathPoints(json: String): List<MapPoint> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            List(arr.length()) { idx ->
                val obj = arr.getJSONObject(idx)
                MapPoint(
                    x = obj.optDouble("x", 0.5).toFloat(),
                    y = obj.optDouble("y", 0.5).toFloat(),
                    lat = obj.optDouble("lat", 19.2826),
                    lng = obj.optDouble("lng", -99.6557),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun encodeStrings(items: List<String>): String {
        val arr = JSONArray()
        items.forEach { arr.put(it) }
        return arr.toString()
    }

    fun decodeStrings(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            List(arr.length()) { idx -> arr.optString(idx) }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }
}

class TransitConverters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = TransitJsonCodec.encodeStrings(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = TransitJsonCodec.decodeStrings(value)
}

fun TransitRouteEntity.toDomain(): TransitRoute = TransitRoute(
    id = id,
    code = code,
    title = title,
    origin = origin,
    destination = destination,
    transportType = TransportType.entries.firstOrNull { it.name == transportType } ?: TransportType.CAMION,
    transferInfo = transferInfo,
    frequencyMinutes = frequencyMinutes,
    durationMinutes = durationMinutes,
    priceMxn = priceMxn,
    capacity = CapacityLevel.entries.firstOrNull { it.name == capacity } ?: CapacityLevel.HAY_LUGAR,
    stops = TransitJsonCodec.decodeStops(stopsJson),
    pathPoints = TransitJsonCodec.decodePathPoints(pathPointsJson),
    badgeColorHex = badgeColorHex,
    isDashed = isDashed,
    highlightTag = highlightTag,
    isSaved = isSaved,
    lastTraveledAt = lastTraveledAt,
    recentChangeNote = recentChangeNote,
    distanceMeters = distanceMeters,
    reportCount = reportCount,
    lastUpdatedAt = lastUpdatedAt,
)

fun TransitRoute.toEntity(): TransitRouteEntity = TransitRouteEntity(
    id = id,
    code = code,
    title = title,
    origin = origin,
    destination = destination,
    transportType = transportType.name,
    transferInfo = transferInfo,
    frequencyMinutes = frequencyMinutes,
    durationMinutes = durationMinutes,
    priceMxn = priceMxn,
    capacity = capacity.name,
    stopsJson = TransitJsonCodec.encodeStops(stops),
    pathPointsJson = TransitJsonCodec.encodePathPoints(pathPoints),
    badgeColorHex = badgeColorHex,
    isDashed = isDashed,
    highlightTag = highlightTag,
    isSaved = isSaved,
    lastTraveledAt = lastTraveledAt,
    recentChangeNote = recentChangeNote,
    distanceMeters = distanceMeters,
    reportCount = reportCount,
    lastUpdatedAt = lastUpdatedAt,
)

fun RouteReportEntity.toDomain(): RouteReport = RouteReport(
    id = id,
    routeId = routeId,
    routeCode = routeCode,
    routeTitle = routeTitle,
    capacity = CapacityLevel.entries.firstOrNull { it.name == capacity } ?: CapacityLevel.HAY_LUGAR,
    priceMxn = priceMxn,
    addedStops = TransitJsonCodec.decodeStrings(addedStopsJson),
    gpsPointsCount = gpsPointsCount,
    latitude = latitude,
    longitude = longitude,
    timestamp = timestamp,
)
