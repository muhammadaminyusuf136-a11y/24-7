package uz.reja24.app.presentation.components

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import uz.reja24.app.R
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

/** To'liq sanalar: kun, oy nomi va yil — qisqartirishsiz. */
/** Hijriy sanani ±1 kunga to'g'rilash (Namoz vaqtlari sozlamasidan) — butun ilova uchun bitta qiymat. */
val LocalHijriAdjustment = staticCompositionLocalOf { 0 }

/** "16 Rabiul oxir 1448-yil" — joriy to'g'rilash bilan. Taqvim chegarasidan tashqarida — null. */
@Composable
fun hijriDateText(date: LocalDate): String? =
    FullDateText.hijri(LocalContext.current.resources, date, LocalHijriAdjustment.current)

/** Davr uchun hijriy sanalar: "20–26 Jumodul oxir 1448-yil" yoki "28 Jumodul avval – 4 Jumodul oxir 1448-yil". */
@Composable
fun hijriRangeText(from: LocalDate, to: LocalDate): String? =
    FullDateText.hijriRange(LocalContext.current.resources, from, to, LocalHijriAdjustment.current)

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
