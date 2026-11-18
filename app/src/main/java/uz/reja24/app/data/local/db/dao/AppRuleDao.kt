package uz.reja24.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import uz.reja24.app.data.local.db.entity.AppRuleEntity
import uz.reja24.app.data.local.db.entity.AppUsageEntity
import java.time.LocalDate

/** Kunduzgi nazorat: ilova qoidalari (kunlik chegara, pauza) va bugungi foydalanish vaqti. */
@Dao
interface AppRuleDao {

    @Query("SELECT * FROM app_rules ORDER BY appName COLLATE NOCASE")
    fun observeRules(): Flow<List<AppRuleEntity>>

    @Upsert
    suspend fun upsertRule(rule: AppRuleEntity)

    @Query("DELETE FROM app_rules WHERE packageName = :packageName")
    suspend fun deleteRule(packageName: String)

    @Query("SELECT * FROM app_usage WHERE date = :date")
    fun observeUsage(date: LocalDate): Flow<List<AppUsageEntity>>

    @Query("SELECT * FROM app_usage WHERE date = :date")
    suspend fun getUsage(date: LocalDate): List<AppUsageEntity>

    @Upsert
    suspend fun upsertUsage(usage: AppUsageEntity)

    /** Eski kunlar kerak emas — bazani shishirmasin. */
    @Query("DELETE FROM app_usage WHERE date < :before")
    suspend fun deleteUsageBefore(before: LocalDate)
}
