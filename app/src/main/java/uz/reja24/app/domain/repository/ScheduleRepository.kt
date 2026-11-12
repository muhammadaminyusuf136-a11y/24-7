package uz.reja24.app.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.reja24.app.domain.model.DailyTask
import uz.reja24.app.domain.model.ScheduleEntry
import java.time.LocalDate

interface ScheduleRepository {
    /** Barcha rejalar (faol va nofaol), boshlanish vaqti bo'yicha tartiblangan. */
    fun observeAllEntries(): Flow<List<ScheduleEntry>>

    /** Berilgan kun uchun rejalar + bajarilganlik holati. */
    fun observeTasksForDate(date: LocalDate): Flow<List<DailyTask>>

    /** Kunlar oralig'i uchun rejalar (haftalik / oylik ko'rinish). Kalit — sana. */
    fun observeTasksForRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, List<DailyTask>>>

    suspend fun getTasksForDate(date: LocalDate): List<DailyTask>
    suspend fun getEntry(id: Long): ScheduleEntry?

    /** Yangi reja qo'shadi yoki mavjudini yangilaydi. Reja id sini qaytaradi. */
    suspend fun saveEntry(entry: ScheduleEntry): Long
    suspend fun deleteEntry(id: Long)
    suspend fun setEntryActive(id: Long, isActive: Boolean)
    suspend fun setTaskCompleted(entryId: Long, date: LocalDate, isCompleted: Boolean)
}
