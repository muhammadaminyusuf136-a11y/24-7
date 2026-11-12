package uz.reja24.app.service.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import uz.reja24.app.core.util.TimeProvider
import uz.reja24.app.domain.repository.ReminderScheduler
import uz.reja24.app.domain.repository.RoutineSettingsRepository
import uz.reja24.app.service.DayCloser

/** Har kuni tongda ishlaydi: yopilgan kunlarning bali, streak, yakshanba — haftalik kubok. */
@HiltWorker
class DailyEvaluationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val dayCloser: DayCloser,
    private val reminderScheduler: ReminderScheduler,
    private val routineSettingsRepository: RoutineSettingsRepository,
    private val timeProvider: TimeProvider
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        dayCloser.closeFinishedDays(notifyTrophy = true)
        reminderScheduler.rescheduleAll()
        // Ertangi baholash
        WorkScheduler.scheduleDailyEvaluation(
            applicationContext,
            timeProvider.now(),
            routineSettingsRepository.getSettings().wakeTime
        )
        Result.success()
    } catch (e: Exception) {
        if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
    }

    companion object {
        const val TAG = "daily-evaluation"
        private const val MAX_RETRIES = 3
    }
}
