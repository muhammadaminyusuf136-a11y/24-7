package uz.reja24.app.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.reja24.app.domain.model.DailyScore
import uz.reja24.app.domain.model.WeeklyAward
import java.time.LocalDate

interface ScoreRepository {
    // ---- Kunlik baho ----
    fun observeDailyScore(date: LocalDate): Flow<DailyScore?>
    fun observeDailyScores(from: LocalDate, to: LocalDate): Flow<List<DailyScore>>
    suspend fun getDailyScore(date: LocalDate): DailyScore?
    suspend fun getDailyScores(from: LocalDate, to: LocalDate): List<DailyScore>
    suspend fun saveDailyScore(score: DailyScore)

    /** Favqulodda kirishni qayd qiladi va shu kungi yangi sonni qaytaradi. */
    suspend fun registerBypass(date: LocalDate): Int

    /** Ertalab o'z vaqtida telefon ochildi (uyg'ondi). */
    suspend fun markWokeOnTime(date: LocalDate)

    /** Uyqu vaqtidan keyin telefon ishlatildi. */
    suspend fun markPhoneUsedAfterSleep(date: LocalDate)

    // ---- Haftalik mukofot ----
    fun observeWeeklyAwards(): Flow<List<WeeklyAward>>
    suspend fun getWeeklyAward(weekStartDate: LocalDate): WeeklyAward?
    suspend fun saveWeeklyAward(award: WeeklyAward)
    fun observePerfectWeekCount(): Flow<Int>
    suspend fun getPerfectWeekCount(): Int
}
