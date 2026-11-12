package uz.reja24.app.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.reja24.app.domain.model.StreakRecord
import uz.reja24.app.domain.model.StreakSummary
import java.time.LocalDate

interface StreakRepository {
    /** Joriy va eng uzun streak (today — bugungi sana). */
    fun observeSummary(today: LocalDate): Flow<StreakSummary>
    suspend fun getSummary(today: LocalDate): StreakSummary
    suspend fun saveRecord(record: StreakRecord)
}
