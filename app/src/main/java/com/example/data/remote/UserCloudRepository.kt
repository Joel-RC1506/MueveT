package com.example.data.remote

import android.content.Context
import androidx.annotation.Keep
import com.example.R
import com.example.data.model.CapacityLevel
import com.example.data.model.TransitRoute
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Keep
@IgnoreExtraProperties
data class UserProfileRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val city: String = "Toluca",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    @ServerTimestamp val lastLoginAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CloudSavedRoute(
    @DocumentId val id: String = "",
    val routeCode: String = "",
    val routeTitle: String = "",
    val origin: String = "",
    val destination: String = "",
    val transportType: String = "",
    val priceMxn: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CloudUserReport(
    @DocumentId val id: String = "",
    val routeId: String = "",
    val routeCode: String = "",
    val routeTitle: String = "",
    val capacity: String = CapacityLevel.HAY_LUGAR.name,
    val priceMxn: Double = 0.0,
    val addedStops: List<String> = emptyList(),
    val gpsPointsCount: Long = 0L,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

class UserCloudRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("El usuario debe iniciar sesión con Google.")
    }

    suspend fun registerOrUpdateCurrentUser(city: String = "Toluca"): Result<UserProfileRecord?> {
        val current = auth.currentUser
            ?: return Result.failure(IllegalStateException("Sesión no iniciada."))
        val uid = current.uid
        if (!VALID_ID_REGEX.matches(uid)) {
            return Result.failure(IllegalArgumentException("UID de usuario inválido."))
        }
        val docPath = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snapshot = docRef.get().await()

            val safeName = (current.displayName?.trim()?.takeIf { it.isNotBlank() }
                ?: current.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
                ?: "Usuario MueveT").take(80)
            val safeEmail = (current.email?.trim()?.takeIf { it.length >= 3 }
                ?: "usuario@muevet.app").take(120)
            val safePhoto = current.photoUrl?.toString()?.trim()?.takeIf { it.isNotBlank() }?.take(500)
            val safeCity = city.trim().ifBlank { "Toluca" }.take(60)

            if (!snapshot.exists()) {
                val createPayload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "photoUrl" to safePhoto,
                    "city" to safeCity,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "lastLoginAt" to FieldValue.serverTimestamp(),
                ).filterValues { it != null }
                docRef.set(createPayload).await()
            } else {
                val updatePayload = mapOf(
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "photoUrl" to safePhoto,
                    "city" to safeCity,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "lastLoginAt" to FieldValue.serverTimestamp(),
                ).filterValues { it != null }
                docRef.update(updatePayload).await()
            }

            val updatedSnap = docRef.get().await()
            val profile = updatedSnap.toObject(
                UserProfileRecord::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfileRecord?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Sesión cambiada; lectura rechazada." }
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    snap.toObject(
                        UserProfileRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    )
                }
        )
    }

    fun observeSavedRouteIds(userId: String): Flow<Set<String>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Sesión cambiada; lectura rechazada." }
        emitAll(
            db.collection("users").document(uid).collection("saved_routes")
                .snapshots()
                .map { snapshot -> snapshot.documents.map { it.id }.toSet() }
        )
    }

    suspend fun syncSavedRoute(userId: String, route: TransitRoute, save: Boolean): Result<Unit> {
        val sanitizedRouteId = route.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        val docPath = "users/$userId/saved_routes/$sanitizedRouteId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Sesión cambiada; escritura rechazada." }
            require(VALID_ID_REGEX.matches(sanitizedRouteId)) { "ID de ruta inválido." }
            val docRef = db.collection("users").document(uid)
                .collection("saved_routes").document(sanitizedRouteId)

            if (save) {
                val safeCode = route.code.trim().ifBlank { "R" }.take(20)
                val safeTitle = route.title.trim().ifBlank { "Ruta" }.take(100)
                val safeOrigin = route.origin.trim().ifBlank { "Origen" }.take(60)
                val safeDest = route.destination.trim().ifBlank { "Destino" }.take(60)
                val safeType = route.transportType.name.take(30)
                val safePrice = route.priceMxn.coerceIn(0.0, 500.0)

                val existing = docRef.get().await()
                if (!existing.exists()) {
                    docRef.set(
                        mapOf(
                            "routeCode" to safeCode,
                            "routeTitle" to safeTitle,
                            "origin" to safeOrigin,
                            "destination" to safeDest,
                            "transportType" to safeType,
                            "priceMxn" to safePrice,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    ).await()
                } else {
                    docRef.update(
                        mapOf(
                            "routeCode" to safeCode,
                            "routeTitle" to safeTitle,
                            "origin" to safeOrigin,
                            "destination" to safeDest,
                            "transportType" to safeType,
                            "priceMxn" to safePrice,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    ).await()
                }
            } else {
                docRef.delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (save) OperationType.WRITE else OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordCloudReport(
        userId: String,
        reportId: String,
        routeId: String,
        routeCode: String,
        routeTitle: String,
        capacity: CapacityLevel,
        priceMxn: Double,
        addedStops: List<String>,
        gpsPointsCount: Int,
        latitude: Double?,
        longitude: Double?,
    ): Result<Unit> {
        val sanitizedReportId = reportId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        val docPath = "users/$userId/reports/$sanitizedReportId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Sesión cambiada; escritura rechazada." }
            require(VALID_ID_REGEX.matches(sanitizedReportId)) { "ID de reporte inválido." }

            val safeStops = addedStops.map { it.trim().take(60) }.filter { it.isNotBlank() }.take(15)
            val safeLat = latitude?.takeIf { it in -90.0..90.0 }
            val safeLng = longitude?.takeIf { it in -180.0..180.0 }

            val payload = mapOf(
                "routeId" to routeId.trim().ifBlank { "ruta" }.take(64),
                "routeCode" to routeCode.trim().ifBlank { "R" }.take(20),
                "routeTitle" to routeTitle.trim().ifBlank { "Ruta" }.take(100),
                "capacity" to capacity.name,
                "priceMxn" to priceMxn.coerceIn(0.0, 500.0),
                "addedStops" to safeStops,
                "gpsPointsCount" to gpsPointsCount.coerceIn(0, 500).toLong(),
                "latitude" to safeLat,
                "longitude" to safeLng,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ).filterValues { it != null }

            db.collection("users").document(uid)
                .collection("reports").document(sanitizedReportId)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    private companion object {
        val VALID_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]+$")
    }
}
