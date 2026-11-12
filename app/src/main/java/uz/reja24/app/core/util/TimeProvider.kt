package uz.reja24.app.core.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/** Joriy vaqt manbai. Testlarda soxta vaqt berish uchun interfeys qilingan. */
interface TimeProvider {
    fun now(): LocalDateTime
    fun currentMillis(): Long
    fun today(): LocalDate = now().toLocalDate()
    fun zone(): ZoneId = ZoneId.systemDefault()

    /** LocalDateTime → epoch millisekund (budilnik uchun). */
    fun toMillis(dateTime: LocalDateTime): Long = dateTime.atZone(zone()).toInstant().toEpochMilli()
}

class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun now(): LocalDateTime = LocalDateTime.now()
    override fun currentMillis(): Long = System.currentTimeMillis()
}

/** Soat va sanalarni formatlash. Raqamlar har doim lotincha (0–9), arab tilida ham. */
object TimeFormat {
    private val HOUR_MINUTE: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

    fun time(time: LocalTime): String = time.format(HOUR_MINUTE)

    /** Masalan "chorshanba" (til bo'yicha). */
    fun weekday(date: LocalDate, locale: Locale): String =
        date.format(DateTimeFormatter.ofPattern("EEEE", locale))

    fun shortWeekday(date: LocalDate, locale: Locale): String =
        date.format(DateTimeFormatter.ofPattern("EEE", locale))

    /** Masalan "24 sentabr". */
    fun dayMonth(date: LocalDate, locale: Locale): String =
        date.format(DateTimeFormatter.ofPattern("d MMMM", locale))

    /** Masalan "sentabr 2026". Oy nomi yakka holda (LLLL). */
    fun monthYear(date: LocalDate, locale: Locale): String =
        date.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))

    /** Qolgan vaqt: "1:05" (soat:daqiqa) yoki "12:30" (daqiqa:soniya emas). */
    fun hoursMinutes(totalMinutes: Long): String {
        val safe = totalMinutes.coerceAtLeast(0)
        return String.format(Locale.ROOT, "%d:%02d", safe / 60, safe % 60)
    }
}
