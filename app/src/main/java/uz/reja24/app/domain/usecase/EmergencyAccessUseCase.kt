package uz.reja24.app.domain.usecase

import uz.reja24.app.core.util.TimeProvider
import uz.reja24.app.domain.model.BlockPolicy
import uz.reja24.app.domain.repository.AppStateRepository
import uz.reja24.app.domain.repository.RoutineSettingsRepository
import uz.reja24.app.domain.repository.ScoreRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Favqulodda kirish: bir kechada 3 marta (qattiq rejimda 1 marta), har biri 5 daqiqa, −30 ball.
 *
 * Limit kalendar kuniga emas, "rejim kuni"ga bog'langan: aks holda 23:50 da 3 marta,
 * 00:05 da yana 3 marta foydalanish mumkin bo'lardi.
 */
class EmergencyAccessUseCase @Inject constructor(
    private val scoreRepository: ScoreRepository,
    private val appStateRepository: AppStateRepository,
    private val routineSettingsRepository: RoutineSettingsRepository,
    private val timeProvider: TimeProvider
) {

    /** Shu kecha yana necha marta favqulodda kirish mumkin. */
    suspend fun remainingToday(): Int {
        val settings = routineSettingsRepository.getSettings()
        val day = settings.routineDayFor(timeProvider.now())
        val used = scoreRepository.getDailyScore(day)?.bypassCount ?: 0
        return (settings.maxDailyBypasses - used).coerceAtLeast(0)
    }

    /** Ruxsat beradi. Limit tugagan bo'lsa false qaytaradi. */
    suspend fun grant(): Boolean {
        if (remainingToday() <= 0) return false
        val settings = routineSettingsRepository.getSettings()
        scoreRepository.registerBypass(settings.routineDayFor(timeProvider.now()))
        val until = timeProvider.currentMillis() +
            TimeUnit.MINUTES.toMillis(BlockPolicy.BYPASS_DURATION_MINUTES)
        appStateRepository.setBypassUntilMillis(until)
        return true
    }
}
