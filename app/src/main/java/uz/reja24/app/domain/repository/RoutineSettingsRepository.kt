package uz.reja24.app.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.reja24.app.domain.model.RoutineSettings

interface RoutineSettingsRepository {
    /** Sozlamalar hali saqlanmagan bo'lsa — standart qiymatlar qaytadi. */
    fun observeSettings(): Flow<RoutineSettings>
    suspend fun getSettings(): RoutineSettings
    suspend fun saveSettings(settings: RoutineSettings)
}
