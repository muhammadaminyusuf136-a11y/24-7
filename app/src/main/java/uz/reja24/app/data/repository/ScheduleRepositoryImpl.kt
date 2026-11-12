package uz.reja24.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import uz.reja24.app.data.local.db.dao.ScheduleDao
import uz.reja24.app.data.local.db.dao.TaskCompletionDao
import uz.reja24.app.data.local.db.entity.ScheduleEntryEntity
import uz.reja24.app.data.local.db.entity.TaskCompletionEntity
import uz.reja24.app.data.mapper.toDomain
import uz.reja24.app.data.mapper.toEntity
import uz.reja24.app.domain.model.DailyTask
import uz.reja24.app.domain.model.ScheduleEntry
import uz.reja24.app.domain.repository.ScheduleRepository
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val scheduleDao: ScheduleDao,
    private val completionDao: TaskCompletionDao
) : ScheduleRepository {

    override fun observeAllEntries(): Flow<List<ScheduleEntry>> =
        scheduleDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeTasksForDate(date: LocalDate): Flow<List<DailyTask>> =
        combine(
            scheduleDao.observeActive(),
            completionDao.observeForDate(date)
        ) { entries, completions ->
            buildTasks(entries, completions, date)
        }

    override fun observeTasksForRange(
        from: LocalDate,
        to: LocalDate
    ): Flow<Map<LocalDate, List<DailyTask>>> {
        require(!to.isBefore(from)) { "to ($to) from ($from) dan oldin bo'lishi mumkin emas" }
        require(ChronoUnit.DAYS.between(from, to) <= MAX_RANGE_DAYS) { "Oraliq juda katta" }
        return combine(
            scheduleDao.observeActive(),
            completionDao.observeForRange(from, to)
        ) { entries, completions ->
            val completionsByDate = completions.groupBy { it.date }
            generateSequence(from) { it.plusDays(1) }
                .takeWhile { !it.isAfter(to) }
                .associateWith { date ->
                    buildTasks(entries, completionsByDate[date].orEmpty(), date)
                }
        }
    }

    override suspend fun getTasksForDate(date: LocalDate): List<DailyTask> =
        buildTasks(scheduleDao.getActive(), completionDao.getForDate(date), date)

    override suspend fun getEntry(id: Long): ScheduleEntry? = scheduleDao.getById(id)?.toDomain()

    override suspend fun saveEntry(entry: ScheduleEntry): Long {
        val result = scheduleDao.upsert(entry.toEntity())
        // Upsert yangi qatorda yangi id, yangilashda -1 qaytaradi
        return if (entry.id == 0L) result else entry.id
    }

    override suspend fun deleteEntry(id: Long) = scheduleDao.deleteById(id)

    override suspend fun setEntryActive(id: Long, isActive: Boolean) =
        scheduleDao.setActive(id, isActive)

    override suspend fun setTaskCompleted(entryId: Long, date: LocalDate, isCompleted: Boolean) {
        if (isCompleted) {
            completionDao.insert(TaskCompletionEntity(entryId, date, System.currentTimeMillis()))
        } else {
            completionDao.delete(entryId, date)
        }
    }

    /** Faol rejalardan shu kunga tegishlilarini tanlab, bajarilganlik belgisini qo'shadi. */
    private fun buildTasks(
        entries: List<ScheduleEntryEntity>,
        completions: List<TaskCompletionEntity>,
        date: LocalDate
    ): List<DailyTask> {
        val completedIds = completions.mapTo(HashSet()) { it.entryId }
        return entries.asSequence()
            .map { it.toDomain() }
            .filter { it.occursOn(date) }
            .map { DailyTask(entry = it, date = date, isCompleted = it.id in completedIds) }
            .toList()
    }

    companion object {
        /** Bir so'rovda ko'pi bilan ~1 yil (yillik ko'rinish uchun yetarli). */
        const val MAX_RANGE_DAYS = 366L
    }
}
