package uz.reja24.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uz.reja24.app.data.local.db.dao.AppRuleDao
import uz.reja24.app.data.local.db.entity.AppRuleEntity
import uz.reja24.app.data.local.db.entity.AppUsageEntity
import uz.reja24.app.domain.repository.AppRule
import uz.reja24.app.domain.repository.AppRuleRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRuleRepositoryImpl @Inject constructor(
    private val dao: AppRuleDao
) : AppRuleRepository {

    override fun observeRules(): Flow<List<AppRule>> =
        dao.observeRules().map { list ->
            list.map { AppRule(it.packageName, it.appName, it.dailyLimitMinutes, it.pauseOn) }
        }

    override suspend fun saveRule(rule: AppRule) {
        // Na chegara, na pauza bo'lsa — qoida kerak emas
        if (!rule.hasLimit && !rule.pauseOn) {
            dao.deleteRule(rule.packageName)
        } else {
            dao.upsertRule(AppRuleEntity(rule.packageName, rule.appName, rule.dailyLimitMinutes.coerceAtLeast(0), rule.pauseOn))
        }
    }

    override suspend fun removeRule(packageName: String) = dao.deleteRule(packageName)

    override fun observeUsage(date: LocalDate): Flow<Map<String, Long>> =
        dao.observeUsage(date).map { list -> list.associate { it.packageName to it.seconds } }

    override suspend fun getUsage(date: LocalDate): Map<String, Long> =
        dao.getUsage(date).associate { it.packageName to it.seconds }

    override suspend fun setUsage(date: LocalDate, packageName: String, seconds: Long) =
        dao.upsertUsage(AppUsageEntity(date, packageName, seconds.coerceAtLeast(0)))

    override suspend fun clearUsageBefore(date: LocalDate) = dao.deleteUsageBefore(date)
}
