package jp.circadianguard.sensor

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle

/**
 * Coarse, low-frequency location source built on the framework LocationManager
 * (no Google Play Services, keeping the app F-Droid compatible).
 *
 * Uses the network provider at a relaxed interval; never runs continuous GPS.
 */
class LocationTracker(
    context: Context,
    private val onLocation: (Location) -> Unit,
) {

    private val locationManager =
        context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) = onLocation(location)

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

        override fun onProviderEnabled(provider: String) = Unit

        override fun onProviderDisabled(provider: String) = Unit
    }

    fun start() {
        try {
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?.let(onLocation)
            @Suppress("DEPRECATION")
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                UPDATE_INTERVAL_MS,
                MIN_DISTANCE_M,
                listener,
            )
        } catch (_: SecurityException) {
            // Coarse location permission not yet granted — start again once it is.
        }
    }

    fun stop() {
        try {
            locationManager.removeUpdates(listener)
        } catch (_: Exception) {
            // Provider was never registered.
        }
    }

    companion object {
        private const val UPDATE_INTERVAL_MS = 60_000L
        private const val MIN_DISTANCE_M = 50f
    }
}
