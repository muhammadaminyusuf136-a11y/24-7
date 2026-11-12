package uz.reja24.app.data.local.db

import androidx.room.TypeConverter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Room faqat oddiy turlarni (son, matn) saqlay oladi.
 * Bu konverterlar vaqt va sanalarni songa aylantiradi va aksincha.
 */
class Converters {

    // LocalTime ↔ kun boshidan beri o'tgan daqiqalar (08:30 → 510).
    // Son bo'lgani uchun "ORDER BY startTime" to'g'ri tartiblaydi.
    @TypeConverter
    fun localTimeToMinutes(time: LocalTime?): Int? = time?.let { it.hour * 60 + it.minute }

    @TypeConverter
    fun minutesToLocalTime(minutes: Int?): LocalTime? =
        minutes?.let { LocalTime.of((it / 60).coerceIn(0, 23), (it % 60).coerceIn(0, 59)) }

    // LocalDate ↔ epoch kun (1970-01-01 dan beri kunlar). Oraliq so'rovlari uchun qulay.
    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)

    // Hafta kunlari to'plami ↔ bit-niqob: dushanba = 1, seshanba = 2, chorshanba = 4 ...
    @TypeConverter
    fun daysOfWeekToMask(days: Set<DayOfWeek>): Int =
        days.fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }

    @TypeConverter
    fun maskToDaysOfWeek(mask: Int): Set<DayOfWeek> =
        DayOfWeek.values().filterTo(mutableSetOf()) { mask and (1 shl (it.value - 1)) != 0 }
}
