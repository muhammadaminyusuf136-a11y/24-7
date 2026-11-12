package uz.reja24.app.service

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uz.reja24.app.core.util.TimeProvider
import uz.reja24.app.domain.model.TrophyLevel
import uz.reja24.app.domain.repository.RoutineSettingsRepository
import uz.reja24.app.domain.usecase.EvaluateDayUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tugagan "rejim kun"larini yopadi (hisoblaydi). Uch joydan chaqiriladi: uyg'onish budilnigi,
 * kunlik WorkManager vazifasi va ilova ochilishi. Mutex — ular bir vaqtda kelib qolsa,
 * bir kun ikki marta hisoblanmasligi uchun.
 *
 * Kubok bildirishnomasi olib tashlangan ("Natija" o'rniga "Muhosaba" bo'limi) —
 * notifyTrophy parametri chaqiruvchilar o'zgarmasligi uchun qoldirilgan.
 */
@Singleton
class DayCloser @Inject constructor(
    private val evaluateDayUseCase: EvaluateDayUseCase,
    private val routineSettingsRepository: RoutineSettingsRepository,
    private val timeProvider: TimeProvider
) {
    private val mutex = Mutex()

    @Suppress("UNUSED_PARAMETER")
    suspend fun closeFinishedDays(notifyTrophy: Boolean): TrophyLevel? = mutex.withLock {
        val settings = routineSettingsRepository.getSettings()
        evaluateDayUseCase.catchUp(settings.routineDayFor(timeProvider.now()))
    }
}
