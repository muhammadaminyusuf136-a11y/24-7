package uz.reja24.app.service.alarm

import android.content.Context
import android.app.PendingIntent
import android.content.Intent
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import uz.reja24.app.R
import uz.reja24.app.core.util.DurationText
import uz.reja24.app.core.util.LocaleHelper
import uz.reja24.app.core.util.TimeFormat
import uz.reja24.app.core.util.TimeProvider
import uz.reja24.app.domain.model.AppLanguage
import uz.reja24.app.domain.model.PrayerAlarmKind
import uz.reja24.app.domain.model.RoutineAlarmKind
import uz.reja24.app.domain.model.RoutineSettings
import uz.reja24.app.domain.model.TaskAlarmKind
import uz.reja24.app.domain.model.VoiceGender
import uz.reja24.app.domain.prayer.Prayer
import uz.reja24.app.domain.repository.AppStateRepository
import uz.reja24.app.domain.repository.PrayerSettingsRepository
import uz.reja24.app.domain.repository.RoutineSettingsRepository
import uz.reja24.app.domain.repository.ScheduleRepository
import uz.reja24.app.domain.repository.StreakRepository
import uz.reja24.app.domain.repository.UserPreferencesRepository
import uz.reja24.app.presentation.components.prayerNameRes
import uz.reja24.app.service.DayCloser
import uz.reja24.app.service.blocking.BlockingMonitorService
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Budilnik chalinganda nima qilishni hal qiladi: xabar, ovoz, budilnik, bloklashni boshlash. */
@Singleton
class AlarmHandler @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val routineSettingsRepository: RoutineSettingsRepository,
    private val scheduleRepository: ScheduleRepository,
    private val streakRepository: StreakRepository,
    private val appStateRepository: AppStateRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val prayerSettingsRepository: PrayerSettingsRepository,
    private val alarmScheduler: AlarmScheduler,
    private val dayCloser: DayCloser,
    private val timeProvider: TimeProvider
) {

    /** Bitta eslatma uchun kerakli til va ovoz sozlamalari. */
    private class Speech(val context: Context, val locale: Locale, val gender: VoiceGender)

    suspend fun handle(intent: Intent) {
        val prefs = userPreferencesRepository.preferences.first()
        val language: AppLanguage? = if (prefs.isLanguageChosen) prefs.language else null
        val context = LocaleHelper.localizedContext(appContext, language)
        val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
        val speech = Speech(context, locale, prefs.voiceGender)
        val kindName = intent.getStringExtra(AlarmScheduler.EXTRA_KIND)

        try {
            when (intent.action) {
                AlarmScheduler.ACTION_ROUTINE -> {
                    val kind = RoutineAlarmKind.entries.firstOrNull { it.name == kindName }
                    if (kind != null) handleRoutine(speech, kind)
                }
                AlarmScheduler.ACTION_TASK -> {
                    val kind = TaskAlarmKind.entries.firstOrNull { it.name == kindName }
                    val entryId = intent.getLongExtra(AlarmScheduler.EXTRA_ENTRY_ID, 0L)
                    val date = LocalDate.ofEpochDay(
                        intent.getLongExtra(AlarmScheduler.EXTRA_DATE, timeProvider.today().toEpochDay())
                    )
                    if (kind != null && entryId > 0) handleTask(speech, kind, entryId, date)
                }
                AlarmScheduler.ACTION_PRAYER -> {
                    val kind = PrayerAlarmKind.entries.firstOrNull { it.name == kindName }
                    val prayer = Prayer.fromName(intent.getStringExtra(AlarmScheduler.EXTRA_PRAYER))
                    val atMillis = intent.getLongExtra(AlarmScheduler.EXTRA_PRAYER_AT, 0L)
                    if (kind != null && prayer != null && atMillis > 0) handlePrayer(speech, kind, prayer, atMillis)
                }
                AlarmScheduler.ACTION_SNOOZED -> AlarmRingService.start(
                    appContext,
                    title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE).orEmpty(),
                    text = intent.getStringExtra(AlarmScheduler.EXTRA_TEXT).orEmpty(),
                    soundUri = intent.getStringExtra(AlarmScheduler.EXTRA_SOUND)
                )
            }
        } finally {
            // Keyingi eslatmalarni har doim qayta qo'yamiz
            alarmScheduler.rescheduleAll()
        }
    }

    private suspend fun handleRoutine(speech: Speech, kind: RoutineAlarmKind) {
        val settings = routineSettingsRepository.getSettings()
        if (!settings.isRoutineActive) return
        val context = speech.context
        val res = context.resources
        val title = context.getString(R.string.app_name)

        when (kind) {
            RoutineAlarmKind.STAGE_1 -> alert(speech, settings, NotificationHelper.ID_ROUTINE, title,
                context.getString(R.string.reminder_stage1, DurationText.format(res, settings.reminderStage1Minutes)))
            RoutineAlarmKind.STAGE_2 -> alert(speech, settings, NotificationHelper.ID_ROUTINE, title,
                context.getString(R.string.reminder_stage2, DurationText.format(res, settings.reminderStage2Minutes)))
            RoutineAlarmKind.STAGE_3 -> {
                // Oxirgi eslatma: xabar + kechki muhosaba oynasi (kunni hisob qilish)
                val text = context.getString(R.string.reminder_stage3, DurationText.format(res, settings.reminderStage3Minutes)) +
                    " " + context.getString(R.string.muhosaba_reminder_suffix)
                openMuhosaba()
                alert(speech, settings, NotificationHelper.ID_ROUTINE, title, text, NotificationHelper.openMuhosabaIntent(context))
            }
            RoutineAlarmKind.FINAL_WARNING -> alert(speech, settings, NotificationHelper.ID_ROUTINE, title,
                context.getString(R.string.reminder_final, DurationText.format(res, settings.finalWarningMinutes)))
            RoutineAlarmKind.BLOCK_START -> {
                appStateRepository.setBypassUntilMillis(0L)
                BlockingMonitorService.start(appContext)
                val message = res.getStringArray(R.array.block_messages).random()
                alert(speech, settings, NotificationHelper.ID_ROUTINE,
                    context.getString(R.string.block_started_title), message)
            }
            RoutineAlarmKind.WAKE_UP -> {
                appStateRepository.setBypassUntilMillis(0L)
                // Kechagi "rejim kuni" shu daqiqada tugadi — avval uni baholaymiz, keyin streak aniq bo'ladi
                runCatching { dayCloser.closeFinishedDays(notifyTrophy = true) }
                val streak = streakRepository.getSummary(timeProvider.today()).current
                val text = if (streak > 0) {
                    context.getString(R.string.morning_streak, DurationText.days(res, streak))
                } else {
                    context.getString(R.string.morning_new_day)
                }
                alert(speech, settings, NotificationHelper.ID_MORNING,
                    context.getString(R.string.morning_title), text)
            }
        }
    }

    private suspend fun handleTask(
        speech: Speech,
        kind: TaskAlarmKind,
        entryId: Long,
        date: LocalDate
    ) {
        val entry = scheduleRepository.getEntry(entryId) ?: return
        if (!entry.isActive || !entry.occursOn(date)) return
        val settings = routineSettingsRepository.getSettings()
        val context = speech.context
        val res = context.resources
        val notificationId = NotificationHelper.taskNotificationId(entryId)

        val prefs = userPreferencesRepository.preferences.first()
        when (kind) {
            TaskAlarmKind.REMINDER, TaskAlarmKind.REMINDER_2 -> {
                val minutes = if (kind == TaskAlarmKind.REMINDER) entry.reminderMinutesBefore else prefs.taskSecondReminderMinutes
                val left = DurationText.format(res, minutes)
                val text = context.getString(R.string.task_reminder, TimeFormat.time(entry.startTime), entry.title, left)
                // Ovozda: "Sport boshlanishiga 10 daqiqa qoldi." + izoh (masalan "Yo'lga chiq") yoki "Tayyorlaning."
                val spoken = context.getString(R.string.task_voice_reminder, entry.title, left) + " " +
                    entry.description.trim().ifEmpty { context.getString(R.string.task_voice_ready) }
                alert(speech, settings, notificationId, entry.title, text, voice = spoken.takeIf { prefs.taskVoiceOn })
            }
            TaskAlarmKind.START -> {
                val template = res.getStringArray(R.array.task_start_messages).random()
                val text = String.format(Locale.ROOT, template, entry.title)
                if (entry.alarmEnabled) {
                    AlarmRingService.start(appContext, entry.title, text, entry.alarmSoundUri)
                } else {
                    alert(speech, settings, notificationId, entry.title, text, voice = text.takeIf { prefs.taskVoiceOn })
                }
            }
            TaskAlarmKind.END -> {
                val completed = scheduleRepository.getTasksForDate(date)
                    .firstOrNull { it.entry.id == entryId }?.isCompleted ?: false
                if (!completed) {
                    NotificationHelper.show(context, notificationId, entry.title,
                        res.getStringArray(R.array.missed_messages).random())
                }
            }
        }
    }

    private suspend fun handlePrayer(speech: Speech, kind: PrayerAlarmKind, prayer: Prayer, atMillis: Long) {
        val prayerSettings = prayerSettingsRepository.getSettings()
        if (!prayerSettings.isEnabled) return
        val context = speech.context
        val name = context.getString(prayerNameRes(prayer))
        val time = TimeFormat.time(Instant.ofEpochMilli(atMillis).atZone(timeProvider.zone()).toLocalTime())
        val id = NotificationHelper.prayerNotificationId(prayer.ordinal)
        val routine = routineSettingsRepository.getSettings()
        when (kind) {
            PrayerAlarmKind.REMINDER -> alert(
                speech, routine, id, name,
                context.getString(
                    R.string.prayer_reminder_text, name,
                    DurationText.format(context.resources, prayerSettings.reminderMinutes), time
                )
            )
            PrayerAlarmKind.AT_TIME -> alert(speech, routine, id, name, context.getString(R.string.prayer_at_time_text, name, time))
            PrayerAlarmKind.ALARM -> AlarmRingService.start(
                appContext, name, context.getString(R.string.prayer_at_time_text, name, time), prayerSettings.alarmSoundUri
            )
            PrayerAlarmKind.AZKAR -> {
                if (!prayerSettings.azkarOn) return
                val title = context.getString(
                    if (prayer == Prayer.FAJR) R.string.azkar_morning_title else R.string.azkar_evening_title
                )
                // Har safar boshqa oyat / hadis / ulamolar so'zi
                val quotes = context.resources.getStringArray(R.array.azkar_quotes)
                val quote = quotes[Math.floorMod((atMillis / 86_400_000L).toInt() * 2 + prayer.ordinal, quotes.size)]
                alert(
                    speech, routine, NotificationHelper.prayerNotificationId(prayer.ordinal) + AZKAR_ID_SHIFT, title, quote,
                    voice = title.takeIf { routine.isVoiceReminderOn }
                )
            }
        }
    }

    /**
     * Kechki muhosaba oynasini ekranga chiqaradi. Fondan oyna ochishga Android faqat
     * "Ekran ustida ko'rsatish" ruxsati bor ilovalarga yo'l qo'yadi; ruxsat bo'lmasa —
     * bildirishnoma bosilganda ochiladi.
     */
    private fun openMuhosaba() {
        if (!Settings.canDrawOverlays(appContext)) return
        runCatching { appContext.startActivity(NotificationHelper.muhosabaIntent(appContext)) }
    }

    /** Bildirishnoma + (ixtiyoriy) tanlangan rington + (ixtiyoriy) ovozli o'qish. */
    private suspend fun alert(
        speech: Speech,
        settings: RoutineSettings,
        id: Int,
        title: String,
        text: String,
        contentIntent: PendingIntent? = null,
        /** Ovoz chiqarib o'qiladigan matn (null — o'qilmaydi). Rejim va namozda — umumiy "Ovozli eslatma" sozlamasi. */
        voice: String? = text.takeIf { settings.isVoiceReminderOn }
    ) {
        val context = speech.context
        val customSound = settings.ringtoneUri != null
        NotificationHelper.show(
            context, id, title, text,
            channel = if (customSound) NotificationHelper.CHANNEL_REMINDERS_SILENT else NotificationHelper.CHANNEL_REMINDERS,
            contentIntent = contentIntent ?: NotificationHelper.openAppIntent(context)
        )
        if (customSound) ReminderSound.playRingtone(context, settings.ringtoneUri)
        if (voice != null) ReminderSound.speak(context, voice, speech.locale, speech.gender)
    }

    private companion object {
        /** Zikr bildirishnomasi namoz bildirishnomasini almashtirib yubormasin. */
        const val AZKAR_ID_SHIFT = 50
    }
}
