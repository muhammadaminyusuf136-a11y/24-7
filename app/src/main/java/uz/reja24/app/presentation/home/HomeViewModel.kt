package uz.reja24.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import uz.reja24.app.core.util.TimeProvider
import uz.reja24.app.domain.model.DailyScore
import uz.reja24.app.domain.model.DailyTask
import uz.reja24.app.domain.model.RoutineSettings
import uz.reja24.app.domain.prayer.Prayer
import uz.reja24.app.domain.prayer.PrayerOverview
import uz.reja24.app.domain.prayer.PrayerSchedule
import uz.reja24.app.domain.repository.PrayerSettingsRepository
import uz.reja24.app.domain.repository.RoutineSettingsRepository
import uz.reja24.app.domain.repository.ScheduleRepository
import uz.reja24.app.domain.repository.StreakRepository
import uz.reja24.app.domain.usecase.ObserveDayScoreUseCase
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class HomeUiState(
    val now: LocalDateTime,
    /** Kecha, bugun va ertangi rejalar (lenta uchun). */
    val tasksByDate: Map<LocalDate, List<DailyTask>>,
    val settings: RoutineSettings,
    /** Davom etayotgan "rejim kuni" bahosi (yarim tundan keyin — hali kechagi kun). */
    val score: DailyScore,
    val streakDays: Int,
    /** Kecha, bugun va ertangi namoz vaqtlari. */
    val prayer: PrayerOverview
) {
    val today: LocalDate get() = now.toLocalDate()
    val tasks: List<DailyTask> get() = tasksByDate[today].orEmpty()
    val next: NextItem? get() = HomeCalculations.nextItem(tasks, settings, now)
    val nextPrayer: Pair<Prayer, LocalDateTime>? get() = PrayerSchedule.next(prayer.days.values.sortedBy { it.date }, now)
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    scheduleRepository: ScheduleRepository,
    routineSettingsRepository: RoutineSettingsRepository,
    streakRepository: StreakRepository,
    prayerSettingsRepository: PrayerSettingsRepository,
    observeDayScore: ObserveDayScoreUseCase,
    timeProvider: TimeProvider
) : ViewModel() {

    /** Har soniyada joriy vaqt (soat, lenta va countdown uchun). */
    private val ticker: Flow<LocalDateTime> = flow {
        while (true) {
            emit(timeProvider.now())
            delay(TICK_MS)
        }
    }

    private val settings = routineSettingsRepository.observeSettings()
    private val today = ticker.map { it.toLocalDate() }.distinctUntilChanged()
    private val routineDay = combine(ticker, settings) { now, s -> s.routineDayFor(now) }.distinctUntilChanged()

    /** Namoz vaqtlari faqat kun yoki sozlama o'zgarganda qayta hisoblanadi (har soniyada emas). */
    private val prayer: Flow<PrayerOverview> = combine(prayerSettingsRepository.settings, today) { s, day ->
        PrayerOverview.build(s, timeProvider.zone(), listOf(day.minusDays(1), day, day.plusDays(1)))
    }

    private val base = combine(
        ticker,
        today.flatMapLatest { day -> scheduleRepository.observeTasksForRange(day.minusDays(1), day.plusDays(1)) },
        settings,
        routineDay.flatMapLatest { observeDayScore(it) },
        routineDay.flatMapLatest { streakRepository.observeSummary(it) }
    ) { now, tasks, s, score, streak -> Base(now, tasks, s, score, streak.current) }

    val uiState: StateFlow<HomeUiState?> = combine(base, prayer) { b, p ->
        HomeUiState(b.now, b.tasks, b.settings, b.score, b.streak, p)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private data class Base(
        val now: LocalDateTime,
        val tasks: Map<LocalDate, List<DailyTask>>,
        val settings: RoutineSettings,
        val score: DailyScore,
        val streak: Int
    )

    private companion object {
        const val TICK_MS = 1_000L
    }
}
