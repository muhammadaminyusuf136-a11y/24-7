package uz.reja24.app.service.prayer

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uz.reja24.app.core.util.DeviceLocation
import uz.reja24.app.core.util.TimeProvider
import uz.reja24.app.domain.prayer.Cities
import uz.reja24.app.domain.prayer.DetectedLocation
import uz.reja24.app.domain.repository.PrayerSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Avtomatik rejimda joyni yangilaydi (ilova ochilganda va "Joyni yangilash" bosilganda).
 * Fonda joy olinmaydi — buning uchun alohida "fonda joylashuv" ruxsati kerak bo'lardi.
 * Safarda telefon vaqt zonasi o'zgarsa, vaqtlar baribir yangi zona shahri bo'yicha hisoblanadi.
 */
@Singleton
class PrayerLocationUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prayerSettingsRepository: PrayerSettingsRepository,
    private val timeProvider: TimeProvider
) {
    private val mutex = Mutex()

    enum class Result { UPDATED, UNCHANGED, NO_PERMISSION, LOCATION_OFF, NOT_FOUND, MANUAL_MODE }

    suspend fun refresh(): Result = mutex.withLock {
        val settings = prayerSettingsRepository.getSettings()
        if (!settings.autoLocation) return Result.MANUAL_MODE
        if (!DeviceLocation.hasPermission(context)) return Result.NO_PERMISSION
        if (!DeviceLocation.isLocationEnabled(context)) return Result.LOCATION_OFF
        val location = DeviceLocation.current(context) ?: return Result.NOT_FOUND

        val previous = settings.detected
        val movedKm = previous?.let {
            Cities.distanceKm(it.latitude, it.longitude, location.latitude, location.longitude)
        }
        if (movedKm != null && movedKm < MIN_MOVE_KM) return Result.UNCHANGED

        prayerSettingsRepository.update {
            it.copy(detected = DetectedLocation(location.latitude, location.longitude, timeProvider.currentMillis()))
        }
        Result.UPDATED
    }

    private companion object {
        /** Bundan kam siljish namoz vaqtini sezilarli o'zgartirmaydi (1 km ≈ 4–5 soniya). */
        const val MIN_MOVE_KM = 2.0
    }
}
