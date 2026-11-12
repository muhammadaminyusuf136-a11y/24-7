package uz.reja24.app.data.place

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import uz.reja24.app.domain.model.AppLanguage
import uz.reja24.app.domain.prayer.PlaceRecord
import uz.reja24.app.domain.prayer.PlaceResult
import uz.reja24.app.domain.prayer.PlaceSearch
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Shahar qidiruvi:
 *  - internetsiz — ilova ichidagi GeoNames bazasi (assets/cities.tsv, ~34 000 shahar);
 *  - onlayn — faqat foydalanuvchi "Onlayn qidirish"ni bosganda, telefonning o'z joy xizmati
 *    (Android Geocoder) orqali. Ilovaning o'zida INTERNET ruxsati yo'q — so'rovni tizim yuboradi.
 */
@Singleton
class PlaceSearchRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val mutex = Mutex()
    private var records: List<PlaceRecord>? = null

    sealed interface OnlineResult {
        data class Found(val places: List<PlaceResult>) : OnlineResult
        data object NotFound : OnlineResult
        data object Unavailable : OnlineResult
    }

    suspend fun searchOffline(query: String, language: AppLanguage): List<PlaceResult> =
        withContext(Dispatchers.Default) {
            PlaceSearch.search(query, language, loadRecords())
        }

    /** Birinchi qidiruvda bir marta o'qiladi va xotirada qoladi. */
    private suspend fun loadRecords(): List<PlaceRecord> = mutex.withLock {
        records ?: withContext(Dispatchers.IO) {
            try {
                context.assets.open(ASSET).bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.mapNotNull(PlaceRecord::parse).toList()
                }
            } catch (e: IOException) {
                emptyList()
            }
        }.also { records = it }
    }

    suspend fun searchOnline(query: String, locale: Locale): OnlineResult {
        if (!Geocoder.isPresent()) return OnlineResult.Unavailable
        val geocoder = Geocoder(context, locale)
        val addresses: List<Address>? = withTimeoutOrNull(ONLINE_TIMEOUT_MS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocationName(query, MAX_ONLINE, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) continuation.resume(addresses)
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) continuation.resume(null)
                        }
                    })
                }
            } else {
                withContext(Dispatchers.IO) {
                    try {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocationName(query, MAX_ONLINE)
                    } catch (e: IOException) {
                        null
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }
            }
        }
        val places = addresses.orEmpty()
            .filter { it.hasLatitude() && it.hasLongitude() }
            .map { address ->
                PlaceResult(
                    name = listOfNotNull(address.locality, address.subAdminArea, address.adminArea, address.featureName)
                        .firstOrNull { it.isNotBlank() } ?: query.trim(),
                    latitude = address.latitude,
                    longitude = address.longitude,
                    countryCode = address.countryCode
                )
            }
            .distinctBy { "${it.name}|${it.countryCode}" }
        return when {
            addresses == null -> OnlineResult.NotFound
            places.isEmpty() -> OnlineResult.NotFound
            else -> OnlineResult.Found(places)
        }
    }

    private companion object {
        const val ASSET = "cities.tsv"
        const val MAX_ONLINE = 8
        const val ONLINE_TIMEOUT_MS = 15_000L
    }
}
