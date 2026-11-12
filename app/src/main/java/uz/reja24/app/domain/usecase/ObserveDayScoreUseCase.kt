package uz.reja24.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import uz.reja24.app.domain.model.DailyScore
import uz.reja24.app.domain.model.ScoreCalculator
import uz.reja24.app.domain.repository.RoutineSettingsRepository
import uz.reja24.app.domain.repository.ScheduleRepository
import uz.reja24.app.domain.repository.ScoreRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Kun bahosini "jonli" hisoblaydi: reja belgilansa yoki favqulodda kirish bo'lsa,
 * ekrandagi ball darhol yangilanadi (kun yopilishini kutmaydi).
 */
class ObserveDayScoreUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val scoreRepository: ScoreRepository,
    private val routineSettingsRepository: RoutineSettingsRepository
) {
    operator fun invoke(date: LocalDate): Flow<DailyScore> = combine(
        scheduleRepository.observeTasksForDate(date),
        scoreRepository.observeDailyScore(date),
        routineSettingsRepository.observeSettings()
    ) { tasks, stored, settings ->
        ScoreCalculator.calculate(
            date = date,
            completedTasks = tasks.count { it.isCompleted },
            totalTasks = tasks.size,
            isRoutineActive = settings.isRoutineActive,
            bypassCount = stored?.bypassCount ?: 0,
            usedPhoneAfterSleep = stored?.usedPhoneAfterSleep ?: false,
            wokeOnTime = stored?.wokeOnTime ?: false
        )
    }
}
