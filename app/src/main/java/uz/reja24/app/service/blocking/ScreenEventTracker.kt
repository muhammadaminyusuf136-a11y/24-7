package uz.reja24.app.service.blocking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import uz.reja24.app.core.di.ApplicationScope
import uz.reja24.app.core.util.TimeProvider
import uz.reja24.app.domain.repository.RoutineSettingsRepository
import uz.reja24.app.domain.repository.ScoreRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Telefon qulfdan ochilgan paytlarni kuzatadi (faqat vaqtni — boshqa hech narsani):
 *  - uyg'onishdan 1 soat oldin — 30 daqiqa keyin ochilsa → "vaqtida uyg'ondi" (+10 ball)
 *  - uyqu vaqtidan keyin, tun bo'yi ochilsa            → "vaqtida uxlamadi" (10 ball berilmaydi)
 */
@Singleton
class ScreenEventTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val routineSettingsRepository: RoutineSettingsRepository,
    private val scoreRepository: ScoreRepository,
    private val timeProvider: TimeProvider,
    @ApplicationScope private val scope: CoroutineScope
) {
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_USER_PRESENT) onUnlock()
        }
    }

    fun register() {
        if (registered) return
        try {
            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(Intent.ACTION_USER_PRESENT),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            registered = true
        } catch (e: Exception) {
            // ro'yxatdan o'tmasa — faqat uyqu/uyg'onish ballari hisoblanmaydi
        }
    }

    private fun onUnlock() {
        scope.launch {
            try {
                val now = timeProvider.now()
                val settings = routineSettingsRepository.getSettings()
                if (!settings.isRoutineActive) return@launch
                val time = now.toLocalTime()
                // Tong — o'sha tongning kuniga, tungi ishlatish — kechqurun boshlangan "rejim kuni"ga
                if (settings.isInWakeWindow(time)) scoreRepository.markWokeOnTime(settings.wakeDayFor(now))
                if (settings.isAfterBedtime(time)) {
                    scoreRepository.markPhoneUsedAfterSleep(settings.routineDayFor(now))
                }
            } catch (e: Exception) {
                // ma'lumot bazasi band bo'lsa — bu hodisa o'tkazib yuboriladi
            }
        }
    }
}
