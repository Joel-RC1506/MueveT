package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.CapacityLevel
import com.example.data.model.MapPoint
import com.example.data.model.TransitRoute
import com.example.data.model.TransitStop
import com.example.data.model.TransportType

@Database(
    entities = [TransitRouteEntity::class, RouteReportEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(TransitConverters::class)
abstract class MueveTDatabase : RoomDatabase() {
    abstract fun mueveTDao(): MueveTDao

    companion object {
        @Volatile
        private var INSTANCE: MueveTDatabase? = null

        fun getInstance(context: Context): MueveTDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MueveTDatabase::class.java,
                    "muevet_transit.db",
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

object SeedTransitCatalog {
    fun initialRoutes(): List<TransitRoute> {
        val now = System.currentTimeMillis()
        return listOf(
            TransitRoute(
                id = "ruta_76",
                code = "76",
                title = "Centro · Paseo Tollocan · Terminal",
                origin = "Centro",
                destination = "Terminal Toluca",
                transportType = TransportType.CAMION,
                transferInfo = "Sin transbordos",
                frequencyMinutes = 8,
                durationMinutes = 20,
                priceMxn = 12.0,
                capacity = CapacityLevel.HAY_LUGAR,
                stops = listOf(
                    TransitStop("s76_1", "Los Portales · Centro", 0.50f, 0.30f, 19.2925, -99.6569),
                    TransitStop("s76_2", "Av. Juárez", 0.54f, 0.44f, 19.2865, -99.6530),
                    TransitStop("s76_3", "Paseo Tollocan", 0.60f, 0.56f, 19.2802, -99.6478),
                    TransitStop("s76_4", "Terminal Toluca", 0.68f, 0.64f, 19.2775, -99.6445),
                ),
                pathPoints = listOf(
                    MapPoint(0.46f, 0.22f, 19.2960, -99.6600),
                    MapPoint(0.50f, 0.30f, 19.2925, -99.6569),
                    MapPoint(0.54f, 0.44f, 19.2865, -99.6530),
                    MapPoint(0.60f, 0.56f, 19.2802, -99.6478),
                    MapPoint(0.68f, 0.64f, 19.2775, -99.6445),
                ),
                badgeColorHex = 0xFFEB6E3E,
                isDashed = false,
                highlightTag = "La más rápida",
                isSaved = true,
                lastTraveledAt = now - 3_600_000L * 4,
                distanceMeters = 260,
                reportCount = 14,
                lastUpdatedAt = now - 180_000L,
            ),
            TransitRoute(
                id = "ruta_76_12",
                code = "76 + 12",
                title = "Centro · CU · Zinacantepec",
                origin = "Centro",
                destination = "Zinacantepec",
                transportType = TransportType.COMBI_CAMION,
                transferInfo = "1 transbordo",
                frequencyMinutes = 6,
                durationMinutes = 28,
                priceMxn = 14.0,
                capacity = CapacityLevel.CASI_LLENA,
                stops = listOf(
                    TransitStop("s7612_1", "Plaza de los Mártires", 0.52f, 0.32f, 19.2928, -99.6560),
                    TransitStop("s7612_2", "Ciudad Universitaria", 0.34f, 0.48f, 19.2837, -99.6756),
                    TransitStop("s7612_3", "Av. Adolfo López Mateos", 0.22f, 0.54f, 19.2810, -99.6920),
                    TransitStop("s7612_4", "Zinacantepec Centro", 0.12f, 0.60f, 19.2785, -99.7060),
                ),
                pathPoints = listOf(
                    MapPoint(0.52f, 0.32f, 19.2928, -99.6560),
                    MapPoint(0.34f, 0.48f, 19.2837, -99.6756),
                    MapPoint(0.22f, 0.54f, 19.2810, -99.6920),
                    MapPoint(0.12f, 0.60f, 19.2785, -99.7060),
                ),
                badgeColorHex = 0xFF8B6BB1,
                isDashed = true,
                highlightTag = "Menos caminata",
                isSaved = false,
                lastTraveledAt = now - 86_400_000L,
                distanceMeters = 380,
                reportCount = 9,
                lastUpdatedAt = now - 600_000L,
            ),
            TransitRoute(
                id = "ruta_24",
                code = "24",
                title = "Terminal · Alfredo del Mazo · Galerías",
                origin = "Terminal Toluca",
                destination = "Galerías Toluca",
                transportType = TransportType.CAMION,
                transferInfo = "Sin transbordos",
                frequencyMinutes = 10,
                durationMinutes = 25,
                priceMxn = 12.0,
                capacity = CapacityLevel.HAY_LUGAR,
                stops = listOf(
                    TransitStop("s24_1", "Terminal Toluca", 0.66f, 0.64f, 19.2775, -99.6445),
                    TransitStop("s24_2", "Isidro Fabela", 0.64f, 0.46f, 19.2860, -99.6435),
                    TransitStop("s24_3", "Alfredo del Mazo", 0.72f, 0.28f, 19.2965, -99.6360),
                    TransitStop("s24_4", "Galerías Toluca", 0.78f, 0.22f, 19.2995, -99.6315),
                ),
                pathPoints = listOf(
                    MapPoint(0.66f, 0.64f, 19.2775, -99.6445),
                    MapPoint(0.64f, 0.46f, 19.2860, -99.6435),
                    MapPoint(0.72f, 0.28f, 19.2965, -99.6360),
                    MapPoint(0.78f, 0.22f, 19.2995, -99.6315),
                ),
                badgeColorHex = 0xFF527D5A,
                isDashed = false,
                highlightTag = "Más económica",
                isSaved = false,
                lastTraveledAt = null,
                distanceMeters = 410,
                reportCount = 7,
                lastUpdatedAt = now - 900_000L,
            ),
            TransitRoute(
                id = "ruta_t09",
                code = "T-09",
                title = "Los Portales · Tollocan · Metepec",
                origin = "Centro",
                destination = "Metepec",
                transportType = TransportType.TAXI_COLECTIVO,
                transferInfo = "Sin transbordos",
                frequencyMinutes = 5,
                durationMinutes = 18,
                priceMxn = 15.0,
                capacity = CapacityLevel.CASI_LLENA,
                stops = listOf(
                    TransitStop("st09_1", "Los Portales", 0.50f, 0.30f, 19.2925, -99.6569),
                    TransitStop("st09_2", "Av. Pino Suárez", 0.58f, 0.58f, 19.2788, -99.6488),
                    TransitStop("st09_3", "Pilares", 0.78f, 0.74f, 19.2712, -99.6225),
                    TransitStop("st09_4", "Metepec Centro", 0.88f, 0.86f, 19.2575, -99.6065),
                ),
                pathPoints = listOf(
                    MapPoint(0.50f, 0.30f, 19.2925, -99.6569),
                    MapPoint(0.58f, 0.58f, 19.2788, -99.6488),
                    MapPoint(0.78f, 0.74f, 19.2712, -99.6225),
                    MapPoint(0.88f, 0.86f, 19.2575, -99.6065),
                ),
                badgeColorHex = 0xFFD4942A,
                isDashed = false,
                highlightTag = null,
                isSaved = false,
                lastTraveledAt = now - 172_800_000L,
                distanceMeters = 490,
                reportCount = 11,
                lastUpdatedAt = now - 1_200_000L,
            ),
            TransitRoute(
                id = "ruta_c03",
                code = "C-03",
                title = "Ciudad Universitaria · Colón · Terminal",
                origin = "Ciudad Universitaria",
                destination = "Terminal Toluca",
                transportType = TransportType.COMBI,
                transferInfo = "Sin transbordos",
                frequencyMinutes = 4,
                durationMinutes = 16,
                priceMxn = 12.0,
                capacity = CapacityLevel.HAY_LUGAR,
                stops = listOf(
                    TransitStop("sc03_1", "CU UAEM", 0.34f, 0.48f, 19.2837, -99.6756),
                    TransitStop("sc03_2", "Paseo Colón", 0.46f, 0.58f, 19.2798, -99.6602),
                    TransitStop("sc03_3", "Terminal Toluca", 0.68f, 0.64f, 19.2775, -99.6445),
                ),
                pathPoints = listOf(
                    MapPoint(0.34f, 0.48f, 19.2837, -99.6756),
                    MapPoint(0.46f, 0.58f, 19.2798, -99.6602),
                    MapPoint(0.68f, 0.64f, 19.2775, -99.6445),
                ),
                badgeColorHex = 0xFF4A7C59,
                isDashed = false,
                highlightTag = "Recomendada",
                isSaved = false,
                lastTraveledAt = null,
                distanceMeters = 310,
                reportCount = 8,
                lastUpdatedAt = now - 450_000L,
            ),
        )
    }
}
