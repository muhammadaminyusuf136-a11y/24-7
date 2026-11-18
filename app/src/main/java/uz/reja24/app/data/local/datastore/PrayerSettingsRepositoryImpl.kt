package uz.reja24.app.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import uz.reja24.app.domain.prayer.AsrMadhab
import uz.reja24.app.domain.prayer.CalculationMethod
import uz.reja24.app.domain.prayer.DetectedLocation
import uz.reja24.app.domain.prayer.ManualPlace
import uz.reja24.app.domain.prayer.Prayer
import uz.reja24.app.domain.prayer.PrayerSettings
import uz.reja24.app.domain.repository.PrayerSettingsRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerSettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PrayerSettingsRepository {

    override val settings: Flow<PrayerSettings> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it.toSettings() }
        .distinctUntilChanged()

    override suspend fun getSettings(): PrayerSettings = settings.first()

    override suspend fun update(transform: (PrayerSettings) -> PrayerSettings) {
        dataStore.edit { prefs -> prefs.write(transform(prefs.toSettings())) }
    }

    private fun Preferences.toSettings(): PrayerSettings {
        val lat = this[Keys.LAT]
        val lon = this[Keys.LON]
        return PrayerSettings(
            isEnabled = this[Keys.ENABLED] ?: true,
            autoLocation = this[Keys.AUTO_LOCATION] ?: true,
            detected = if (lat != null && lon != null) DetectedLocation(lat, lon, this[Keys.LOCATION_TIME] ?: 0L) else null,
            manualPlace = manualPlace(),
            method = CalculationMethod.fromName(this[Keys.METHOD]),
            madhab = AsrMadhab.fromName(this[Keys.MADHAB]),
            adjustments = decodeAdjustments(this[Keys.ADJUSTMENTS]),
            reminderMinutes = (this[Keys.REMINDER] ?: PrayerSettings.DEFAULT_REMINDER_MINUTES).coerceIn(0, 60),
            notifyAtTime = this[Keys.NOTIFY_AT_TIME] ?: true,
            azkarOn = this[Keys.AZKAR] ?: true,
            mutedPrayers = decodePrayers(this[Keys.MUTED]),
            alarmPrayers = decodePrayers(this[Keys.ALARMS]),
            alarmSoundUri = this[Keys.ALARM_SOUND],
            hijriAdjustmentDays = (this[Keys.HIJRI_ADJUST] ?: 0)
                .coerceIn(-PrayerSettings.MAX_HIJRI_ADJUSTMENT, PrayerSettings.MAX_HIJRI_ADJUSTMENT)
        )
    }

    private fun MutablePreferences.write(s: PrayerSettings) {
        this[Keys.ENABLED] = s.isEnabled
        this[Keys.AUTO_LOCATION] = s.autoLocation
        val detected = s.detected
        if (detected != null) {
            this[Keys.LAT] = detected.latitude
            this[Keys.LON] = detected.longitude
            this[Keys.LOCATION_TIME] = detected.timeMillis
        } else {
            remove(Keys.LAT)
            remove(Keys.LON)
            remove(Keys.LOCATION_TIME)
        }
        val place = s.manualPlace
        if (place != null) {
            this[Keys.MANUAL_NAME] = place.name
            this[Keys.MANUAL_LAT] = place.latitude
            this[Keys.MANUAL_LON] = place.longitude
            setOrRemove(Keys.MANUAL_COUNTRY, place.countryCode)
            setOrRemove(Keys.MANUAL_CITY, place.cityId)
        } else {
            remove(Keys.MANUAL_NAME)
            remove(Keys.MANUAL_LAT)
            remove(Keys.MANUAL_LON)
            remove(Keys.MANUAL_COUNTRY)
            remove(Keys.MANUAL_CITY)
        }
        setOrRemove(Keys.METHOD, s.method?.name)
        setOrRemove(Keys.MADHAB, s.madhab?.name)
        this[Keys.ADJUSTMENTS] = s.adjustments.filterValues { it != 0 }
            .entries.joinToString(",") { "${it.key.name}:${it.value}" }
        this[Keys.REMINDER] = s.reminderMinutes
        this[Keys.NOTIFY_AT_TIME] = s.notifyAtTime
        this[Keys.AZKAR] = s.azkarOn
        this[Keys.MUTED] = s.mutedPrayers.map { it.name }.toSet()
        this[Keys.ALARMS] = s.alarmPrayers.map { it.name }.toSet()
        setOrRemove(Keys.ALARM_SOUND, s.alarmSoundUri)
        this[Keys.HIJRI_ADJUST] = s.hijriAdjustmentDays
    }

    private fun MutablePreferences.setOrRemove(key: Preferences.Key<String>, value: String?) {
        if (value == null) remove(key) else this[key] = value
    }

    private fun Preferences.manualPlace(): ManualPlace? {
        val name = this[Keys.MANUAL_NAME] ?: return null
        val lat = this[Keys.MANUAL_LAT] ?: return null
        val lon = this[Keys.MANUAL_LON] ?: return null
        return ManualPlace(name, lat, lon, this[Keys.MANUAL_COUNTRY], this[Keys.MANUAL_CITY])
    }

    private fun decodePrayers(names: Set<String>?): Set<Prayer> =
        names.orEmpty().mapNotNull { Prayer.fromName(it) }.toSet()

    private fun decodeAdjustments(raw: String?): Map<Prayer, Int> =
        raw.orEmpty().split(',').mapNotNull { part ->
            val (name, value) = part.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
            val prayer = Prayer.fromName(name) ?: return@mapNotNull null
            val minutes = value.toIntOrNull() ?: return@mapNotNull null
            prayer to minutes.coerceIn(-PrayerSettings.MAX_ADJUSTMENT_MINUTES, PrayerSettings.MAX_ADJUSTMENT_MINUTES)
        }.toMap()

    private object Keys {
        val ENABLED = booleanPreferencesKey("prayer_enabled")
        val AUTO_LOCATION = booleanPreferencesKey("prayer_auto_location")
        val LAT = doublePreferencesKey("prayer_lat")
        val LON = doublePreferencesKey("prayer_lon")
        val LOCATION_TIME = longPreferencesKey("prayer_location_time")
        val MANUAL_CITY = stringPreferencesKey("prayer_manual_city")
        val MANUAL_NAME = stringPreferencesKey("prayer_manual_name")
        val MANUAL_LAT = doublePreferencesKey("prayer_manual_lat")
        val MANUAL_LON = doublePreferencesKey("prayer_manual_lon")
        val MANUAL_COUNTRY = stringPreferencesKey("prayer_manual_country")
        val METHOD = stringPreferencesKey("prayer_method")
        val MADHAB = stringPreferencesKey("prayer_madhab")
        val ADJUSTMENTS = stringPreferencesKey("prayer_adjustments")
        val REMINDER = intPreferencesKey("prayer_reminder_minutes")
        val NOTIFY_AT_TIME = booleanPreferencesKey("prayer_notify_at_time")
        val AZKAR = booleanPreferencesKey("prayer_azkar_on")
        val MUTED = stringSetPreferencesKey("prayer_muted")
        val ALARMS = stringSetPreferencesKey("prayer_alarms")
        val ALARM_SOUND = stringPreferencesKey("prayer_alarm_sound")
        val HIJRI_ADJUST = intPreferencesKey("hijri_adjust_days")
    }
}
