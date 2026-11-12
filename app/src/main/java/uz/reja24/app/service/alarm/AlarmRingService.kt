package uz.reja24.app.service.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uz.reja24.app.R
import uz.reja24.app.presentation.alarm.AlarmActivity

/** Hozir chalinayotgan budilnik (budilnik oynasi shuni ko'rsatadi). */
data class RingingAlarm(
    val title: String,
    val text: String,
    val soundUri: String?,
    val startedAtMillis: Long
)

/**
 * Budilnik: tanlangan ovozni takror-takror chaladi va telefonni titratadi —
 * foydalanuvchi "O'chirish" yoki "Keyinroq" bosmaguncha (ko'pi bilan 10 daqiqa).
 * Ovoz "budilnik" kanalidan chaladi: "Bezovta qilmang" rejimida ham eshitiladi.
 */
class AlarmRingService : Service() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoStop = Runnable { stopRinging() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopRinging()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                _ringing.value?.let { AlarmScheduler.scheduleSnooze(this, it, SNOOZE_MINUTES) }
                stopRinging()
                return START_NOT_STICKY
            }
        }
        val alarm = RingingAlarm(
            title = intent?.getStringExtra(EXTRA_TITLE).orEmpty(),
            text = intent?.getStringExtra(EXTRA_TEXT).orEmpty(),
            soundUri = intent?.getStringExtra(EXTRA_SOUND),
            startedAtMillis = System.currentTimeMillis()
        )
        // Oyna ochilganda nimani ko'rsatishni bilishi uchun — bildirishnomadan oldin
        _ringing.value = alarm
        // startForegroundService dan keyin 5 soniya ichida albatta chaqirilishi shart
        val started = try {
            ServiceCompat.startForeground(
                this, NotificationHelper.ID_ALARM, buildNotification(alarm),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
            )
            true
        } catch (e: Exception) {
            // Tizim fon xizmatini taqiqlasa ham — hech bo'lmasa oddiy bildirishnoma chiqadi
            NotificationHelper.show(this, NotificationHelper.ID_ALARM, alarm.title, alarm.text)
            false
        }
        if (!started) {
            _ringing.value = null
            stopSelf()
            return START_NOT_STICKY
        }
        startSound(alarm.soundUri)
        startVibration()
        handler.removeCallbacks(autoStop)
        handler.postDelayed(autoStop, MAX_RING_MS)
        return START_NOT_STICKY
    }

    private fun buildNotification(alarm: RingingAlarm): Notification {
        val fullScreen = PendingIntent.getActivity(
            this, REQUEST_FULL_SCREEN,
            Intent(this, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ALARMS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(alarm.title)
            .setContentText(alarm.text)
            .setColor(getColor(R.color.reja_accent))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setSilent(true)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .addAction(0, getString(R.string.alarm_snooze, SNOOZE_MINUTES.toString()), actionIntent(ACTION_SNOOZE, REQUEST_SNOOZE))
            .addAction(0, getString(R.string.alarm_stop), actionIntent(ACTION_STOP, REQUEST_STOP))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun actionIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this, requestCode,
            Intent(this, AlarmRingService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun startSound(soundUri: String?) {
        releasePlayer()
        val uris = listOfNotNull(
            soundUri?.let { runCatching { Uri.parse(it) }.getOrNull() },
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        )
        // Tanlangan ovoz o'chirilgan bo'lsa (masalan fayl o'chirilgan) — keyingisi sinaladi
        for (uri in uris) {
            val created = try {
                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setWakeMode(this@AlarmRingService, PowerManager.PARTIAL_WAKE_LOCK)
                    setDataSource(this@AlarmRingService, uri)
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                null
            }
            if (created != null) {
                player = created
                return
            }
        }
    }

    private fun startVibration() {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
        if (v == null || !v.hasVibrator()) return
        vibrator = v
        runCatching { v.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, 0)) }
    }

    private fun stopRinging() {
        handler.removeCallbacks(autoStop)
        releasePlayer()
        runCatching { vibrator?.cancel() }
        vibrator = null
        _ringing.value = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releasePlayer() {
        player?.let { runCatching { it.stop() }; runCatching { it.release() } }
        player = null
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoStop)
        releasePlayer()
        runCatching { vibrator?.cancel() }
        _ringing.value = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "uz.reja24.app.action.ALARM_START"
        const val ACTION_STOP = "uz.reja24.app.action.ALARM_STOP"
        const val ACTION_SNOOZE = "uz.reja24.app.action.ALARM_SNOOZE"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
        const val EXTRA_SOUND = "sound"
        const val SNOOZE_MINUTES = 5

        private const val MAX_RING_MS = 10 * 60 * 1000L
        private const val REQUEST_FULL_SCREEN = 7001
        private const val REQUEST_SNOOZE = 7002
        private const val REQUEST_STOP = 7003
        private val VIBRATION_PATTERN = longArrayOf(0, 800, 600)

        private val _ringing = MutableStateFlow<RingingAlarm?>(null)
        val ringing: StateFlow<RingingAlarm?> = _ringing.asStateFlow()

        fun start(context: Context, title: String, text: String, soundUri: String?) {
            val intent = Intent(context, AlarmRingService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_TEXT, text)
                .putExtra(EXTRA_SOUND, soundUri)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                // Android fon xizmatini boshlashga ruxsat bermadi — oddiy bildirishnoma
                NotificationHelper.show(context, NotificationHelper.ID_ALARM, title, text)
            }
        }

        fun send(context: Context, action: String) {
            runCatching { context.startService(Intent(context, AlarmRingService::class.java).setAction(action)) }
        }
    }
}
