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
import uz.reja24.app.domain.repository.MuhosabaItem
import uz.reja24.app.domain.repository.MuhosabaRepository
import uz.reja24.app.domain.repository.RoutineSettingsRepository
import uz.reja24.app.domain.usecase.ObserveDayScoreUseCase
import java.time.LocalDate
import javax.inject.Inject

/** Bir bandning davr (hafta/oy) bo'yicha xulosasi. */
data class ItemSummary(val item: MuhosabaItem, val done: Int, val missed: Int, val days: Int)

data class MuhosabaUiState(
    /** Hisob qilinayotgan kun ("rejim kuni": yarim tundan keyin ham uyg'onishgacha — kechagi kun). */
    val date: LocalDate,
    val isToday: Boolean,
    val items: List<MuhosabaItem>,
    /** Shu kunning belgilari: band id → bajarildimi. */
    val marks: Map<Long, Boolean>,
    /** Ilova o'zi biladigan narsalar: rejalar, rejim, favqulodda kirish. */
    val facts: DailyScore,
    val week: List<ItemSummary>,
    val month: List<ItemSummary>
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MuhosabaViewModel @Inject constructor(
    private val repository: MuhosabaRepository,
    private val routineSettingsRepository: RoutineSettingsRepository,
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
        combine(
            repository.observeItems(),
            repository.observeMarks(today.minusDays(MONTH_DAYS - 1L), today),
            observeDayScore(day)
        ) { items, marks, facts ->
            MuhosabaUiState(
                date = day,
                isToday = day == today,
                items = items,
                marks = marks[day].orEmpty(),
                facts = facts,
                week = MuhosabaSummary.summarize(items, marks, today, WEEK_DAYS),
                month = MuhosabaSummary.summarize(items, marks, today, MONTH_DAYS)
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
}
