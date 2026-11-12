package uz.reja24.app.domain.prayer

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Natijalar Aladhan xizmati (api.aladhan.com, latitudeAdjustmentMethod=3) bilan solishtirilgan.
 * Yaxlitlash farqi uchun ±1 daqiqa ruxsat etiladi.
 */
class PrayerCalculatorTest {

    private fun check(
        date: LocalDate,
        lat: Double,
        lon: Double,
        zone: String,
        method: CalculationMethod,
        madhab: AsrMadhab,
        expected: List<String>
    ) {
        val zoneId = ZoneId.of(zone)
        val result = PrayerCalculator.calculate(date, lat, lon, zoneId, method, madhab)
        Prayer.entries.forEachIndexed { index, prayer ->
            val want = LocalDateTime.of(date, LocalTime.parse(expected[index]))
            val got = result.getValue(prayer)
            val diff = Duration.between(want, got).toMinutes()
            assertTrue("$prayer: kutilgan $want, chiqdi $got", kotlin.math.abs(diff) <= 1)
        }
    }

    @Test
    fun tashkent_mwl_hanafi() = check(
        LocalDate.of(2026, 9, 27), 41.2995, 69.2401, "Asia/Tashkent",
        CalculationMethod.MWL, AsrMadhab.HANAFI,
        listOf("04:43", "06:15", "12:14", "16:26", "18:12", "19:39")
    )

    @Test
    fun tashkent_summer_mwl_hanafi() = check(
        LocalDate.of(2026, 6, 21), 41.2995, 69.2401, "Asia/Tashkent",
        CalculationMethod.MWL, AsrMadhab.HANAFI,
        listOf("02:41", "04:50", "12:25", "17:40", "20:00", "21:59")
    )

    @Test
    fun cairo_egypt_shafii() = check(
        LocalDate.of(2026, 9, 27), 30.0444, 31.2357, "Africa/Cairo",
        CalculationMethod.EGYPT, AsrMadhab.SHAFII,
        listOf("05:19", "06:46", "12:46", "16:12", "18:46", "20:03")
    )

    @Test
    fun makkah_ummalqura() = check(
        LocalDate.of(2026, 9, 27), 21.4225, 39.8262, "Asia/Riyadh",
        CalculationMethod.MAKKAH, AsrMadhab.SHAFII,
        listOf("04:55", "06:11", "12:12", "15:36", "18:12", "19:42")
    )

    @Test
    fun moscow_summer_high_latitude() = check(
        LocalDate.of(2026, 6, 21), 55.7558, 37.6173, "Europe/Moscow",
        CalculationMethod.RUSSIA, AsrMadhab.HANAFI,
        listOf("02:02", "03:45", "12:31", "18:20", "21:18", "22:55")
    )

    @Test
    fun adjustments_are_applied() {
        val date = LocalDate.of(2026, 9, 27)
        val zone = ZoneId.of("Asia/Tashkent")
        val base = PrayerCalculator.calculate(date, 41.2995, 69.2401, zone, CalculationMethod.MWL, AsrMadhab.HANAFI)
        val tuned = PrayerCalculator.calculate(
            date, 41.2995, 69.2401, zone, CalculationMethod.MWL, AsrMadhab.HANAFI,
            adjustmentsMinutes = mapOf(Prayer.FAJR to 10, Prayer.ISHA to -3)
        )
        assertTrue(Duration.between(base.getValue(Prayer.FAJR), tuned.getValue(Prayer.FAJR)).toMinutes() == 10L)
        assertTrue(Duration.between(base.getValue(Prayer.ISHA), tuned.getValue(Prayer.ISHA)).toMinutes() == -3L)
        assertTrue(base.getValue(Prayer.DHUHR) == tuned.getValue(Prayer.DHUHR))
    }

    @Test
    fun ramadan_isha_is_two_hours_after_maghrib_in_makkah_method() {
        val result = PrayerCalculator.calculate(
            LocalDate.of(2026, 9, 27), 21.4225, 39.8262, ZoneId.of("Asia/Riyadh"),
            CalculationMethod.MAKKAH, AsrMadhab.SHAFII, isRamadan = true
        )
        val gap = Duration.between(result.getValue(Prayer.MAGHRIB), result.getValue(Prayer.ISHA)).toMinutes()
        assertTrue(gap == 120L)
    }
}
