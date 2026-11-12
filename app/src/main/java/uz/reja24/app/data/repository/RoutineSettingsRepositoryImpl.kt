package uz.reja24.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import uz.reja24.app.data.local.db.dao.RoutineSettingsDao
import uz.reja24.app.data.mapper.toDomain
import uz.reja24.app.data.mapper.toEntity
import uz.reja24.app.domain.model.RoutineSettings
import uz.reja24.app.domain.repository.RoutineSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutineSettingsRepositoryImpl @Inject constructor(
    private val dao: RoutineSettingsDao
) : RoutineSettingsRepository {

    // Bazada hali yozuv bo'lmasa (birinchi ishga tushish) — standart qiymatlar: 22:00 / 23:00 / 06:00
    override fun observeSettings(): Flow<RoutineSettings> =
        dao.observe()
            .map { it?.toDomain() ?: RoutineSettings() }
            .distinctUntilChanged()

    override suspend fun getSettings(): RoutineSettings = dao.get()?.toDomain() ?: RoutineSettings()

    override suspend fun saveSettings(settings: RoutineSettings) = dao.upsert(settings.toEntity())
}
