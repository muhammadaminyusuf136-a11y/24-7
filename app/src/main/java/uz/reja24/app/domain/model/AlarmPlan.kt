package uz.reja24.app.domain.model

import uz.reja24.app.domain.prayer.Prayer
import uz.reja24.app.domain.prayer.PrayerDay
import uz.reja24.app.domain.prayer.PrayerSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Rejim eslatmalari turlari. */
enum class RoutineAlarmKind {
    STAGE_1,
    STAGE_2,
    STAGE_3,
    FINAL_WARNING,
    BLOCK_START,
    WAKE_UP
}

/** Reja eslatmalari turlari. */
enum class TaskAlarmKind {
    REMINDER,   // boshlanishidan N daqiqa oldin
    START,      // boshlanganda
    END,        // tugaganda (bajarilmagan bo'lsa — yumshoq xabar)
    REMINDER_2  // ikkinchi eslatma (sozlamalarda: 5 / 10 / 15 daqiqa oldin). Oxirida — so'rov kodlari o'zgarmasin
}

data class PlannedTaskAlarm(
    val entryId: Long,
    val kind: TaskAlarmKind,
    val dayIndex: Int,          // 0 — bugun, 1 — ertaga
    val triggerAt: LocalDateTime,
    /** Oddiy bildirishnoma emas — o'chirilguncha chaladigan budilnik. */
    val rings: Boolean = false
)

/** Namoz eslatmalari turlari. */
enum class PrayerAlarmKind {
    REMINDER,   // vaqt kirishidan N daqiqa oldin
    AT_TIME,    // vaqt kirganda (bildirishnoma)
    ALARM       // vaqt kirganda budilnik
}

data class PlannedPrayerAlarm(
    val prayer: Prayer,
    val kind: PrayerAlarmKind,
    val dayIndex: Int,          // 0 — bugun, 1 — ertaga
    val triggerAt: LocalDateTime
)

/** Qachon qaysi eslatma chalinishini hisoblaydi (sof mantiq — testlanadi). */
object AlarmPlanner {

    /** now dan keyingi eng yaqin `time` (bugun yoki ertaga). */
    fun nextOccurrence(now: LocalDateTime, time: LocalTime): LocalDateTime {
        val today = now.toLocalDate().atTime(time)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    /** Rejim eslatmalari: bloklashdan 60/30/15/1 daqiqa oldin, bloklash va uyg'onish. */
    fun routineAlarms(now: LocalDateTime, settings: RoutineSettings): Map<RoutineAlarmKind, LocalDateTime> {
        if (!settings.isRoutineActive) return emptyMap()
        val block = settings.blockStartTime
        val result = linkedMapOf<RoutineAlarmKind, LocalDateTime>()
        fun beforeBlock(kind: RoutineAlarmKind, minutes: Int) {
            if (minutes > 0) result[kind] = nextOccurrence(now, block.minusMinutes(minutes.toLong()))
        }
        beforeBlock(RoutineAlarmKind.STAGE_1, settings.reminderStage1Minutes)
        beforeBlock(RoutineAlarmKind.STAGE_2, settings.reminderStage2Minutes)
        beforeBlock(RoutineAlarmKind.STAGE_3, settings.reminderStage3Minutes)
        beforeBlock(RoutineAlarmKind.FINAL_WARNING, settings.finalWarningMinutes)
        result[RoutineAlarmKind.BLOCK_START] = nextOccurrence(now, block)
        result[RoutineAlarmKind.WAKE_UP] = nextOccurrence(now, settings.wakeTime)
        return result
    }

    /**
     * Bugun va ertangi rejalar uchun eslatmalar (faqat kelajakdagilari).
     * tasksByDay: 0 — bugungi, 1 — ertangi rejalar.
     */
    fun taskAlarms(
        now: LocalDateTime,
        today: LocalDate,
        tasksByDay: List<List<ScheduleEntry>>,
        /** Ikkinchi eslatma (0 — yo'q). Birinchisi bilan bir xil bo'lsa — takrorlanmaydi. */
        secondReminderMinutes: Int = 0
    ): List<PlannedTaskAlarm> {
        val result = mutableListOf<PlannedTaskAlarm>()
        tasksByDay.forEachIndexed { dayIndex, entries ->
            val date = today.plusDays(dayIndex.toLong())
            entries.forEach { entry ->
                val start = date.atTime(entry.startTime)
                val end = start.plusMinutes(entry.durationMinutes.toLong())
                if (entry.reminderMinutesBefore > 0) {
                    result += PlannedTaskAlarm(
                        entry.id, TaskAlarmKind.REMINDER, dayIndex,
                        start.minusMinutes(entry.reminderMinutesBefore.toLong())
                    )
                }
                if (secondReminderMinutes > 0 && secondReminderMinutes != entry.reminderMinutesBefore) {
                    result += PlannedTaskAlarm(
                        entry.id, TaskAlarmKind.REMINDER_2, dayIndex,
                        start.minusMinutes(secondReminderMinutes.toLong())
                    )
                }
                result += PlannedTaskAlarm(entry.id, TaskAlarmKind.START, dayIndex, start, rings = entry.alarmEnabled)
                result += PlannedTaskAlarm(entry.id, TaskAlarmKind.END, dayIndex, end)
            }
        }
        return result.filter { it.triggerAt.isAfter(now) }
    }

    /**
     * Namoz eslatmalari (bugun va ertaga, faqat kelajakdagilari).
     * Budilnik yoqilgan namoz uchun "vaqt kirdi" bildirishnomasi alohida chiqmaydi — budilnik o'zi yetarli.
     */
    fun prayerAlarms(now: LocalDateTime, days: List<PrayerDay>, settings: PrayerSettings): List<PlannedPrayerAlarm> {
        if (!settings.isEnabled) return emptyList()
        val result = mutableListOf<PlannedPrayerAlarm>()
        days.forEachIndexed { dayIndex, day ->
            Prayer.OBLIGATORY.forEach { prayer ->
                val at = day.time(prayer)
                val rings = prayer in settings.alarmPrayers
                val muted = prayer in settings.mutedPrayers
                if (!muted && settings.reminderMinutes > 0) {
                    result += PlannedPrayerAlarm(
                        prayer, PrayerAlarmKind.REMINDER, dayIndex,
                        at.minusMinutes(settings.reminderMinutes.toLong())
                    )
                }
                when {
                    rings -> result += PlannedPrayerAlarm(prayer, PrayerAlarmKind.ALARM, dayIndex, at)
                    !muted && settings.notifyAtTime -> result += PlannedPrayerAlarm(prayer, PrayerAlarmKind.AT_TIME, dayIndex, at)
                }
            }
        }
        return result.filter { it.triggerAt.isAfter(now) }
    }
}
