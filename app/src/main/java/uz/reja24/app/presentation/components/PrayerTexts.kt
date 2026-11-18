package uz.reja24.app.presentation.components

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import uz.reja24.app.R
import uz.reja24.app.domain.model.DateDisplay
import uz.reja24.app.domain.prayer.AsrMadhab
import uz.reja24.app.domain.prayer.CalculationMethod
import uz.reja24.app.domain.prayer.HijriCalendar
import uz.reja24.app.domain.prayer.Prayer
import java.time.LocalDate

@StringRes
fun prayerNameRes(prayer: Prayer): Int = when (prayer) {
    Prayer.FAJR -> R.string.prayer_fajr
    Prayer.SUNRISE -> R.string.prayer_sunrise
    Prayer.DHUHR -> R.string.prayer_dhuhr
    Prayer.ASR -> R.string.prayer_asr
    Prayer.MAGHRIB -> R.string.prayer_maghrib
    Prayer.ISHA -> R.string.prayer_isha
}

@StringRes
fun methodNameRes(method: CalculationMethod): Int = when (method) {
    CalculationMethod.MWL -> R.string.method_mwl
    CalculationMethod.EGYPT -> R.string.method_egypt
    CalculationMethod.MAKKAH -> R.string.method_makkah
    CalculationMethod.KARACHI -> R.string.method_karachi
    CalculationMethod.ISNA -> R.string.method_isna
    CalculationMethod.RUSSIA -> R.string.method_russia
    CalculationMethod.SOUTHEAST_ASIA -> R.string.method_southeast_asia
}

@StringRes
fun madhabNameRes(madhab: AsrMadhab): Int = when (madhab) {
    AsrMadhab.HANAFI -> R.string.madhab_hanafi
    AsrMadhab.SHAFII -> R.string.madhab_shafii
}

/** Sanalar qanday ko'rsatiladi (Sozlamalar → Sana) va hijriy sanani ±kun to'g'rilash. */
data class DateStyle(val display: DateDisplay = DateDisplay.BOTH, val hijriAdjustment: Int = 0) {
    val showGregorian: Boolean get() = display != DateDisplay.HIJRI
    val showHijri: Boolean get() = display != DateDisplay.GREGORIAN
    val showBoth: Boolean get() = display == DateDisplay.BOTH
}

/** Butun ilova uchun bitta qiymat (MainActivity'da beriladi). */
val LocalDateStyle = staticCompositionLocalOf { DateStyle() }

/** "16 Rabiul oxir 1448-yil" — hijriy ko'rsatilsa. "Faqat milodiy" tanlanganda yoki taqvimdan tashqarida — null. */
@Composable
fun hijriDateText(date: LocalDate): String? {
    val style = LocalDateStyle.current
    if (!style.showHijri) return null
    return FullDateText.hijri(LocalContext.current.resources, date, style.hijriAdjustment)
}

/** Asosiy sana: milodiy "17 noyabr 2026-yil"; "faqat hijriy" tanlanganda — hijriy. */
@Composable
fun mainDateText(date: LocalDate): String {
    val style = LocalDateStyle.current
    val resources = LocalContext.current.resources
    if (!style.showGregorian) FullDateText.hijri(resources, date, style.hijriAdjustment)?.let { return it }
    return FullDateText.gregorian(resources, date)
}

/** Asosiy sana ostidagi ikkinchi sana — faqat "ikkalasi" tanlanganda (hijriy). */
@Composable
fun secondDateText(date: LocalDate): String? =
    if (LocalDateStyle.current.showBoth) hijriDateText(date) else null

/** Davr sarlavhasi: milodiy matn (tayyor holda beriladi) yoki "faqat hijriy"da — hijriy davr. */
@Composable
fun mainRangeText(from: LocalDate, to: LocalDate, gregorian: String): String {
    val style = LocalDateStyle.current
    if (!style.showGregorian) {
        FullDateText.hijriRange(LocalContext.current.resources, from, to, style.hijriAdjustment)?.let { return it }
    }
    return gregorian
}

/** Davr sarlavhasi ostidagi hijriy davr — faqat "ikkalasi" tanlanganda. */
@Composable
fun secondRangeText(from: LocalDate, to: LocalDate): String? {
    val style = LocalDateStyle.current
    if (!style.showBoth) return null
    return FullDateText.hijriRange(LocalContext.current.resources, from, to, style.hijriAdjustment)
}

/**
 * Kalendar katagi uchun kun raqamlari: katta raqam (milodiy, "faqat hijriy"da — hijriy)
 * va "ikkalasi"da ostida kichik, boshqa rangdagi hijriy kun.
 */
data class DayNumbers(val main: String, val hijriSmall: String?)

@Composable
fun dayNumbers(date: LocalDate): DayNumbers {
    val style = LocalDateStyle.current
    val hijriDay = HijriCalendar.from(date, style.hijriAdjustment)?.day?.toString()
    return when {
        style.showBoth -> DayNumbers(date.dayOfMonth.toString(), hijriDay)
        style.showGregorian -> DayNumbers(date.dayOfMonth.toString(), null)
        else -> DayNumbers(hijriDay ?: date.dayOfMonth.toString(), null)
    }
}

object FullDateText {

    /** Masalan "27 sentabr 2026-yil". */
    fun gregorian(resources: Resources, date: LocalDate): String {
        val months = resources.getStringArray(R.array.gregorian_months)
        return resources.getString(
            R.string.date_full,
            date.dayOfMonth.toString(),
            months[date.monthValue - 1],
            date.year.toString()
        )
    }

    /** Davr: bir oy ichida "20–26 Jumodul oxir 1448-yil", aks holda ikkala chekka to'liq. */
    fun hijriRange(resources: Resources, from: LocalDate, to: LocalDate, adjustmentDays: Int): String? {
        val a = HijriCalendar.from(from, adjustmentDays) ?: return null
        val b = HijriCalendar.from(to, adjustmentDays) ?: return null
        val months = resources.getStringArray(R.array.hijri_months)
        return if (a.month == b.month && a.year == b.year) {
            resources.getString(R.string.date_full, a.day.toString() + "–" + b.day, months[b.month - 1], b.year.toString())
        } else {
            a.day.toString() + " " + months[a.month - 1] + " – " +
                resources.getString(R.string.date_full, b.day.toString(), months[b.month - 1], b.year.toString())
        }
    }

    /** Masalan "16 Rabiul oxir 1448-yil". Taqvim chegarasidan tashqarida — null. */
    fun hijri(resources: Resources, date: LocalDate, adjustmentDays: Int): String? {
        val hijri = HijriCalendar.from(date, adjustmentDays) ?: return null
        val months = resources.getStringArray(R.array.hijri_months)
        return resources.getString(
            R.string.date_full,
            hijri.day.toString(),
            months[hijri.month - 1],
            hijri.year.toString()
        )
    }
}
