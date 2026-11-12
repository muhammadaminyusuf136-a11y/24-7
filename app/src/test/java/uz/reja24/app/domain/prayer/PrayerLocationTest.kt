package uz.reja24.app.domain.prayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class PrayerLocationTest {

    private val tashkentZone = ZoneId.of("Asia/Tashkent")

    @Test
    fun without_detection_uses_time_zone_city() {
        val location = PrayerLocationResolver.resolve(PrayerSettings(), tashkentZone)!!
        assertEquals("tashkent", location.city?.id)
        assertEquals(ResolvedLocation.Source.TIME_ZONE, location.source)
    }

    @Test
    fun detected_location_near_city_shows_city_but_keeps_exact_coordinates() {
        // Yangiyo'l — Toshkentdan ~25 km, ro'yxatda alohida yo'q
        val settings = PrayerSettings(detected = DetectedLocation(41.1122, 69.0472, 0L))
        val location = PrayerLocationResolver.resolve(settings, tashkentZone)!!
        assertEquals("tashkent", location.city?.id)
        assertEquals(41.1122, location.latitude, 0.0)
        assertEquals(ResolvedLocation.Source.DETECTED, location.source)
    }

    @Test
    fun far_from_any_city_has_no_city_name() {
        // Orol dengizi atrofi — ro'yxatdagi shaharlardan uzoq
        val settings = PrayerSettings(detected = DetectedLocation(45.0, 60.0, 0L))
        val location = PrayerLocationResolver.resolve(settings, tashkentZone)!!
        assertNull(location.city)
        assertEquals("UZ", location.countryCode)
    }

    @Test
    fun old_detection_in_another_country_is_ignored_after_travel() {
        val settings = PrayerSettings(detected = DetectedLocation(41.2995, 69.2401, 0L))
        val location = PrayerLocationResolver.resolve(settings, ZoneId.of("Africa/Cairo"))!!
        assertEquals("cairo", location.city?.id)
        assertEquals(ResolvedLocation.Source.TIME_ZONE, location.source)
    }

    @Test
    fun fergana_valley_and_kyrgyzstan_travel() {
        // Marg'ilon — o'z nomi bilan
        val margilan = PrayerLocationResolver.resolve(PrayerSettings(detected = DetectedLocation(40.47, 71.72, 0L)), tashkentZone)!!
        assertEquals("margilan", margilan.city?.id)
        // Andijonga borib ilovani ochdi — Andijon
        val andijan = PrayerLocationResolver.resolve(PrayerSettings(detected = DetectedLocation(40.78, 72.34, 0L)), tashkentZone)!!
        assertEquals("andijan", andijan.city?.id)
        // Qirg'izistonga o'tdi, telefon soati Bishkekka o'tdi, ilova hali ochilmagan — eski joy emas, Bishkek
        val kg = PrayerLocationResolver.resolve(PrayerSettings(detected = DetectedLocation(40.47, 71.72, 0L)), ZoneId.of("Asia/Bishkek"))!!
        assertEquals("bishkek", kg.city?.id)
        // O'shda ilovani ochdi — aniq O'sh
        val osh = PrayerLocationResolver.resolve(PrayerSettings(detected = DetectedLocation(40.51, 72.81, 0L)), ZoneId.of("Asia/Bishkek"))!!
        assertEquals("osh", osh.city?.id)
        assertEquals(ResolvedLocation.Source.DETECTED, osh.source)
    }

    @Test
    fun manual_city_wins() {
        val makkah = Cities.byId("makkah")!!
        val settings = PrayerSettings(autoLocation = false, manualPlace = ManualPlace("Makka", makkah.latitude, makkah.longitude, "SA", "makkah"))
        val location = PrayerLocationResolver.resolve(settings, tashkentZone)!!
        assertEquals("makkah", location.city?.id)
        assertEquals(ResolvedLocation.Source.MANUAL, location.source)
        // Ro'yxatda yo'q, qidiruvda topilgan joy (Rishton) — o'z nomi va koordinatasi bilan
        val rishton = PrayerLocationResolver.resolve(
            PrayerSettings(autoLocation = false, manualPlace = ManualPlace("Rishton", 40.35667, 71.28472, "UZ")), tashkentZone
        )!!
        assertEquals(null, rishton.city)
        assertEquals("Rishton", rishton.placeName)
        assertEquals("UZ", rishton.countryCode)
    }

    @Test
    fun unknown_zone_and_no_detection_gives_null() {
        assertNull(PrayerLocationResolver.resolve(PrayerSettings(), ZoneId.of("Pacific/Tahiti")))
    }

    @Test
    fun country_defaults() {
        assertEquals(CalculationMethod.MWL to AsrMadhab.HANAFI, Cities.defaultsFor("UZ"))
        assertEquals(CalculationMethod.EGYPT to AsrMadhab.SHAFII, Cities.defaultsFor("EG"))
        assertEquals(CalculationMethod.RUSSIA to AsrMadhab.HANAFI, Cities.defaultsFor("RU"))
    }

    @Test
    fun every_zone_is_valid_and_city_ids_unique() {
        Cities.ALL.mapNotNull { it.zone }.forEach { ZoneId.of(it) }
        assertEquals(Cities.ALL.size, Cities.ALL.map { it.id }.toSet().size)
        assertEquals(Cities.ALL.mapNotNull { it.zone }.size, Cities.ALL.mapNotNull { it.zone }.toSet().size)
    }

    @Test
    fun hijri_date_matches_umm_al_qura() {
        assertEquals(HijriDate(16, 4, 1448), HijriCalendar.from(LocalDate.of(2026, 9, 27)))
        assertEquals(HijriDate(6, 1, 1448), HijriCalendar.from(LocalDate.of(2026, 6, 21)))
        assertEquals(HijriDate(17, 4, 1448), HijriCalendar.from(LocalDate.of(2026, 9, 27), adjustmentDays = 1))
    }
}
