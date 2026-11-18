package uz.reja24.app.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import uz.reja24.app.domain.model.AppLanguage
import uz.reja24.app.domain.model.BackgroundStyle
import uz.reja24.app.domain.model.DateDisplay
import uz.reja24.app.domain.model.UserPreferences
import uz.reja24.app.domain.model.VoiceGender
import uz.reja24.app.domain.repository.UserPreferencesRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Oddiy sozlamalar (til, mavzu, orqa fon, onboarding) DataStore'da saqlanadi.
 */
@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    override val preferences: Flow<UserPreferences> = dataStore.data
        // Fayl buzilgan yoki o'qib bo'lmasa — ilova yiqilmaydi, standart qiymatlar ishlatiladi
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs ->
            UserPreferences(
                language = AppLanguage.fromTag(prefs[Keys.LANGUAGE]),
                isLanguageChosen = prefs[Keys.LANGUAGE] != null,
                backgroundStyle = prefs[Keys.BACKGROUND]?.let(BackgroundStyle::fromName)
                    // Eski versiyada "Qora" mavzu tanlaganlar — to'q ko'k rangda davom etadi
                    ?: if (prefs[Keys.THEME] == "DARK") BackgroundStyle.POWER else BackgroundStyle.PURE,
                dateDisplay = DateDisplay.fromName(prefs[Keys.DATE_DISPLAY]),
                isOnboardingCompleted = prefs[Keys.ONBOARDING_DONE] ?: false,
                voiceGender = VoiceGender.fromName(prefs[Keys.VOICE_GENDER]),
                nightAllowOnly = prefs[Keys.NIGHT_ALLOW_ONLY] ?: false,
                allowedPackages = prefs[Keys.ALLOWED_PACKAGES].orEmpty(),
                taskVoiceOn = prefs[Keys.TASK_VOICE] ?: false,
                taskSecondReminderMinutes = prefs[Keys.TASK_SECOND_REMINDER] ?: 0
            )
        }
        .distinctUntilChanged()

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[Keys.LANGUAGE] = language.tag }
    }

    override suspend fun setDateDisplay(display: DateDisplay) {
        dataStore.edit { it[Keys.DATE_DISPLAY] = display.name }
    }

    override suspend fun setBackgroundStyle(style: BackgroundStyle) {
        dataStore.edit { it[Keys.BACKGROUND] = style.name }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_DONE] = completed }
    }

    override suspend fun setVoiceGender(gender: VoiceGender) {
        dataStore.edit { it[Keys.VOICE_GENDER] = gender.name }
    }

    override suspend fun setNightAllowOnly(enabled: Boolean) {
        dataStore.edit { it[Keys.NIGHT_ALLOW_ONLY] = enabled }
    }

    override suspend fun setAllowedPackages(packages: Set<String>) {
        dataStore.edit { it[Keys.ALLOWED_PACKAGES] = packages }
    }

    override suspend fun setTaskVoice(enabled: Boolean) {
        dataStore.edit { it[Keys.TASK_VOICE] = enabled }
    }

    override suspend fun setTaskSecondReminder(minutes: Int) {
        dataStore.edit { it[Keys.TASK_SECOND_REMINDER] = minutes.coerceIn(0, 60) }
    }

    private object Keys {
        val TASK_VOICE = booleanPreferencesKey("task_voice_on")
        val TASK_SECOND_REMINDER = intPreferencesKey("task_second_reminder_minutes")
        val NIGHT_ALLOW_ONLY = booleanPreferencesKey("night_allow_only")
        val ALLOWED_PACKAGES = stringSetPreferencesKey("night_allowed_packages")
        val VOICE_GENDER = stringPreferencesKey("voice_gender")
        val LANGUAGE = stringPreferencesKey("language")
        /** Eski versiyadagi mavzu — faqat o'qiladi (yangi rang tanlovi BACKGROUND'da). */
        val THEME = stringPreferencesKey("theme_mode")
        val DATE_DISPLAY = stringPreferencesKey("date_display")
        val BACKGROUND = stringPreferencesKey("background_style")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_completed")
    }
}
