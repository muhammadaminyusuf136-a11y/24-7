package uz.reja24.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uz.reja24.app.data.local.db.dao.ScheduleDao
import uz.reja24.app.data.local.db.dao.TaskCompletionDao
import uz.reja24.app.data.local.db.entity.ScheduleEntryEntity
import uz.reja24.app.data.local.db.entity.TaskCompletionEntity
import uz.reja24.app.domain.model.Category
import uz.reja24.app.domain.model.RepeatType
import uz.reja24.app.domain.model.ScheduleEntry
import java.time.LocalDate
import java.time.LocalTime

/** Repository mantig'ini haqiqiy bazasiz, xotiradagi "soxta" DAO'lar bilan tekshiramiz. */
class ScheduleRepositoryImplTest {

    private lateinit var scheduleDao: FakeScheduleDao
    private lateinit var completionDao: FakeTaskCompletionDao
    private lateinit var repository: ScheduleRepositoryImpl

    private val monday = LocalDate.of(2026, 9, 21)

    @Before
    fun setUp() {
        scheduleDao = FakeScheduleDao()
        completionDao = FakeTaskCompletionDao()
        repository = ScheduleRepositoryImpl(scheduleDao, completionDao)
    }

    private fun entry(title: String, repeat: RepeatType, date: LocalDate? = null, hour: Int = 9) =
        ScheduleEntry(
            title = title,
            startTime = LocalTime.of(hour, 0),
            endTime = LocalTime.of(hour + 1, 0),
            category = Category.STUDY,
            repeatType = repeat,
            specificDate = date
        )

    @Test
    fun `save new entry returns generated id and update keeps id`() = runTest {
        val id = repository.saveEntry(entry("Dars", RepeatType.DAILY))
        assertEquals(1L, id)

        val updatedId = repository.saveEntry(repository.getEntry(id)!!.copy(title = "  Matematika  "))
        assertEquals(id, updatedId)
        assertEquals("Matematika", repository.getEntry(id)!!.title) // bo'sh joylar olib tashlanadi
    }

    @Test
    fun `tasks for date include only occurring entries sorted by time`() = runTest {
        repository.saveEntry(entry("Kechki", RepeatType.DAILY, hour = 18))
        repository.saveEntry(entry("Ertalab", RepeatType.DAILY, hour = 7))
        repository.saveEntry(entry("Ertaga", RepeatType.ONCE, date = monday.plusDays(1)))

        val tasks = repository.observeTasksForDate(monday).first()
        assertEquals(listOf("Ertalab", "Kechki"), tasks.map { it.entry.title })
    }

    @Test
    fun `completion is per day for repeating entries`() = runTest {
        val id = repository.saveEntry(entry("Sport", RepeatType.DAILY))
        repository.setTaskCompleted(id, monday, true)

        assertTrue(repository.getTasksForDate(monday).single().isCompleted)
        assertFalse(repository.getTasksForDate(monday.plusDays(1)).single().isCompleted)

        repository.setTaskCompleted(id, monday, false)
        assertFalse(repository.getTasksForDate(monday).single().isCompleted)
    }

    @Test
    fun `inactive entry is hidden from daily tasks`() = runTest {
        val id = repository.saveEntry(entry("Mutolaa", RepeatType.DAILY))
        repository.setEntryActive(id, false)
        assertTrue(repository.getTasksForDate(monday).isEmpty())
        assertEquals(1, repository.observeAllEntries().first().size)
    }

    @Test
    fun `range returns every day in interval`() = runTest {
        repository.saveEntry(entry("Ish", RepeatType.WEEKDAYS))
        val week = repository.observeTasksForRange(monday, monday.plusDays(6)).first()
        assertEquals(7, week.size)
        assertEquals(1, week[monday]!!.size)
        assertEquals(0, week[monday.plusDays(5)]!!.size) // shanba
    }

    @Test
    fun `delete removes entry`() = runTest {
        val id = repository.saveEntry(entry("Oila", RepeatType.DAILY))
        repository.deleteEntry(id)
        assertNull(repository.getEntry(id))
    }

    // ---------------- Soxta DAO'lar ----------------

    private class FakeScheduleDao : ScheduleDao {
        private val entries = MutableStateFlow<List<ScheduleEntryEntity>>(emptyList())
        private var nextId = 1L

        private fun sorted(list: List<ScheduleEntryEntity>) =
            list.sortedWith(compareBy({ it.startTime }, { it.id }))

        override fun observeAll(): Flow<List<ScheduleEntryEntity>> = entries.map(::sorted)
        override fun observeActive(): Flow<List<ScheduleEntryEntity>> =
            entries.map { list -> sorted(list.filter { it.isActive }) }
        override suspend fun getActive(): List<ScheduleEntryEntity> =
            sorted(entries.value.filter { it.isActive })
        override suspend fun getById(id: Long) = entries.value.firstOrNull { it.id == id }

        override suspend fun upsert(entry: ScheduleEntryEntity): Long {
            return if (entry.id == 0L) {
                val id = nextId++
                entries.value = entries.value + entry.copy(id = id)
                id
            } else {
                entries.value = entries.value.map { if (it.id == entry.id) entry else it }
                -1L
            }
        }

        override suspend fun deleteById(id: Long) {
            entries.value = entries.value.filterNot { it.id == id }
        }

        override suspend fun setActive(id: Long, isActive: Boolean) {
            entries.value = entries.value.map { if (it.id == id) it.copy(isActive = isActive) else it }
        }
    }

    private class FakeTaskCompletionDao : TaskCompletionDao {
        private val completions = MutableStateFlow<List<TaskCompletionEntity>>(emptyList())

        override fun observeForDate(date: LocalDate) =
            completions.map { list -> list.filter { it.date == date } }
        override fun observeForRange(from: LocalDate, to: LocalDate) =
            completions.map { list -> list.filter { !it.date.isBefore(from) && !it.date.isAfter(to) } }
        override suspend fun getForDate(date: LocalDate) = completions.value.filter { it.date == date }

        override suspend fun insert(completion: TaskCompletionEntity) {
            completions.value = completions.value.filterNot {
                it.entryId == completion.entryId && it.date == completion.date
            } + completion
        }

        override suspend fun delete(entryId: Long, date: LocalDate) {
            completions.value = completions.value.filterNot { it.entryId == entryId && it.date == date }
        }
    }
}
