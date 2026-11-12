package uz.reja24.app.domain.prayer

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Qo'lda tanlangan joy: ichki ro'yxatdagi shahar (cityId) yoki qidiruvda topilgan istalgan joy. */
data class ManualPlace(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val countryCode: String?,
    /** Ichki ro'yxatdagi shahar bo'lsa — nomi foydalanuvchi tilida ko'rsatiladi. */
    val cityId: String? = null
)

/** GPS yoki tarmoq orqali aniqlangan so'nggi joy. */
data class DetectedLocation(
    val latitude: Double,
    val longitude: Double,
    val timeMillis: Long
)

/**
 * Namoz vaqtlari sozlamalari.
 *  - autoLocation = true: joy telefondan olinadi; olinmasa — telefon vaqt zonasidagi asosiy shahar.
 *  - autoLocation = false: foydalanuvchi o'zi tanlagan joy (manualPlace).
 *  - method / madhab = null: mamlakatga mos usul avtomatik tanlanadi.
 */
data class PrayerSettings(
    val isEnabled: Boolean = true,
    val autoLocation: Boolean = true,
    val detected: DetectedLocation? = null,
    val manualPlace: ManualPlace? = null,
    val method: CalculationMethod? = null,
    val madhab: AsrMadhab? = null,
    val adjustments: Map<Prayer, Int> = emptyMap(),
    val reminderMinutes: Int = DEFAULT_REMINDER_MINUTES,
    val notifyAtTime: Boolean = true,
    val mutedPrayers: Set<Prayer> = emptySet(),
    val alarmPrayers: Set<Prayer> = emptySet(),
    val alarmSoundUri: String? = null,
    val hijriAdjustmentDays: Int = 0
) {
    companion object {
        const val DEFAULT_REMINDER_MINUTES = 15
        const val MAX_ADJUSTMENT_MINUTES = 30
        const val MAX_HIJRI_ADJUSTMENT = 2
    }
}

/** Hisoblash uchun aniq joy: koordinata va (bo'lsa) yaqin shahar. */
data class ResolvedLocation(
    val latitude: Double,
    val longitude: Double,
    /** Ko'rsatish uchun shahar; null — yaqin atrofda ro'yxatdagi shahar yo'q. */
    val city: City?,
    val countryCode: String?,
    val source: Source,
    /** Qidiruvda topilgan joy nomi (ichki ro'yxatda bo'lmasa). */
    val placeName: String? = null
) {
    enum class Source { DETECTED, TIME_ZONE, MANUAL }
}

object PrayerLocationResolver {

    /**
     * Qaysi joy bo'yicha hisoblash kerak.
     * Avtomatik rejimda telefondan olingan joy boshqa vaqt zonasiga tegishli bo'lsa
     * (odam safarga chiqqan, ilova hali ochilmagan) — eski joy emas, yangi zona shahri olinadi.
     */
    fun resolve(settings: PrayerSettings, deviceZone: ZoneId): ResolvedLocation? {
        if (!settings.autoLocation) {
            val place = settings.manualPlace ?: return null
            val city = Cities.byId(place.cityId)
            return ResolvedLocation(
                latitude = place.latitude,
                longitude = place.longitude,
                city = city,
                countryCode = place.countryCode ?: city?.countryCode,
                source = ResolvedLocation.Source.MANUAL,
                placeName = place.name
            )
        }
        val zoneCity = Cities.forZone(deviceZone.id)
        val detected = settings.detected
        if (detected != null) {
            val nearest = Cities.nearest(detected.latitude, detected.longitude)
            val distance = Cities.distanceKm(detected.latitude, detected.longitude, nearest.latitude, nearest.longitude)
            val nearCity = nearest.takeIf { distance <= Cities.NEAR_CITY_KM }
            val stale = zoneCity != null && zoneCity.countryCode != nearest.countryCode &&
                !sameZone(nearest, deviceZone)
            if (!stale) {
                return ResolvedLocation(
                    detected.latitude, detected.longitude, nearCity, nearest.countryCode,
                    ResolvedLocation.Source.DETECTED
                )
            }
        }
        zoneCity ?: return null
        return ResolvedLocation(zoneCity.latitude, zoneCity.longitude, zoneCity, zoneCity.countryCode, ResolvedLocation.Source.TIME_ZONE)
    }

    private fun sameZone(city: City, zone: ZoneId): Boolean = city.zone == zone.id
}

/** Ekranlar uchun: sozlama, aniqlangan joy va kerakli kunlarning vaqtlari. */
data class PrayerOverview(
    val settings: PrayerSettings,
    val location: ResolvedLocation?,
    val days: Map<LocalDate, PrayerDay>
) {
    val isActive: Boolean get() = settings.isEnabled && location != null

    companion object {
        fun build(settings: PrayerSettings, zone: ZoneId, dates: Collection<LocalDate>): PrayerOverview {
            val location = PrayerLocationResolver.resolve(settings, zone)
            val days = if (settings.isEnabled && location != null) {
                dates.associateWith { PrayerSchedule.forDate(it, settings, location, zone) }
            } else {
                emptyMap()
            }
            return PrayerOverview(settings, location, days)
        }
    }
}

/** Bir kunlik vaqtlar. */
data class PrayerDay(val date: LocalDate, val times: Map<Prayer, LocalDateTime>) {
    fun time(prayer: Prayer): LocalDateTime = times.getValue(prayer)
}

object PrayerSchedule {

    fun methodFor(settings: PrayerSettings, location: ResolvedLocation): CalculationMethod =
        settings.method ?: Cities.defaultsFor(location.countryCode).first

    fun madhabFor(settings: PrayerSettings, location: ResolvedLocation): AsrMadhab =
        settings.madhab ?: Cities.defaultsFor(location.countryCode).second

    fun forDate(
        date: LocalDate,
        settings: PrayerSettings,
        location: ResolvedLocation,
        zone: ZoneId
    ): PrayerDay {
        val hijri = HijriCalendar.from(date, settings.hijriAdjustmentDays)
        val times = PrayerCalculator.calculate(
            date = date,
            latitude = location.latitude,
            longitude = location.longitude,
            zone = zone,
            method = methodFor(settings, location),
            madhab = madhabFor(settings, location),
            adjustmentsMinutes = settings.adjustments,
            isRamadan = hijri?.month == HijriCalendar.RAMADAN
        )
        return PrayerDay(date, times)
    }

    /** now dan keyingi eng yaqin farz namoz (bugun yoki ertaga). */
    fun next(days: List<PrayerDay>, now: LocalDateTime): Pair<Prayer, LocalDateTime>? =
        days.flatMap { day -> Prayer.OBLIGATORY.map { it to day.time(it) } }
            .filter { it.second.isAfter(now) }
            .minByOrNull { it.second }
}
