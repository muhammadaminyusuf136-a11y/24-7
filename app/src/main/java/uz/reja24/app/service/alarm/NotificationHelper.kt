package uz.reja24.app.service.alarm

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import uz.reja24.app.MainActivity
import uz.reja24.app.R
import uz.reja24.app.core.util.PermissionChecker

/**
 * Bildirishnomalar. Kanallar 4 ta:
 *  - reminders         — eslatmalar (standart ovoz bilan)
 *  - reminders_silent  — eslatmalar (ovozsiz; foydalanuvchi o'z ringtonini tanlaganda
 *                        ovozni ilovaning o'zi chaladi)
 *  - monitor           — doimiy "rejim faol" bildirishnomasi
 *  - achievements      — kuboklar
 */
object NotificationHelper {

    const val CHANNEL_REMINDERS = "reminders"
    const val CHANNEL_REMINDERS_SILENT = "reminders_silent"
    const val CHANNEL_MONITOR = "monitor"
    /** Eski (o'chirilgan) kanal nomi — faqat telefondan o'chirish uchun. */
    private const val CHANNEL_ACHIEVEMENTS = "achievements"
    /** Budilnik: ovozni xizmat o'zi chaladi (kanal ovozsiz), ekran to'liq ochiladi. */
    const val CHANNEL_ALARMS = "alarms"

    const val ID_MONITOR = 1
    const val ID_ROUTINE = 2
    const val ID_MORNING = 3
    const val ID_ALARM = 5
    private const val ID_PRAYER_BASE = 500
    private const val ID_TASK_BASE = 1000

    fun taskNotificationId(entryId: Long): Int = ID_TASK_BASE + (entryId % 100_000).toInt()

    fun prayerNotificationId(prayerOrdinal: Int): Int = ID_PRAYER_BASE + prayerOrdinal

    /**
     * Kanallarni yaratadi (yoki nomlarini joriy tilga yangilaydi).
     * context — kerakli tildagi kontekst.
     */
    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channels = listOf(
            NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.channel_reminders),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = context.getString(R.string.channel_reminders_desc) },
            NotificationChannel(
                CHANNEL_REMINDERS_SILENT,
                context.getString(R.string.channel_reminders_silent),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_reminders_silent_desc)
                setSound(null, null)
            },
            NotificationChannel(
                CHANNEL_MONITOR,
                context.getString(R.string.channel_monitor),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_monitor_desc)
                setShowBadge(false)
            },
            NotificationChannel(
                CHANNEL_ALARMS,
                context.getString(R.string.channel_alarms),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_alarms_desc)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
        try {
            manager.createNotificationChannels(channels)
            // Kuboklar olib tashlangan — eski "Mukofotlar" kanali telefon sozlamalarida qolmasin
            manager.deleteNotificationChannel(CHANNEL_ACHIEVEMENTS)
        } catch (e: Exception) {
            // kanal yaratilmasa ham ilova ishlashda davom etadi
        }
    }

    fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Kechki muhosaba oynasini ochadigan intent (bildirishnoma bosilganda yoki to'g'ridan-to'g'ri). */
    fun muhosabaIntent(context: Context): Intent =
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MainActivity.EXTRA_OPEN_MUHOSABA, true)

    fun openMuhosabaIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 1, muhosabaIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** Oddiy xabar ko'rsatadi. Ruxsat bo'lmasa — jimgina o'tkazib yuboradi. */
    @SuppressLint("MissingPermission")
    fun show(
        context: Context,
        id: Int,
        title: String,
        text: String,
        channel: String = CHANNEL_REMINDERS,
        contentIntent: PendingIntent = openAppIntent(context)
    ) {
        if (!PermissionChecker.hasNotificationPermission(context)) return
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setColor(context.getColor(R.color.reja_accent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // foydalanuvchi ruxsatni shu orada olib qo'ygan
        }
    }

    /** Doimiy bildirishnoma (Foreground Service uchun). */
    fun monitorNotification(context: Context, text: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_MONITOR)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.monitor_title))
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openAppIntent(context))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
}
