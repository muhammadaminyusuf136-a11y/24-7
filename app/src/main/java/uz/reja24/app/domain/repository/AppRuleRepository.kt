package uz.reja24.app.domain.repository

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Kunduzgi nazorat qoidasi: ilova uchun kunlik vaqt chegarasi va ochishdan oldin "o'ylab ko'r" pauzasi.
 * Tungi rejimga aloqasi yo'q — tunda bloklash o'z qoidasi bilan ishlaydi.
 */
data class AppRule(
    val packageName: String,
    val appName: String,
    /** 0 — chegara yo'q. */
    val dailyLimitMinutes: Int,
    val pauseOn: Boolean
) {
    val hasLimit: Boolean get() = dailyLimitMinutes > 0
}

interface AppRuleRepository {
    fun observeRules(): Flow<List<AppRule>>
    suspend fun saveRule(rule: AppRule)
    suspend fun removeRule(packageName: String)

    /** Shu kuni ilovalar necha soniya ochiq turgan: paket → soniya. */
    fun observeUsage(date: LocalDate): Flow<Map<String, Long>>
    suspend fun getUsage(date: LocalDate): Map<String, Long>
    suspend fun setUsage(date: LocalDate, packageName: String, seconds: Long)

    /** Kechagi va undan eski hisoblarni o'chiradi. */
    suspend fun clearUsageBefore(date: LocalDate)
}
