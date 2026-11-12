package uz.reja24.app.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.reja24.app.domain.prayer.PrayerSettings

interface PrayerSettingsRepository {
    val settings: Flow<PrayerSettings>
    suspend fun getSettings(): PrayerSettings
    suspend fun update(transform: (PrayerSettings) -> PrayerSettings)
}
