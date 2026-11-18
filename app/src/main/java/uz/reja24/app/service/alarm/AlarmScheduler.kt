package uz.reja24.app.service.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uz.reja24.app.MainActivity
import uz.reja24.app.core.util.PermissionChecker
import uz.reja24.app.core.util.TimeProvider
import uz.reja24.app.domain.model.AlarmPlanner
import uz.reja24.app.domain.model.PlannedPrayerAlarm
import uz.reja24.app.domain.model.PlannedTaskAlarm
import uz.reja24.app.domain.model.PrayerAlarmKind
import uz.reja24.app.domain.model.RoutineAlarmKind
import uz.reja24.app.domain.prayer.Prayer
import uz.reja24.app.domain.prayer.PrayerOverview
import uz.reja24.app.domain.repository.PrayerSettingsRepository
import uz.reja24.app.domain.repository.ReminderScheduler
import uz.reja24.app.domain.repository.RoutineSettingsRepository
import uz.reja24.app.domain.repository.ScheduleRepository
import uz.reja24.app.domain.repository.UserPreferencesRepository
import uz.reja24.app.widget.DayPlanWidget
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Barcha eslatmalarni AlarmManager orqali rejalashtiradi.
 *
 * setExactAndAllowWhileIdle — telefon "uxlab" yotgan (Doze) holatda ham aniq vaqtda chaladi.
 * Budilniklar uchun setAlarmClock — eng ishonchli usul (tizim uni hech qachon kechiktirmaydi).
 * Android 12+ da foydalanuvchi "Aniq budilnik" ruxsatini bermagan bo'lsa,
 * setAndAllowWhileIdle ishlatiladi (bir necha daqiqa kechikishi mumkin, lekin ishlaydi).
 *
 * Har safar faqat eng yaqin eslatmalar qo'yiladi (bugun + ertaga) va har bir
 * eslatma chalingach hammasi qayta hisoblanadi — shunda eslatmalar hech qachon "yo'qolmaydi".
 */
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val routineSettingsRepository: RoutineSettingsRepository,
    private val scheduleRepository: ScheduleRepository,
    private val prayerSettingsRepository: PrayerSettingsRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val timeProvider: TimeProvider
) : ReminderScheduler {

    private val alarmManager: AlarmManager? = context.getSystemService(AlarmManager::class.java)
    private val codesPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mutex = Mutex()

    override suspend fun rescheduleAll() {
        mutex.withLock {
            // Har bir qism alohida: birida xato bo'lsa, qolganlari baribir qo'yiladi
            runCatching { scheduleRoutineAlarms() }
            runCatching { scheduleTaskAlarms() }
            runCatching { schedulePrayerAlarms() }
            runCatching { scheduleMidnightRefresh() }
        }
        // Vidjet — har safar rejalar yoki eslatmalar yangilanganda (hozirgi reja va keyingi namoz o'zgaradi)
        DayPlanWidget.refresh(context)
    }

    /**
     * Yarim tundan keyin vidjet yangi kunni ko'rsatishi uchun. RTC (uyg'otmaydigan) — telefon
     * uxlab yotgan bo'lsa, ekran yoqilganda yetkaziladi; batareya sarflanmaydi.
     */
    private fun scheduleMidnightRefresh() {
        val manager = alarmManager ?: return
        val at = timeProvider.today().plusDays(1).atTime(0, 1)
        val pending = PendingIntent.getBroadcast(
            context, MIDNIGHT_CODE,
            Intent(context, AlarmReceiver::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        manager.set(AlarmManager.RTC, timeProvider.toMillis(at), pending)
    }

    private suspend fun scheduleRoutineAlarms() {
        RoutineAlarmKind.entries.forEach { kind -> cancel(routineIntent(kind), routineRequestCode(kind)) }
        val settings = routineSettingsRepository.getSettings()
        AlarmPlanner.routineAlarms(timeProvider.now(), settings).forEach { (kind, at) ->
            setAlarm(timeProvider.toMillis(at), pendingIntent(routineIntent(kind), routineRequestCode(kind)))
        }
    }

    private suspend fun scheduleTaskAlarms() {
        // Avvalgi eslatmalarni bekor qilamiz
        codesPrefs.getStringSet(KEY_TASK_CODES, emptySet()).orEmpty().forEach { code ->
            code.toIntOrNull()?.let { cancel(taskIntentTemplate(), it) }
        }

        val today = timeProvider.today()
        val todayEntries = scheduleRepository.getTasksForDate(today)
            .filterNot { it.isCompleted }
            .map { it.entry }
        val tomorrowEntries = scheduleRepository.getTasksForDate(today.plusDays(1)).map { it.entry }
        val second = userPreferencesRepository.preferences.first().taskSecondReminderMinutes
        val planned = AlarmPlanner.taskAlarms(timeProvider.now(), today, listOf(todayEntries, tomorrowEntries), second)

        val codes = mutableSetOf<String>()
        planned.forEach { alarm ->
            val code = taskRequestCode(alarm)
            val intent = taskIntentTemplate()
                .putExtra(EXTRA_ENTRY_ID, alarm.entryId)
                .putExtra(EXTRA_KIND, alarm.kind.name)
                .putExtra(EXTRA_DATE, today.plusDays(alarm.dayIndex.toLong()).toEpochDay())
            val millis = timeProvider.toMillis(alarm.triggerAt)
            if (alarm.rings) {
                setAlarmClock(context, millis, pendingIntent(intent, code))
            } else {
                setAlarm(millis, pendingIntent(intent, code))
            }
            codes += code.toString()
        }
        codesPrefs.edit().putStringSet(KEY_TASK_CODES, codes).apply()
    }

    private suspend fun schedulePrayerAlarms() {
        // Barcha mumkin bo'lgan kodlar oldindan ma'lum — hammasi bekor qilinadi
        for (day in 0..1) {
            Prayer.entries.forEach { prayer ->
                PrayerAlarmKind.entries.forEach { kind ->
                    cancel(prayerIntentTemplate(), prayerRequestCode(day, prayer, kind))
                }
            }
        }
        val settings = prayerSettingsRepository.getSettings()
        val today = timeProvider.today()
        val overview = PrayerOverview.build(settings, timeProvider.zone(), listOf(today, today.plusDays(1)))
        if (!overview.isActive) return
        val days = listOf(today, today.plusDays(1)).mapNotNull { overview.days[it] }
        AlarmPlanner.prayerAlarms(timeProvider.now(), days, settings).forEach { alarm ->
            val intent = prayerIntentTemplate()
                .putExtra(EXTRA_PRAYER, alarm.prayer.name)
                .putExtra(EXTRA_KIND, alarm.kind.name)
                .putExtra(EXTRA_PRAYER_AT, timeProvider.toMillis(days[alarm.dayIndex].time(alarm.prayer)))
            val pending = pendingIntent(intent, prayerRequestCode(alarm))
            val millis = timeProvider.toMillis(alarm.triggerAt)
            if (alarm.kind == PrayerAlarmKind.ALARM) setAlarmClock(context, millis, pending) else setAlarm(millis, pending)
        }
    }

    private fun setAlarm(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        val manager = alarmManager ?: return
        try {
            if (PermissionChecker.canScheduleExactAlarms(context)) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            // Aniq budilnik ruxsati olib qo'yilgan — oddiy budilnikka o'tamiz
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun cancel(intent: Intent, requestCode: Int) {
        val existing = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager?.cancel(existing)
        existing.cancel()
    }

    private fun pendingIntent(intent: Intent, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun routineIntent(kind: RoutineAlarmKind): Intent =
        Intent(context, AlarmReceiver::class.java)
            .setAction(ACTION_ROUTINE)
            .putExtra(EXTRA_KIND, kind.name)

    private fun taskIntentTemplate(): Intent =
        Intent(context, AlarmReceiver::class.java).setAction(ACTION_TASK)

    private fun prayerIntentTemplate(): Intent =
        Intent(context, AlarmReceiver::class.java).setAction(ACTION_PRAYER)

    private fun routineRequestCode(kind: RoutineAlarmKind) = ROUTINE_CODE_BASE + kind.ordinal

    private fun taskRequestCode(alarm: PlannedTaskAlarm): Int =
        TASK_CODE_BASE + (alarm.entryId % 100_000).toInt() * 8 + alarm.kind.ordinal * 2 + alarm.dayIndex

    private fun prayerRequestCode(alarm: PlannedPrayerAlarm): Int =
        prayerRequestCode(alarm.dayIndex, alarm.prayer, alarm.kind)

    private fun prayerRequestCode(dayIndex: Int, prayer: Prayer, kind: PrayerAlarmKind): Int =
        PRAYER_CODE_BASE + dayIndex * 100 + prayer.ordinal * 10 + kind.ordinal

    companion object {
        const val ACTION_ROUTINE = "uz.reja24.app.action.ROUTINE_ALARM"
        const val ACTION_TASK = "uz.reja24.app.action.TASK_ALARM"
        const val ACTION_PRAYER = "uz.reja24.app.action.PRAYER_ALARM"
        const val ACTION_SNOOZED = "uz.reja24.app.action.SNOOZED_ALARM"
        /** Faqat qayta rejalashtirish va vidjetni yangilash (yarim tunda). */
        const val ACTION_REFRESH = "uz.reja24.app.action.REFRESH"
        const val EXTRA_KIND = "kind"
        const val EXTRA_ENTRY_ID = "entry_id"
        const val EXTRA_DATE = "date_epoch_day"
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_PRAYER_AT = "prayer_at_millis"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
        const val EXTRA_SOUND = "sound"

        private const val ROUTINE_CODE_BASE = 100
        private const val PRAYER_CODE_BASE = 3_000
        private const val SNOOZE_CODE = 3_900
        private const val MIDNIGHT_CODE = 3_901
        // Rejalar: 10 000 + (id % 100 000) * 8 — PRAYER va SNOOZE kodlari bilan to'qnashmaydi
        private const val TASK_CODE_BASE = 10_000
        private const val PREFS_NAME = "alarm_codes"
        private const val KEY_TASK_CODES = "task_codes"

        /**
         * Budilnik: setAlarmClock — Doze'da ham aniq, status barda soat belgisi chiqadi.
         * Aniq budilnik ruxsati bo'lmasa — oddiy (bir necha daqiqa kechikishi mumkin).
         */
        fun setAlarmClock(context: Context, triggerAtMillis: Long, operation: PendingIntent) {
            val manager = context.getSystemService(AlarmManager::class.java) ?: return
            val showIntent = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try {
                if (PermissionChecker.canScheduleExactAlarms(context)) {
                    manager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent), operation)
                } else {
                    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
                }
            } catch (e: SecurityException) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
            }
        }

        /** "Keyinroq" bosilganda — N daqiqadan keyin xuddi shu budilnik qayta chaladi. */
        fun scheduleSnooze(context: Context, alarm: RingingAlarm, minutes: Int) {
            val intent = Intent(context, AlarmReceiver::class.java)
                .setAction(ACTION_SNOOZED)
                .putExtra(EXTRA_TITLE, alarm.title)
                .putExtra(EXTRA_TEXT, alarm.text)
                .putExtra(EXTRA_SOUND, alarm.soundUri)
            val pending = PendingIntent.getBroadcast(
                context, SNOOZE_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setAlarmClock(context, System.currentTimeMillis() + minutes * 60_000L, pending)
        }
    }
}
