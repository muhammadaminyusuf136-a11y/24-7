package uz.reja24.app.service.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Kunlik baholashni rejalashtiradi (WorkManager): har kuni uyg'onish vaqtidan 2 daqiqa keyin
 * kechagi "rejim kuni" yopiladi. Bu zaxira yo'l — asosiy baholash uyg'onish budilnigida bo'ladi.
 */
object WorkScheduler {

    private const val DELAY_AFTER_WAKE_MINUTES = 2L

    fun scheduleDailyEvaluation(context: Context, now: LocalDateTime, wakeTime: LocalTime) {
        var target = now.toLocalDate().atTime(wakeTime).plusMinutes(DELAY_AFTER_WAKE_MINUTES)
        if (!target.isAfter(now)) target = target.plusDays(1)
        val delayMillis = Duration.between(now, target).toMillis().coerceAtLeast(0)

        val request = OneTimeWorkRequestBuilder<DailyEvaluationWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .addTag(DailyEvaluationWorker.TAG)
            .build()
        try {
            // Har bir kun uchun alohida nom — takroriy chaqiriq yangi vazifa yaratmaydi.
            // Worker qaysi kunlarni baholashni o'zi hisoblaydi, shuning uchun kechiksa ham xato bo'lmaydi.
            WorkManager.getInstance(context).enqueueUniqueWork(
                "daily-evaluation-${target.toLocalDate()}",
                ExistingWorkPolicy.KEEP,
                request
            )
        } catch (e: Exception) {
            // WorkManager hali tayyor bo'lmasa — keyingi ochilishda qayta urinadi
        }
    }
}
