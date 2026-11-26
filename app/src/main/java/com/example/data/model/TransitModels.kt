package com.example.data.model

import kotlin.math.roundToInt

enum class TransportType(val labelEs: String) {
    CAMION("Camión"),
    COMBI("Combi"),
    COMBI_CAMION("Combi + camión"),
    TAXI_COLECTIVO("Taxi colectivo"),
    TROLEBUS("Trolebús"),
}

enum class CapacityLevel(
    val labelEs: String,
    val badgeLabelEs: String,
    val activeBars: Int,
) {
    HAY_LUGAR("Hay lugar", "Muchos asientos", 1),
    CASI_LLENA("Casi llena", "Asientos limitados", 2),
    VA_LLENA("Va llena", "Muy llena", 3),
}

enum class RouteFilter(val labelEs: String) {
    FASTEST("Más rápida"),
    CHEAPEST("Más barata"),
    LEAST_CROWDED("Menos llena"),
}

data class MapPoint(
    val x: Float,
    val y: Float,
    val lat: Double,
    val lng: Double,
)

data class TransitStop(
    val id: String,
    val name: String,
    val x: Float,
    val y: Float,
    val lat: Double,
    val lng: Double,
)

data class TransitRoute(
    val id: String,
    val code: String,
    val title: String,
    val origin: String,
    val destination: String,
    val transportType: TransportType,
    val transferInfo: String,
    val frequencyMinutes: Int,
    val durationMinutes: Int,
    val priceMxn: Double,
    val capacity: CapacityLevel,
    val stops: List<TransitStop>,
    val pathPoints: List<MapPoint>,
    val badgeColorHex: Long,
    val isDashed: Boolean = false,
    val highlightTag: String? = null,
    val isSaved: Boolean = false,
    val lastTraveledAt: Long? = null,
    val recentChangeNote: String? = null,
    val distanceMeters: Int = 350,
    val reportCount: Int = 1,
    val lastUpdatedAt: Long = System.currentTimeMillis(),
) {
    val formattedPrice: String
        get() {
            val rounded = (priceMxn * 100.0).roundToInt() / 100.0
            return String.format("$%.2f MXN", rounded)
        }

    val subtitleLine: String
        get() = "${transportType.labelEs} · $transferInfo"
}

data class RouteReport(
    val id: String,
    val routeId: String,
    val routeCode: String,
    val routeTitle: String,
    val capacity: CapacityLevel,
    val priceMxn: Double,
    val addedStops: List<String>,
    val gpsPointsCount: Int,
    val latitude: Double?,
    val longitude: Double?,
    val timestamp: Long,
)

data class GpsPoint(
    val lat: Double,
    val lng: Double,
    val timestamp: Long = System.currentTimeMillis(),
)

object MapBounds {
    const val MIN_LAT = 19.255
    const val MAX_LAT = 19.315
    const val MIN_LNG = -99.710
    const val MAX_LNG = -99.605

    fun isWithinTolucaViewport(lat: Double, lng: Double): Boolean =
        lat in MIN_LAT..MAX_LAT && lng in MIN_LNG..MAX_LNG

    fun toNormalizedPoint(lat: Double, lng: Double): MapPoint {
        val normX = ((lng - MIN_LNG) / (MAX_LNG - MIN_LNG)).toFloat().coerceIn(0.06f, 0.94f)
        val normY = (1.0 - ((lat - MIN_LAT) / (MAX_LAT - MIN_LAT))).toFloat().coerceIn(0.08f, 0.92f)
        return MapPoint(x = normX, y = normY, lat = lat, lng = lng)
    }
}
