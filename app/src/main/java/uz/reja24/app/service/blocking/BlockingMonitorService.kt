package uz.reja24.app.service.blocking

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import uz.reja24.app.R
import uz.reja24.app.core.util.LocaleHelper
import uz.reja24.app.core.util.TimeFormat
import uz.reja24.app.domain.repository.RoutineSettingsRepository
import uz.reja24.app.domain.repository.UserPreferencesRepository
import uz.reja24.app.service.alarm.NotificationHelper
import javax.inject.Inject

/**
 * Foreground Service — doimiy bildirishnoma bilan ishlaydi. Telefon (ayniqsa Xiaomi, Samsung)
 * ilovani fonda "o'ldirib" qo'ymasligi uchun kerak. Bu xizmat hech narsani o'qimaydi —
 * u faqat ilova jarayonini tirik saqlaydi va rejim vaqtini ko'rsatib turadi.
 */
@AndroidEntryPoint
class BlockingMonitorService : Service() {

    @Inject lateinit var routineSettingsRepository: RoutineSettingsRepository
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var started = false
    private var lastContext: Context? = null
    private var lastText: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Har bir startForegroundService chaqiruvidan keyin startForeground SHART
        // (xizmat allaqachon ishlayotgan bo'lsa ham) — aks holda Android ilovani yopadi.
        startForegroundCompat(
            lastContext ?: this,
            lastText ?: getString(R.string.monitor_text_default)
        )
        if (!started) {
            started = true
            observeSettings()
        }
        return START_STICKY
    }

    private fun observeSettings() {
        scope.launch {
            combine(
                routineSettingsRepository.observeSettings(),
                userPreferencesRepository.preferences
            ) { settings, prefs -> settings to prefs }
                .collect { (settings, prefs) ->
                    if (!settings.isRoutineActive) {
                        stopForegroundCompat()
                        stopSelf()
                        return@collect
                    }
                    val context = LocaleHelper.localizedContext(
                        this@BlockingMonitorService,
                        if (prefs.isLanguageChosen) prefs.language else null
                    )
                    val text = context.getString(
                        R.string.monitor_text,
                        TimeFormat.time(settings.blockStartTime),
                        TimeFormat.time(settings.wakeTime)
                    )
                    lastContext = context
                    lastText = text
                    val manager = getSystemService(android.app.NotificationManager::class.java)
                    try {
                        manager?.notify(NotificationHelper.ID_MONITOR, NotificationHelper.monitorNotification(context, text))
                    } catch (e: SecurityException) {
                        // bildirishnoma ruxsati yo'q — xizmat baribir ishlaydi
                    }
                }
        }
    }

    private fun startForegroundCompat(context: Context, text: String) {
        val notification = NotificationHelper.monitorNotification(context, text)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NotificationHelper.ID_MONITOR, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NotificationHelper.ID_MONITOR, notification)
            }
        } catch (e: Exception) {
            // Tizim ruxsat bermadi (masalan fondan ishga tushirish cheklovi) — xizmatni to'xtatamiz
            stopSelf()
        }
    }

    private fun stopForegroundCompat() {
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, BlockingMonitorService::class.java))
            } catch (e: Exception) {
                // Android 12+ da fondan ishga tushirish cheklangan bo'lishi mumkin —
                // ilova ochilganda yoki keyingi budilnikda qayta urinadi
            }
        }

        /** stopService fonda ham ruxsat etilgan (startService esa Android 8+ da fonda taqiqlangan). */
        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, BlockingMonitorService::class.java))
            } catch (e: Exception) {
                // xizmat ishlamayotgan bo'lsa — hech narsa qilish shart emas
            }
        }
    }
}
