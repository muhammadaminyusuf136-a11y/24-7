package uz.reja24.app.domain.prayer

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.tan

/** Kunlik vaqtlar: 5 namoz + quyosh chiqishi (bomdod vaqtining oxiri). */
enum class Prayer(val isObligatory: Boolean) {
    FAJR(true),
    SUNRISE(false),
    DHUHR(true),
    ASR(true),
    MAGHRIB(true),
    ISHA(true);

    companion object {
        val OBLIGATORY: List<Prayer> = entries.filter { it.isObligatory }
        fun fromName(name: String?): Prayer? = entries.firstOrNull { it.name == name }
    }
}

/**
 * Hisoblash usuli: bomdod va xufton uchun quyosh ufqdan necha daraja pastda bo'lishi.
 * ishaMinutes != null bo'lsa — xufton shomdan shuncha daqiqa keyin (Ummul-Quro).
 */
enum class CalculationMethod(val fajrAngle: Double, val ishaAngle: Double, val ishaMinutes: Int? = null) {
    MWL(18.0, 17.0),
    EGYPT(19.5, 17.5),
    MAKKAH(18.5, 0.0, ishaMinutes = 90),
    KARACHI(18.0, 18.0),
    ISNA(15.0, 15.0),
    RUSSIA(16.0, 15.0),
    SOUTHEAST_ASIA(20.0, 18.0);

    companion object {
        fun fromName(name: String?): CalculationMethod? = entries.firstOrNull { it.name == name }
    }
}

/** Asr vaqti: Hanafiy — soya ikki barobar, Shofeiy (va boshqa mazhablar) — bir barobar. */
enum class AsrMadhab(val shadowFactor: Int) {
    SHAFII(1),
    HANAFI(2);

    companion object {
        fun fromName(name: String?): AsrMadhab? = entries.firstOrNull { it.name == name }
    }
}

/**
 * Namoz vaqtlarini astronomik formula bilan hisoblaydi (internet kerak emas).
 * Algoritm — PrayTimes.org (Hamid Zarrabi-Zadeh) usuli; yuqori kengliklarda
 * (masalan, Moskvada yozda) bomdod/xufton "burchakka asoslangan" usulda aniqlanadi.
 * Natija — daqiqagacha yaxlitlangan aniq lahza, telefon vaqt zonasiga o'girib beriladi.
 */
object PrayerCalculator {

    private const val RISE_SET_ANGLE = 0.833

    fun calculate(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        zone: ZoneId,
        method: CalculationMethod,
        madhab: AsrMadhab,
        adjustmentsMinutes: Map<Prayer, Int> = emptyMap(),
        isRamadan: Boolean = false
    ): Map<Prayer, LocalDateTime> {
        val jDate = julian(date.year, date.monthValue, date.dayOfMonth) - longitude / (15.0 * 24.0)
        val ctx = Context(jDate, latitude)

        // Boshlang'ich taxminiy vaqtlar (soatda), keyin bir marta aniqlashtiriladi
        val fajr = ctx.sunAngleTime(method.fajrAngle, 5.0 / 24, ccw = true)
        val sunrise = ctx.sunAngleTime(RISE_SET_ANGLE, 6.0 / 24, ccw = true)
        val dhuhr = ctx.midDay(12.0 / 24)
        val asr = ctx.asrTime(madhab.shadowFactor, 13.0 / 24)
        val sunset = ctx.sunAngleTime(RISE_SET_ANGLE, 18.0 / 24, ccw = false)
        val ishaByAngle = ctx.sunAngleTime(method.ishaAngle, 18.0 / 24, ccw = false)

        // UTC soatlarga o'tkazish
        val shift = -longitude / 15.0
        val sunriseUtc = sunrise + shift
        val sunsetUtc = sunset + shift
        val night = fixHour(sunriseUtc - sunsetUtc)

        val fajrUtc = adjustHighLatitude(fajr + shift, sunriseUtc, method.fajrAngle, night, ccw = true)
        val maghribUtc = sunsetUtc
        val ishaUtc = method.ishaMinutes?.let { minutes ->
            maghribUtc + (if (isRamadan) minutes + 30 else minutes) / 60.0
        } ?: adjustHighLatitude(ishaByAngle + shift, sunsetUtc, method.ishaAngle, night, ccw = false)

        val hours = mapOf(
            Prayer.FAJR to fajrUtc,
            Prayer.SUNRISE to sunriseUtc,
            Prayer.DHUHR to dhuhr + shift,
            Prayer.ASR to asr + shift,
            Prayer.MAGHRIB to maghribUtc,
            Prayer.ISHA to ishaUtc
        )
        val midnightUtc = date.atStartOfDay().toInstant(ZoneOffset.UTC)
        return hours.mapValues { (prayer, utcHours) ->
            val minutes = (utcHours * 60.0).roundToLong() + (adjustmentsMinutes[prayer] ?: 0)
            LocalDateTime.ofInstant(midnightUtc.plusSeconds(minutes * 60), zone)
        }
    }

