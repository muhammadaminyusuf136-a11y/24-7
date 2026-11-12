package uz.reja24.app.presentation.schedule

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uz.reja24.app.domain.model.Category
import uz.reja24.app.domain.model.RepeatType
import uz.reja24.app.domain.model.ScheduleEntry
import uz.reja24.app.testutil.FakeReminderScheduler
import uz.reja24.app.testutil.FakeScheduleRepository
import uz.reja24.app.testutil.FakeTimeProvider
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class EntryEditorViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeScheduleRepository
    private lateinit var scheduler: FakeReminderScheduler
    private val today = LocalDate.of(2026, 9, 24)
    private val time = FakeTimeProvider(today.atTime(10, 0))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeScheduleRepository()
        scheduler = FakeReminderScheduler()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(args: Map<String, Any> = emptyMap()) =
        EntryEditorViewModel(SavedStateHandle(args), repository, scheduler, time)

    @Test
    fun `new entry defaults to the requested date`() {
        val date = today.plusDays(3)
        val vm = viewModel(mapOf("dateEpochDay" to date.toEpochDay()))
        assertTrue(vm.state.value.isNew)
        assertEquals(date, vm.state.value.date)
        // Yangi reja sukut bo'yicha har hafta shu kuni takrorlanadi
        assertEquals(RepeatType.WEEKLY, vm.state.value.repeatType)
        assertEquals(setOf(date.dayOfWeek), vm.state.value.daysOfWeek)
        // Sana o'zgarsa — hafta kuni ham unga ergashadi
        vm.onDateChange(date.plusDays(1))
        assertEquals(setOf(date.plusDays(1).dayOfWeek), vm.state.value.daysOfWeek)
    }

    @Test
    fun `blank title is rejected and nothing is saved`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onTitleChange("   ")
        vm.save()
        assertTrue(vm.state.value.titleError)
        assertTrue(repository.entries.value.isEmpty())
        assertEquals(0, scheduler.calls)
    }

    @Test
    fun `same start and end time is rejected instead of becoming a 24 hour plan`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onTitleChange("Dars")
        vm.onStartTimeChange(LocalTime.of(15, 0))
        vm.onEndTimeChange(LocalTime.of(15, 0))
        vm.save()
        assertTrue(vm.state.value.timeError)
        assertTrue(repository.entries.value.isEmpty())
        vm.onEndTimeChange(LocalTime.of(16, 0))
        assertFalse(vm.state.value.timeError)
    }

    @Test
    fun `weekly repeat requires at least one day`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onTitleChange("Sport")
        vm.onRepeatTypeChange(RepeatType.WEEKLY)
        // Haftalik tanlanganda sananing kuni avtomatik belgilanadi
        assertEquals(setOf(today.dayOfWeek), vm.state.value.daysOfWeek)
        vm.onToggleDay(today.dayOfWeek)
        vm.save()
        assertTrue(vm.state.value.daysError)
        assertTrue(repository.entries.value.isEmpty())
    }

    @Test
    fun `valid entry is saved trimmed and reminders are rescheduled`() = runTest(dispatcher) {
        val vm = viewModel()
        val events = mutableListOf<EditorEvent>()
        val job = launch { vm.events.collect { events += it } }

        vm.onTitleChange("  Matematika  ")
        vm.onCategoryChange(Category.STUDY)
        vm.onStartTimeChange(LocalTime.of(13, 0))
        vm.onEndTimeChange(LocalTime.of(14, 30))
        vm.onRepeatTypeChange(RepeatType.WEEKLY)
        vm.onToggleDay(DayOfWeek.MONDAY)
        vm.save()

        val saved = repository.entries.value.single()
        assertEquals("Matematika", saved.title)
        assertEquals(Category.STUDY.defaultColorHex, saved.colorHex)
        assertTrue(DayOfWeek.MONDAY in saved.daysOfWeek)
        assertEquals(1, scheduler.calls)
        assertEquals(listOf<EditorEvent>(EditorEvent.Saved), events)
        job.cancel()
    }

    @Test
    fun `existing entry is loaded for editing`() = runTest(dispatcher) {
        val id = repository.saveEntry(
            ScheduleEntry(title = "Kitob", startTime = LocalTime.of(21, 0), endTime = LocalTime.of(21, 30), category = Category.READING)
        )
        val vm = viewModel(mapOf("entryId" to id))
        val state = vm.state.first { !it.isLoading }
        assertFalse(state.isNew)
        assertEquals("Kitob", state.title)
        assertEquals(Category.READING, state.category)
    }

    @Test
    fun `save failure reports an error and allows retry`() = runTest(dispatcher) {
        repository.failOnSave = true
        val vm = viewModel()
        val events = mutableListOf<EditorEvent>()
        val job = launch { vm.events.collect { events += it } }
        vm.onTitleChange("Ish")
        vm.save()
        assertEquals(listOf<EditorEvent>(EditorEvent.Error), events)
        assertFalse(vm.state.value.isSaving)
        job.cancel()
    }
}
