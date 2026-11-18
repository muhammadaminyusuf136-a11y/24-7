package uz.reja24.app.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.reja24.app.domain.model.AppLanguage
import uz.reja24.app.domain.model.BackgroundStyle
import uz.reja24.app.domain.model.DateDisplay
import uz.reja24.app.domain.model.UserPreferences
import uz.reja24.app.domain.model.VoiceGender

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setDateDisplay(display: DateDisplay)
    suspend fun setBackgroundStyle(style: BackgroundStyle)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setVoiceGender(gender: VoiceGender)
    suspend fun setNightAllowOnly(enabled: Boolean)
    suspend fun setNightGrayscale(enabled: Boolean)
    suspend fun setAllowedPackages(packages: Set<String>)
    suspend fun setTaskVoice(enabled: Boolean)
    suspend fun setTaskSecondReminder(minutes: Int)
}