    /** Quyosh hech qachon kerakli burchakka tushmasa (yoki juda kech tushsa) — tunning bir qismi olinadi. */
    private fun adjustHighLatitude(time: Double, base: Double, angle: Double, night: Double, ccw: Boolean): Double {
        val portion = angle / 60.0 * night
        val diff = if (ccw) fixHour(base - time) else fixHour(time - base)
        return if (time.isNaN() || diff > portion) {
            if (ccw) base - portion else base + portion
        } else {
            time
        }
    }

    private class Context(val jDate: Double, val latitude: Double) {

        fun midDay(dayPortion: Double): Double {
            val eqt = sunPosition(jDate + dayPortion).equation
            return fixHour(12.0 - eqt)
        }

        fun sunAngleTime(angle: Double, dayPortion: Double, ccw: Boolean): Double {
            val decl = sunPosition(jDate + dayPortion).declination
            val noon = midDay(dayPortion)
            val cosT = (-dsin(angle) - dsin(decl) * dsin(latitude)) / (dcos(decl) * dcos(latitude))
            if (cosT < -1.0 || cosT > 1.0) return Double.NaN
            val t = darccos(cosT) / 15.0
            return noon + if (ccw) -t else t
        }

        fun asrTime(factor: Int, dayPortion: Double): Double {
            val decl = sunPosition(jDate + dayPortion).declination
            val angle = -darccot(factor + dtan(abs(latitude - decl)))
            return sunAngleTime(angle, dayPortion, ccw = false)
        }
    }

    private data class SunPosition(val declination: Double, val equation: Double)

    private fun sunPosition(jd: Double): SunPosition {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = darctan2(dcos(e) * dsin(l), dcos(l)) / 15.0
        val eqt = q / 15.0 - fixHour(ra)
        val decl = darcsin(dsin(e) * dsin(l))
        return SunPosition(decl, eqt)
    }

    private fun julian(yearIn: Int, monthIn: Int, day: Int): Double {
        var year = yearIn
        var month = monthIn
        if (month <= 2) {
            year -= 1
            month += 12
        }
        val a = floor(year / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (year + 4716)) + floor(30.6001 * (month + 1)) + day + b - 1524.5
    }

    private fun dsin(d: Double) = sin(Math.toRadians(d))
    private fun dcos(d: Double) = cos(Math.toRadians(d))
    private fun dtan(d: Double) = tan(Math.toRadians(d))
    private fun darcsin(x: Double) = Math.toDegrees(asin(x))
    private fun darccos(x: Double) = Math.toDegrees(acos(x))
    private fun darctan2(y: Double, x: Double) = Math.toDegrees(atan2(y, x))
    private fun darccot(x: Double) = Math.toDegrees(atan(1.0 / x))
    private fun fixAngle(a: Double) = fix(a, 360.0)
    private fun fixHour(a: Double) = fix(a, 24.0)
    private fun fix(a: Double, b: Double): Double {
        val r = a - b * floor(a / b)
        return if (r < 0) r + b else r
    }
}
