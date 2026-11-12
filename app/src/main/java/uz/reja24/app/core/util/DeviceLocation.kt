package uz.reja24.app.core.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * Taxminiy joylashuv (ACCESS_COARSE_LOCATION — ~1–3 km aniqlik; namoz vaqti uchun yetarli).
 * Joy faqat telefonda ishlatiladi va hech qayerga yuborilmaydi (ilovada INTERNET ruxsati yo'q).
 */
object DeviceLocation {

    private const val CURRENT_FIX_TIMEOUT_MS = 20_000L

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun isLocationEnabled(context: Context): Boolean {
        val manager = context.getSystemService(LocationManager::class.java) ?: return false
        return LocationManagerCompat.isLocationEnabled(manager)
    }

    /** Hozirgi joy; bo'lmasa — so'nggi ma'lum joy; umuman bo'lmasa — null. */
    suspend fun current(context: Context): Location? {
        if (!hasPermission(context)) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val fresh = providers(manager).firstNotNullOfOrNull { provider ->
            withTimeoutOrNull(CURRENT_FIX_TIMEOUT_MS) { requestCurrent(manager, provider) }
        }
        return fresh ?: lastKnown(manager)
    }

    private fun providers(manager: LocationManager): List<String> {
        val candidates = buildList {
            add(LocationManager.NETWORK_PROVIDER)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.GPS_PROVIDER)
        }
        return candidates.filter { provider ->
            try {
                manager.isProviderEnabled(provider)
            } catch (e: IllegalArgumentException) {
                false
            }
        }
    }

    @SuppressLint("MissingPermission") // current() ruxsatni tekshirgandan keyingina chaqiradi
    private suspend fun requestCurrent(manager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { continuation ->
            val signal = CancellationSignal()
            val executor = Executors.newSingleThreadExecutor()
            continuation.invokeOnCancellation {
                signal.cancel()
                executor.shutdown()
            }
            try {
                LocationManagerCompat.getCurrentLocation(manager, provider, signal, executor) { location ->
                    executor.shutdown()
                    if (continuation.isActive) continuation.resume(location)
                }
            } catch (e: SecurityException) {
                // Ruxsat shu orada olib qo'yilgan yoki provayder aniq joy talab qiladi
                executor.shutdown()
                if (continuation.isActive) continuation.resume(null)
            } catch (e: IllegalArgumentException) {
                executor.shutdown()
                if (continuation.isActive) continuation.resume(null)
            }
        }

    @SuppressLint("MissingPermission") // current() ruxsatni tekshirgandan keyingina chaqiradi
    private fun lastKnown(manager: LocationManager): Location? =
        manager.allProviders.mapNotNull { provider ->
            try {
                manager.getLastKnownLocation(provider)
            } catch (e: SecurityException) {
                null
            } catch (e: IllegalArgumentException) {
                null
            }
        }.maxByOrNull { it.time }
}
