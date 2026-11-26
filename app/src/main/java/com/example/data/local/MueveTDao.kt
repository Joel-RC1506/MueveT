package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MueveTDao {
    @Query("SELECT * FROM transit_routes ORDER BY distanceMeters ASC")
    fun observeAllRoutes(): Flow<List<TransitRouteEntity>>

    @Query("SELECT * FROM transit_routes WHERE isSaved = 1 ORDER BY lastUpdatedAt DESC")
    fun observeSavedRoutes(): Flow<List<TransitRouteEntity>>

    @Query("SELECT * FROM route_reports ORDER BY timestamp DESC LIMIT 25")
    fun observeRecentReports(): Flow<List<RouteReportEntity>>

    @Query("SELECT * FROM route_reports WHERE routeId = :routeId ORDER BY timestamp DESC")
    fun observeReportsForRoute(routeId: String): Flow<List<RouteReportEntity>>

    @Query("SELECT COUNT(*) FROM transit_routes")
    suspend fun getRouteCount(): Int

    @Query("SELECT * FROM transit_routes WHERE id = :routeId LIMIT 1")
    suspend fun getRouteById(routeId: String): TransitRouteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutes(routes: List<TransitRouteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: TransitRouteEntity)

    @Update
    suspend fun updateRoute(route: TransitRouteEntity)

    @Query("UPDATE transit_routes SET isSaved = :saved WHERE id = :routeId")
    suspend fun setRouteSaved(routeId: String, saved: Boolean)

    @Query("UPDATE transit_routes SET lastTraveledAt = :timestamp WHERE id = :routeId")
    suspend fun markRouteTraveled(routeId: String, timestamp: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: RouteReportEntity)
}
