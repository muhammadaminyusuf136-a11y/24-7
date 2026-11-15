package uz.reja24.app.presentation.muhosaba

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.reja24.app.core.util.TimeProvider
import uz.reja24.app.domain.model.DailyScore
import uz.reja24.app.domain.model.DailyTask
import uz.reja24.app.domain.repository.MuhosabaItem
import uz.reja24.app.domain.repository.MuhosabaRepository
import uz.reja24.app.domain.repository.ReminderScheduler
import uz.reja24.app.domain.repository.RoutineSettingsRepository
import uz.reja24.app.domain.repository.ScheduleRepository
import uz.reja24.app.domain.usecase.ObserveDayScoreUseCase
import java.time.LocalDate
import javax.inject.Inject

/** Bir bandning davr (hafta/oy) bo'yicha xulosasi. */
data class ItemSummary(val item: MuhosabaItem, val done: Int, val missed: Int, val days: Int)

/** Kun tartibidagi reja va uning shu kundagi belgisi: true — ✓, false — ✗, null — belgilanmagan. */
data class PlanMark(val task: DailyTask, val mark: Boolean?)

/** Rejaning davr bo'yicha xulosasi: necha kun rejada bo'lgan, nechtasida ✓, nechtasida ✗. */
data class PlanSummary(
    val entryId: Long,
    val title: String,
    val colorHex: String,
    val done: Int,
    val missed: Int,
    val scheduled: Int
)

/** "Daftar" — bitta kunning yakuni (kun tartibi + o'z ro'yxati birga). */
data class DayLedger(val date: LocalDate, val done: Int, val missed: Int, val total: Int)

