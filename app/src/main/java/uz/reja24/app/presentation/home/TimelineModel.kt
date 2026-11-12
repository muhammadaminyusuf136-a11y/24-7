package uz.reja24.app.presentation.home

import uz.reja24.app.domain.model.DailyTask
import uz.reja24.app.domain.model.RoutineSettings
import uz.reja24.app.domain.prayer.Prayer
import uz.reja24.app.domain.prayer.PrayerDay
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Lentadagi bitta blok. Vaqt — lenta boshidan (kechagi 00:00) o'tgan daqiqalar.
 * prayer != null — namoz vaqti belgisi; title == null va prayer == null — uyqu rejimi bloki.
 */
data class TimelineBlock(
    val key: String,
    val title: String?,
    val colorHex: String?,
    val startMinute: Int,
    val endMinute: Int,
    val isCompleted: Boolean,
    val prayer: Prayer? = null
) {
    val isPrayer: Boolean get() = prayer != null
    val isSleep: Boolean get() = title == null && prayer == null
    fun contains(minute: Float): Boolean = minute >= startMinute && minute < endMinute
}

/** Lenta: kecha + bugun + ertaga (72 soat). Sof mantiq — testlanadi. */
object TimelineModel {

    const val MINUTES_IN_DAY = 24 * 60
    const val DAYS = 3
    const val TRACK_MINUTES = MINUTES_IN_DAY * DAYS

    /** Namoz belgisi lentada shuncha daqiqa joy egallaydi (taxminiy namoz davomiyligi). */
    const val PRAYER_MINUTES = 20
    const val PRAYER_COLOR = "#2B7A5E"

    /** Lenta qaysi kundan boshlanadi (kecha). */
    fun trackStart(today: LocalDate): LocalDate = today.minusDays(1)

    fun build(
        trackStart: LocalDate,
        tasksByDate: Map<LocalDate, List<DailyTask>>,
        settings: RoutineSettings,
        prayerDays: Map<LocalDate, PrayerDay> = emptyMap()
    ): List<TimelineBlock> {
        val result = mutableListOf<TimelineBlock>()

        // Uyqu rejimi bloklari (avval — rejalar ularning ustida chiziladi)
        if (settings.isRoutineActive) {
            val length = circularLength(settings.blockStartTime, settings.wakeTime)
            if (length > 0) {
                for (dayOffset in -1..DAYS) {
                    val start = dayOffset * MINUTES_IN_DAY + settings.blockStartTime.minuteOfDay()
                    addClipped(result, "sleep-$dayOffset", null, null, start, start + length, false)
                }
            }
        }

        // Rejalar
        tasksByDate.forEach { (date, tasks) ->
            val dayOffset = ChronoUnit.DAYS.between(trackStart, date).toInt()
            if (dayOffset !in 0 until DAYS) return@forEach
            tasks.forEach { task ->
                val start = dayOffset * MINUTES_IN_DAY + task.entry.startTime.minuteOfDay()
                addClipped(
                    result,
                    key = "task-${task.entry.id}-$date",
                    title = task.entry.title,
                    colorHex = task.entry.colorHex,
                    start = start,
                    end = start + task.entry.durationMinutes,
                    completed = task.isCompleted
                )
            }
        }

        // Namoz vaqtlari (oxirida — boshqa bloklar ustida chiziladi)
        prayerDays.forEach { (date, day) ->
            Prayer.OBLIGATORY.forEach { prayer ->
                val at = day.time(prayer)
                val offset = ChronoUnit.DAYS.between(trackStart, at.toLocalDate()).toInt()
                if (offset !in 0 until DAYS) return@forEach
                val start = offset * MINUTES_IN_DAY + at.toLocalTime().minuteOfDay()
                addClipped(result, "prayer-$prayer-$date", null, PRAYER_COLOR, start, start + PRAYER_MINUTES, false, prayer)
            }
        }
        return result
    }

    /** Berilgan daqiqada nima bor — namoz rejadan, reja uyqudan ustun. */
    fun blockAt(blocks: List<TimelineBlock>, minute: Float): TimelineBlock? {
        val hits = blocks.filter { it.contains(minute) }
        return hits.lastOrNull { it.isPrayer } ?: hits.lastOrNull { !it.isSleep } ?: hits.firstOrNull()
    }

    /** Lenta daqiqasi → kun ichidagi vaqt. */
    fun timeOf(minute: Float): LocalTime {
        val m = Math.floorMod(minute.toInt(), MINUTES_IN_DAY)
        return LocalTime.of(m / 60, m % 60)
    }

    /** Lenta daqiqasi qaysi kunda: 0 — kecha, 1 — bugun, 2 — ertaga. */
    fun dayIndexOf(minute: Float): Int = (minute / MINUTES_IN_DAY).toInt().coerceIn(0, DAYS - 1)

    private fun addClipped(
        into: MutableList<TimelineBlock>,
        key: String,
        title: String?,
        colorHex: String?,
        start: Int,
        end: Int,
        completed: Boolean,
        prayer: Prayer? = null
    ) {
        val s = start.coerceAtLeast(0)
        val e = end.coerceAtMost(TRACK_MINUTES)
        if (e > s) into += TimelineBlock(key, title, colorHex, s, e, completed, prayer)
    }

    private fun circularLength(from: LocalTime, to: LocalTime): Int =
        Math.floorMod(to.minuteOfDay() - from.minuteOfDay(), MINUTES_IN_DAY)

    private fun LocalTime.minuteOfDay(): Int = hour * 60 + minute
}
