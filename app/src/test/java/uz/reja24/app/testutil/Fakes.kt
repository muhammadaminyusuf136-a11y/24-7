package uz.reja24.app.testutil

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import uz.reja24.app.core.util.TimeProvider
import uz.reja24.app.domain.model.DailyTask
import uz.reja24.app.domain.model.ScheduleEntry
import uz.reja24.app.domain.repository.ReminderScheduler
import uz.reja24.app.domain.repository.ScheduleRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Testlar uchun qo'lda boshqariladigan vaqt. */
class FakeTimeProvider(var current: LocalDateTime) : TimeProvider {
    override fun now(): LocalDateTime = current
    override fun currentMillis(): Long = current.toInstant(ZoneOffset.UTC).toEpochMilli()
}

class FakeReminderScheduler : ReminderScheduler {
    var calls = 0
    override suspend fun rescheduleAll() {
        calls++
    }
}

/** Xotirada ishlaydigan soddalashtirilgan reja repository'si. */
class FakeScheduleRepository : ScheduleRepository {
    val entries = MutableStateFlow<List<ScheduleEntry>>(emptyList())
    private val completions = MutableStateFlow<Set<Pair<Long, LocalDate>>>(emptySet())
    private var nextId = 1L
    var failOnSave = false

    override fun observeAllEntries(): Flow<List<ScheduleEntry>> = entries

    override fun observeTasksForDate(date: LocalDate): Flow<List<DailyTask>> =
        entries.map { build(it, date) }

    override fun observeTasksForRange(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, List<DailyTask>>> =
        entries.map { list ->
            generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.associateWith { build(list, it) }
        }

    override suspend fun getTasksForDate(date: LocalDate): List<DailyTask> = build(entries.value, date)

    override suspend fun getEntry(id: Long): ScheduleEntry? = entries.value.firstOrNull { it.id == id }

    override suspend fun saveEntry(entry: ScheduleEntry): Long {
        if (failOnSave) throw IllegalStateException("disk full")
        return if (entry.id == 0L) {
            val id = nextId++
            entries.value = entries.value + entry.copy(id = id)
            id
        } else {
            entries.value = entries.value.map { if (it.id == entry.id) entry else it }
            entry.id
        }
    }

    override suspend fun deleteEntry(id: Long) {
        entries.value = entries.value.filterNot { it.id == id }
    }

    override suspend fun setEntryActive(id: Long, isActive: Boolean) {
        entries.value = entries.value.map { if (it.id == id) it.copy(isActive = isActive) else it }
    }

    override suspend fun setTaskCompleted(entryId: Long, date: LocalDate, isCompleted: Boolean) {
        completions.value = if (isCompleted) completions.value + (entryId to date) else completions.value - (entryId to date)
    }

    private fun build(list: List<ScheduleEntry>, date: LocalDate): List<DailyTask> =
        list.filter { it.occursOn(date) }
            .sortedBy { it.startTime }
            .map { DailyTask(it, date, (it.id to date) in completions.value) }
}