data class MuhosabaUiState(
    /** Hisob qilinayotgan kun ("rejim kuni": yarim tundan keyin ham uyg'onishgacha — kechagi kun). */
    val date: LocalDate,
    val isToday: Boolean,
    /** Shu kunning kun tartibi (Reja bo'limidagi rejalar) va belgilari. */
    val plans: List<PlanMark>,
    val items: List<MuhosabaItem>,
    /** Shu kunning belgilari: band id → bajarildimi. */
    val marks: Map<Long, Boolean>,
    /** Ilova o'zi biladigan narsalar: rejim, favqulodda kirish. */
    val facts: DailyScore,
    val week: List<ItemSummary>,
    val month: List<ItemSummary>,
    val planWeek: List<PlanSummary>,
    val planMonth: List<PlanSummary>,
    /** Oxirgi 30 kun, eskisi birinchi. */
    val ledger: List<DayLedger>
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MuhosabaViewModel @Inject constructor(
    private val repository: MuhosabaRepository,
    private val scheduleRepository: ScheduleRepository,
    private val routineSettingsRepository: RoutineSettingsRepository,
    private val reminderScheduler: ReminderScheduler,
    observeDayScore: ObserveDayScoreUseCase,
    private val timeProvider: TimeProvider
) : ViewModel() {

    /** null — "bugungi" rejim kuni; aks holda foydalanuvchi tanlagan o'tgan kun. */
    private val picked = MutableStateFlow<LocalDate?>(null)

    private suspend fun today(): LocalDate =
        routineSettingsRepository.observeSettings().first().routineDayFor(timeProvider.now())

    private val date = combine(picked, routineSettingsRepository.observeSettings()) { p, settings ->
        val today = settings.routineDayFor(timeProvider.now())
        (p ?: today) to today
    }

    val uiState: StateFlow<MuhosabaUiState?> = date.flatMapLatest { (day, today) ->
        val from = today.minusDays(MONTH_DAYS - 1L)
        combine(
            repository.observeItems(),
            repository.observeMarks(from, today),
            observeDayScore(day),
            scheduleRepository.observeTasksForRange(from, today),
            repository.observeTaskMisses(from, today)
        ) { items, marks, facts, tasks, misses ->
            MuhosabaUiState(
                date = day,
                isToday = day == today,
                plans = MuhosabaSummary.planMarks(tasks[day].orEmpty(), misses[day].orEmpty()),
                items = items,
                marks = marks[day].orEmpty(),
                facts = facts,
                week = MuhosabaSummary.summarize(items, marks, today, WEEK_DAYS),
                month = MuhosabaSummary.summarize(items, marks, today, MONTH_DAYS),
                planWeek = MuhosabaSummary.summarizePlans(tasks, misses, today, WEEK_DAYS),
                planMonth = MuhosabaSummary.summarizePlans(tasks, misses, today, MONTH_DAYS),
                ledger = MuhosabaSummary.ledger(tasks, misses, items, marks, today, MONTH_DAYS)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun previousDay() {
        viewModelScope.launch {
            val today = today()
            val current = picked.value ?: today
            // Faqat oxirgi bir oy ichida orqaga
            if (current.isAfter(today.minusDays(MONTH_DAYS - 1L))) picked.value = current.minusDays(1)
        }
    }

    fun nextDay() {
        viewModelScope.launch {
            val today = today()
            val current = picked.value ?: return@launch
            picked.value = current.plusDays(1).takeIf { it.isBefore(today) }
        }
    }

    fun setMark(itemId: Long, done: Boolean) {
        val state = uiState.value ?: return
        // Tanlangan belgi yana bosilsa — olib tashlanadi
        val next = if (state.marks[itemId] == done) null else done
        viewModelScope.launch { runCatching { repository.setMark(itemId, state.date, next) } }
    }

    /** Kun tartibidagi rejaga ✓ yoki ✗. Xuddi shu belgi yana bosilsa — olib tashlanadi. */
    fun setPlanMark(entryId: Long, done: Boolean) {
        val state = uiState.value ?: return
        val current = state.plans.firstOrNull { it.task.entry.id == entryId }?.mark
        val next = if (current == done) null else done
        viewModelScope.launch {
            runCatching {
                scheduleRepository.setTaskCompleted(entryId, state.date, next == true)
                repository.setTaskMissed(entryId, state.date, next == false)
                reminderScheduler.rescheduleAll()
            }
        }
    }

    fun addItem(title: String) {
        viewModelScope.launch { runCatching { repository.addItem(title.take(MAX_TITLE)) } }
    }

    fun addItems(titles: List<String>) {
        viewModelScope.launch { runCatching { titles.forEach { repository.addItem(it) } } }
    }

    fun removeItem(id: Long) {
        viewModelScope.launch { runCatching { repository.removeItem(id) } }
    }

    companion object {
        const val WEEK_DAYS = 7
        const val MONTH_DAYS = 30
        const val MAX_TITLE = 60
    }
}

/** Davr xulosasi (sof mantiq — testlanadi). */
object MuhosabaSummary {

    /** Oxirgi `days` kun (bugun ham kiradi) ichida har bir band necha kun bajarilgan / bajarilmagan. */
    fun summarize(
        items: List<MuhosabaItem>,
        marks: Map<LocalDate, Map<Long, Boolean>>,
        today: LocalDate,
        days: Int
    ): List<ItemSummary> {
        val from = today.minusDays(days - 1L)
        val inRange = marks.filterKeys { !it.isBefore(from) && !it.isAfter(today) }
        return items.map { item ->
            val values = inRange.values.mapNotNull { it[item.id] }
            ItemSummary(item, done = values.count { it }, missed = values.count { !it }, days = days)
        }
    }

    /** Kunning rejalari va belgilari: bajarilgan — ✓, ✗ bosilgan — ✗, qolgani belgilanmagan. */
    fun planMarks(tasks: List<DailyTask>, misses: Set<Long>): List<PlanMark> =
        tasks.sortedBy { it.entry.startTime }.map { task ->
            val mark = when {
                task.isCompleted -> true
                task.entry.id in misses -> false
                else -> null
            }
            PlanMark(task, mark)
        }

    /** Har bir reja davr ichida necha kun rejada bo'lgan, nechtasida ✓ va ✗ qo'yilgan. */
    fun summarizePlans(
        tasksByDate: Map<LocalDate, List<DailyTask>>,
        misses: Map<LocalDate, Set<Long>>,
        today: LocalDate,
        days: Int
    ): List<PlanSummary> {
        val from = today.minusDays(days - 1L)
        val byEntry = LinkedHashMap<Long, MutableList<PlanMark>>()
        tasksByDate.filterKeys { !it.isBefore(from) && !it.isAfter(today) }.toSortedMap().forEach { (date, tasks) ->
            planMarks(tasks, misses[date].orEmpty()).forEach {
                byEntry.getOrPut(it.task.entry.id) { mutableListOf() } += it
            }
        }
        return byEntry.values
            .map { list ->
                val entry = list.last().task.entry
                PlanSummary(
                    entryId = entry.id,
                    title = entry.title,
                    colorHex = entry.colorHex,
                    done = list.count { it.mark == true },
                    missed = list.count { it.mark == false },
                    scheduled = list.size
                )
            }
            .sortedBy { it.title.lowercase() }
    }

    /** Daftar: har kuni nechta ✓, nechta ✗ va jami nechta reja/band bo'lgan (eskisi birinchi). */
    fun ledger(
        tasksByDate: Map<LocalDate, List<DailyTask>>,
        misses: Map<LocalDate, Set<Long>>,
        items: List<MuhosabaItem>,
        marks: Map<LocalDate, Map<Long, Boolean>>,
        today: LocalDate,
        days: Int
    ): List<DayLedger> {
        val itemIds = items.mapTo(HashSet()) { it.id }
        return (days - 1 downTo 0).map { back ->
            val date = today.minusDays(back.toLong())
            val plans = planMarks(tasksByDate[date].orEmpty(), misses[date].orEmpty())
            val itemMarks = marks[date].orEmpty().filterKeys { it in itemIds }.values
            DayLedger(
                date = date,
                done = plans.count { it.mark == true } + itemMarks.count { it },
                missed = plans.count { it.mark == false } + itemMarks.count { !it },
                total = plans.size + items.size
            )
        }
    }
}
