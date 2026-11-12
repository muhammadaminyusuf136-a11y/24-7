package uz.reja24.app.domain.prayer

import java.time.DateTimeException
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

data class HijriDate(val day: Int, val month: Int, val year: Int)

/**
 * Hijriy sana — Ummul-Quro taqvimi (Android va Java'ga o'rnatilgan).
 * Mahalliy e'lon (oy ko'rinishi) bilan 1 kun farq bo'lishi mumkin — shuning uchun
 * sozlamalarda ±kun tuzatish bor.
 */
object HijriCalendar {

    const val RAMADAN = 9

    fun from(date: LocalDate, adjustmentDays: Int = 0): HijriDate? = try {
        val hijrah = HijrahDate.from(date.plusDays(adjustmentDays.toLong()))
        HijriDate(
            day = hijrah.get(ChronoField.DAY_OF_MONTH),
            month = hijrah.get(ChronoField.MONTH_OF_YEAR),
            year = hijrah.get(ChronoField.YEAR)
        )
    } catch (e: DateTimeException) {
        // Taqvim chegarasidan tashqarida (1300–1600 hijriy yillar) — sana ko'rsatilmaydi
        null
    }
}
