package uz.reja24.app.presentation

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.reja24.app.core.di.ApplicationScope
import uz.reja24.app.core.util.LocaleHelper
import uz.reja24.app.domain.model.BackgroundStyle
import uz.reja24.app.domain.model.ThemeMode
import uz.reja24.app.domain.model.TrophyLevel
import uz.reja24.app.domain.repository.AppStateRepository
import uz.reja24.app.domain.repository.PrayerSettingsRepository
import uz.reja24.app.domain.repository.ReminderScheduler
import uz.reja24.app.domain.repository.UserPreferencesRepository
import uz.reja24.app.service.RoutineOrchestrator
import uz.reja24.app.service.prayer.PrayerLocationUpdater
import javax.inject.Inject

data class MainUiState(
    val themeMode: ThemeMode,
    val backgroundStyle: BackgroundStyle,
    val isOnboardingCompleted: Boolean,
    /** Hijriy sanani to'g'rilash (kun) — hamma ekrandagi hijriy sanalar uchun. */
    val hijriAdjustmentDays: Int,
    val pendingTrophy: TrophyLevel?
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val appStateRepository: AppStateRepository,
    private val routineOrchestrator: RoutineOrchestrator,
    private val prayerLocationUpdater: PrayerLocationUpdater,
    private val reminderScheduler: ReminderScheduler,
    prayerSettingsRepository: PrayerSettingsRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ViewModel() {

    /** null — sozlamalar hali o'qilmoqda (bir lahza). */
    val uiState: StateFlow<MainUiState?> = combine(
        userPreferencesRepository.preferences,
        appStateRepository.pendingTrophy,
        prayerSettingsRepository.settings
    ) { prefs, trophy, prayer ->
        MainUiState(
            themeMode = prefs.themeMode,
            backgroundStyle = prefs.backgroundStyle,
            isOnboardingCompleted = prefs.isOnboardingCompleted,
            hijriAdjustmentDays = prayer.hijriAdjustmentDays,
            pendingTrophy = trophy
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            // Tanlangan til saqlangan, lekin tizimda o'rnatilmagan bo'lsa (masalan ma'lumot tiklanganda)
            val prefs = userPreferencesRepository.preferences.first()
            if (prefs.isLanguageChosen && AppCompatDelegate.getApplicationLocales().isEmpty) {
                LocaleHelper.applyLanguage(prefs.language)
            }
        }
    }

    /** Ilova ochilganda: o'tkazib yuborilgan kunlar, eslatmalar, kuzatuv xizmati. */
    fun onAppResumed() {
        applicationScope.launch {
            routineOrchestrator.syncAll()
            // Joy o'zgargan bo'lsa (safar) — namoz vaqtlari va eslatmalari yangilanadi
            val result = runCatching { prayerLocationUpdater.refresh() }.getOrNull()
            if (result == PrayerLocationUpdater.Result.UPDATED) reminderScheduler.rescheduleAll()
        }
    }

    fun onTrophyShown() {
        applicationScope.launch { appStateRepository.setPendingTrophy(null) }
    }
}
