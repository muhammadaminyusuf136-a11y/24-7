package uz.reja24.app.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import uz.reja24.app.core.util.TimeProvider
import uz.reja24.app.domain.repository.ReminderScheduler
import uz.reja24.app.domain.repository.RoutineSettingsRepository
import uz.reja24.app.domain.repository.UserPreferencesRepository
import uz.reja24.app.service.blocking.BlockingMonitorService
import uz.reja24.app.service.worker.WorkScheduler
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hamma fon ishlarini bir joydan "yangilaydi":
 * tugagan kunlarni baholash, eslatmalar, tonggi baholash vazifasi, kuzatuv xizmati.
 * Ilova ochilganda, telefon yoqilganda va vaqt zonasi o'zgarganda chaqiriladi.
 */
@Singleton
class RoutineOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dayCloser: DayCloser,
    private val reminderScheduler: ReminderScheduler,
    private val routineSettingsRepository: RoutineSettingsRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val timeProvider: TimeProvider
) {

    suspend fun syncAll() {
        try {
            // Ilova ochiq — kubok tabrigini ilova o'zi ko'rsatadi, bildirishnoma shart emas
            dayCloser.closeFinishedDays(notifyTrophy = false)
        } catch (e: Exception) {
            // baholashdagi xato eslatmalarga to'sqinlik qilmasin
        }
        reminderScheduler.rescheduleAll()
        WorkScheduler.scheduleDailyEvaluation(
            context,
            timeProvider.now(),
            routineSettingsRepository.getSettings().wakeTime
        )
        refreshMonitorService()
    }

    /** Rejim faol va tanishuv tugagan bo'lsa — kuzatuv xizmatini yoqadi, aks holda o'chiradi. */
    suspend fun refreshMonitorService() {
        val settings = routineSettingsRepository.getSettings()
        val onboardingDone = userPreferencesRepository.preferences.first().isOnboardingCompleted
        if (settings.isRoutineActive && onboardingDone) {
            BlockingMonitorService.start(context)
        } else {
            BlockingMonitorService.stop(context)
        }
    }
}
