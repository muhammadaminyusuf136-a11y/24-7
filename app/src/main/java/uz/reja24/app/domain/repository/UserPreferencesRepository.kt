package uz.reja24.app.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.reja24.app.domain.model.AppLanguage
import uz.reja24.app.domain.model.BackgroundStyle
import uz.reja24.app.domain.model.ThemeMode
import uz.reja24.app.domain.model.UserPreferences
import uz.reja24.app.domain.model.VoiceGender

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setBackgroundStyle(style: BackgroundStyle)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setVoiceGender(gender: VoiceGender)
    suspend fun setNightAllowOnly(enabled: Boolean)
    suspend fun setAllowedPackages(packages: Set<String>)
}
