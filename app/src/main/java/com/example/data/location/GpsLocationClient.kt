package com.example.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.data.model.GpsPoint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class GpsLocationClient(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownOrCurrentLocation(): GpsPoint? {
        if (!hasLocationPermission()) return null
        return suspendCancellableCoroutine { continuation ->
            fusedClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        continuation.resume(GpsPoint(location.latitude, location.longitude))
                    } else {
                        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                            .addOnSuccessListener { fresh ->
                                continuation.resume(fresh?.let { GpsPoint(it.latitude, it.longitude) })
                            }
                            .addOnFailureListener { continuation.resume(null) }
                    }
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        }
    }

    @SuppressLint("MissingPermission")
    fun observeLocationUpdates(intervalMillis: Long = 2500L): Flow<GpsPoint> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateDistanceMeters(2f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    trySend(GpsPoint(loc.latitude, loc.longitude, System.currentTimeMillis()))
                }
            }
        }

        try {
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (_: SecurityException) {
            close()
        } catch (_: Exception) {
            close()
        }

        awaitClose {
            fusedClient.removeLocationUpdates(callback)
        }
    }

    data class PlaceResolution(
        val shortLabel: String,
        val detailedAddress: String,
    )

    suspend fun resolvePlaceLabel(lat: Double, lng: Double): String? {
        return resolvePlaceDetails(lat, lng)?.shortLabel
    }

    suspend fun resolvePlaceDetails(lat: Double, lng: Double): PlaceResolution? {
        val coordFallback = formatCoordinateLabel(lat, lng)
        if (!Geocoder.isPresent()) {
            return PlaceResolution(shortLabel = "GPS en vivo", detailedAddress = coordFallback)
        }
        val locale = Locale.Builder().setLanguage("es").setRegion("MX").build()
        val geocoder = Geocoder(context, locale)
        val address: Address? = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) {
                                continuation.resume(addresses.firstOrNull())
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) {
                                continuation.resume(null)
                            }
                        }
                    })
                }
            } else {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()
                }
            }
        }.getOrNull()

        return formatPlaceResolution(address, coordFallback)
    }

    private fun formatCoordinateLabel(lat: Double, lng: Double): String {
        return String.format(Locale.US, "GPS: %.4f, %.4f", lat, lng)
    }

    private fun formatPlaceResolution(address: Address?, coordFallback: String): PlaceResolution? {
        if (address == null) {
            return PlaceResolution(shortLabel = "GPS en vivo", detailedAddress = coordFallback)
        }
        val city = address.locality?.takeIf { it.isNotBlank() }
            ?: address.subAdminArea?.takeIf { it.isNotBlank() }
            ?: address.adminArea?.takeIf { it.isNotBlank() }
            ?: address.countryName?.takeIf { it.isNotBlank() }
        val neighborhood = address.subLocality?.takeIf {
            it.isNotBlank() && !it.equals(city, ignoreCase = true)
        }
        val shortLabel = when {
            city != null && neighborhood != null -> {
                val combined = "$city · $neighborhood"
                if (combined.length > 30) city.take(30) else combined
            }
            city != null -> city.take(30)
            else -> "GPS en vivo"
        }

        val streetNumber = address.subThoroughfare?.takeIf { it.isNotBlank() }
        val streetName = address.thoroughfare?.takeIf { it.isNotBlank() }
        val street = listOfNotNull(streetName, streetNumber).joinToString(" ").takeIf { it.isNotBlank() }
        val parts = listOfNotNull(street, neighborhood, city).distinct()
        val detailed = when {
            parts.isNotEmpty() -> parts.joinToString(", ")
            !address.getAddressLine(0).isNullOrBlank() -> address.getAddressLine(0)
            else -> coordFallback
        }

        return PlaceResolution(
            shortLabel = shortLabel,
            detailedAddress = detailed,
        )
    }
}
