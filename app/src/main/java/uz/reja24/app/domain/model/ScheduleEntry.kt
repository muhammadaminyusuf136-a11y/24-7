package uz.reja24.app.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/**
 * Kunlik reja yozuvi.
 *
 * Agar endTime < startTime bo'lsa, reja yarim tundan o'tadi
 * (masalan uyqu 23:00 → 06:00). U boshlangan kunga tegishli hisoblanadi.
 *
 * specificDate:
 *  - ONCE uchun — aynan shu kun;
 *  - MONTHLY / YEARLY uchun — qaysi sanada takrorlanishini bildiradi;
 *  - boshqa turlar uchun — reja shu kundan boshlab amal qiladi (null = cheklovsiz).
 */
data class ScheduleEntry(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val startTime: LocalTime,
    val endTime: LocalTime,
    val category: Category = Category.OTHER,
    val colorHex: String = category.defaultColorHex,
    val isActive: Boolean = true,
    val repeatType: RepeatType = RepeatType.ONCE,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val specificDate: LocalDate? = null,
    val reminderMinutesBefore: Int = DEFAULT_REMINDER_MINUTES,
    /** Boshlanish vaqtida budilnik: o'chirilmaguncha (yoki kechiktirilmaguncha) chaladi. */
    val alarmEnabled: Boolean = false,
    /** null — telefonning standart budilnik ovozi. */
    val alarmSoundUri: String? = null
) {
    /** Reja yarim tundan o'tadimi (masalan 23:00 → 06:00). */
    val crossesMidnight: Boolean get() = endTime < startTime

    /** Reja davomiyligi daqiqalarda (yarim tundan o'tishni hisobga olgan holda). */
    val durationMinutes: Int
        get() {
            val start = startTime.toSecondOfDay() / 60
            val end = endTime.toSecondOfDay() / 60
            return if (end > start) end - start else MINUTES_IN_DAY - start + end
        }

    /** Reja berilgan kunda bajarilishi kerakmi. */
    fun occursOn(date: LocalDate): Boolean {
        if (!isActive) return false
        return when (repeatType) {
            RepeatType.ONCE -> specificDate == date
            RepeatType.DAILY -> startsBeforeOrOn(date)
            RepeatType.WEEKDAYS -> startsBeforeOrOn(date) && date.dayOfWeek in WORK_DAYS
            RepeatType.WEEKLY -> startsBeforeOrOn(date) && date.dayOfWeek in daysOfWeek
            RepeatType.MONTHLY -> {
                val anchor = specificDate ?: return false
                if (date < anchor) return false
                // Oyda bu sana bo'lmasa (masalan 31-sana), oyning oxirgi kunida bajariladi
                val lastDay = date.with(TemporalAdjusters.lastDayOfMonth()).dayOfMonth
                date.dayOfMonth == minOf(anchor.dayOfMonth, lastDay)
            }
            RepeatType.YEARLY -> {
                val anchor = specificDate ?: return false
                if (date < anchor || date.month != anchor.month) return false
                // 29-fevral kabiso bo'lmagan yilda 28-fevralda bajariladi
                val lastDay = date.with(TemporalAdjusters.lastDayOfMonth()).dayOfMonth
                date.dayOfMonth == minOf(anchor.dayOfMonth, lastDay)
            }
        }
    }

    private fun startsBeforeOrOn(date: LocalDate): Boolean =
        specificDate == null || !date.isBefore(specificDate)

    companion object {
        const val DEFAULT_REMINDER_MINUTES = 15
        const val MINUTES_IN_DAY = 24 * 60
        val WORK_DAYS: Set<DayOfWeek> = setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        )
    }
}

/**
 * Muayyan kun uchun reja: yozuv + o'sha kuni bajarilganmi.
 * Takrorlanadigan reja har kuni alohida belgilanadi, shuning uchun
 * "bajarildi" belgisi yozuvning o'zida emas, shu yerda.
 */
data class DailyTask(
    val entry: ScheduleEntry,
    val date: LocalDate,
    val isCompleted: Boolean
)
