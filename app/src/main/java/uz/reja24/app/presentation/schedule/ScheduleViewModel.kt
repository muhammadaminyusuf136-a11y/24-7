package uz.reja24.app.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.reja24.app.core.util.TimeProvider
import uz.reja24.app.domain.model.DailyScore
import uz.reja24.app.domain.model.DailyTask
import uz.reja24.app.domain.model.Goal
import uz.reja24.app.domain.model.RoutineSettings
import uz.reja24.app.domain.model.ScheduleEntry
import uz.reja24.app.domain.model.WeekRepeat
import uz.reja24.app.domain.model.ScoreCalculator
import uz.reja24.app.domain.model.WeeklyEvaluator
import uz.reja24.app.domain.prayer.PrayerDay
import uz.reja24.app.domain.prayer.PrayerOverview
import uz.reja24.app.domain.repository.GoalRepository
import uz.reja24.app.domain.repository.PrayerSettingsRepository
import uz.reja24.app.domain.repository.ReminderScheduler
import uz.reja24.app.domain.repository.RoutineSettingsRepository
import uz.reja24.app.domain.repository.ScheduleRepository
import uz.reja24.app.domain.repository.ScoreRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class ScheduleMode { DAY, WEEK, MONTH, YEAR }

/** Haftalik / oylik ko'rinishdagi bitta kun. */
data class DaySummary(
    val date: LocalDate,
    val taskCount: Int,
    val completedCount: Int,
    val score: DailyScore?,
    /** Shu kunning rejalari (haftalik jadval uchun). */
    val tasks: List<DailyTask> = emptyList()
)

sealed interface ScheduleEvent {
    /** Reja bajarildi — motivatsion xabar ko'rsatish uchun. */
    data class TaskCompleted(val pointsGained: Int) : ScheduleEvent
    /** Haftadagi bir martalik rejalar har haftaga aylantirildi. */
    data class WeekRepeated(val count: Int) : ScheduleEvent
    data object Error : ScheduleEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val scoreRepository: ScoreRepository,
    private val goalRepository: GoalRepository,
    private val reminderScheduler: ReminderScheduler,
    routineSettingsRepository: RoutineSettingsRepository,
    prayerSettingsRepository: PrayerSettingsRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    val today: LocalDate get() = timeProvider.today()

    private val _mode = MutableStateFlow(ScheduleMode.DAY)
    val mode: StateFlow<ScheduleMode> = _mode.asStateFlow()

    private val _selectedDate = MutableStateFlow(timeProvider.today())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _month = MutableStateFlow(YearMonth.from(timeProvider.today()))
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    private val _events = MutableSharedFlow<ScheduleEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<ScheduleEvent> = _events.asSharedFlow()

    val settings: StateFlow<RoutineSettings> = routineSettingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutineSettings())

    /** Tanlangan kunning namoz vaqtlari (o'chirilgan yoki joy noma'lum bo'lsa — null). */
    val dayPrayers: StateFlow<PrayerDay?> = combine(prayerSettingsRepository.settings, _selectedDate) { s, date ->
        PrayerOverview.build(s, timeProvider.zone(), listOf(date)).days[date]
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val dayTasks: StateFlow<List<DailyTask>> = _selectedDate
        .flatMapLatest { scheduleRepository.observeTasksForDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val weekDays: StateFlow<List<DaySummary>> = _selectedDate
        .map { WeeklyEvaluator.weekStartOf(it) }
        .flatMapLatest { start -> summaries(start, start.plusDays(6)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Ko'rilayotgan haftadagi "bir marta" rejalar — "har haftaga takrorlash" tugmasi uchun. */
    val weekOnceEntries: StateFlow<List<ScheduleEntry>> = weekDays
        .map { days -> WeekRepeat.onceEntries(days.flatMap { it.tasks }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Haftadagi bir martalik rejalarni har hafta o'z kunida takrorlanadigan qiladi.
     * Keyinroq qo'lda qayta kiritilgan aynan bir xil nusxalar (nom, vaqt, kun bir xil) o'chiriladi —
     * aks holda o'sha kunlarda reja ikki marta chiqardi.
     */
    fun repeatWeekEveryWeek() {
        val once = weekOnceEntries.value
        if (once.isEmpty()) return
        viewModelScope.launch {
            try {
                val all = scheduleRepository.observeAllEntries().first()
                val plan = WeekRepeat.plan(once, all)
                plan.converted.forEach { scheduleRepository.saveEntry(it) }
                plan.duplicatesToDelete.forEach { scheduleRepository.deleteEntry(it) }
                reminderScheduler.rescheduleAll()
                _events.emit(ScheduleEvent.WeekRepeated(plan.converted.size))
            } catch (e: Exception) {
                _events.emit(ScheduleEvent.Error)
            }
        }
    }

    val monthDays: StateFlow<List<DaySummary>> = _month
        .flatMapLatest { ym -> summaries(ym.atDay(1), ym.atEndOfMonth()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val goals: StateFlow<List<Goal>> = goalRepository.observeGoals(timeProvider.today().year)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun summaries(from: LocalDate, to: LocalDate) = combine(
        scheduleRepository.observeTasksForRange(from, to),
        scoreRepository.observeDailyScores(from, to)
    ) { tasksByDate, scores ->
        val scoreByDate = scores.associateBy { it.date }
        tasksByDate.entries.sortedBy { it.key }.map { (date, tasks) ->
            DaySummary(
                date = date,
                taskCount = tasks.size,
                completedCount = tasks.count { it.isCompleted },
                score = scoreByDate[date],
                tasks = tasks
            )
        }
    }

    fun setMode(mode: ScheduleMode) {
        _mode.value = mode
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun showMonth(month: YearMonth) {
        _month.value = month
    }

    /** Oylik kalendarda kunni bosish — o'sha kunning rejasiga o'tadi. */
    fun openDay(date: LocalDate) {
        _selectedDate.value = date
        _mode.value = ScheduleMode.DAY
    }

    fun toggleCompleted(task: DailyTask) {
        viewModelScope.launch {
            try {
                val completing = !task.isCompleted
                val tasks = dayTasks.value
                val completedBefore = tasks.count { it.isCompleted }
                scheduleRepository.setTaskCompleted(task.entry.id, task.date, completing)
                if (completing) {
                    val gained = ScoreCalculator.taskPoints(completedBefore + 1, tasks.size) -
                        ScoreCalculator.taskPoints(completedBefore, tasks.size)
                    _events.emit(ScheduleEvent.TaskCompleted(gained.coerceAtLeast(0)))
                }
                reminderScheduler.rescheduleAll()
            } catch (e: Exception) {
                _events.emit(ScheduleEvent.Error)
            }
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            try {
                scheduleRepository.deleteEntry(id)
                reminderScheduler.rescheduleAll()
            } catch (e: Exception) {
                _events.emit(ScheduleEvent.Error)
            }
        }
    }

    // ---------- Yillik maqsadlar ----------

    fun saveGoal(goal: Goal) {
        viewModelScope.launch {
            try {
                goalRepository.saveGoal(goal.copy(year = if (goal.year == 0) today.year else goal.year))
            } catch (e: Exception) {
                _events.emit(ScheduleEvent.Error)
            }
        }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch {
            try {
                goalRepository.deleteGoal(id)
            } catch (e: Exception) {
                _events.emit(ScheduleEvent.Error)
            }
        }
    }
}
