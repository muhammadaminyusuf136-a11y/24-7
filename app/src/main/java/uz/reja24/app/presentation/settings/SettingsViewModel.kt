package uz.reja24.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import uz.reja24.app.core.util.LocaleHelper
import uz.reja24.app.core.util.TimeProvider
import uz.reja24.app.domain.model.RoutineLock
import java.time.LocalTime
import uz.reja24.app.domain.model.AppLanguage
import uz.reja24.app.domain.model.BackgroundStyle
import uz.reja24.app.domain.model.BlockedApp
import uz.reja24.app.domain.model.RoutineSettings
import uz.reja24.app.domain.model.ThemeMode
import uz.reja24.app.domain.model.UserPreferences
import uz.reja24.app.domain.model.VoiceGender
import uz.reja24.app.domain.repository.BlockedAppRepository
import uz.reja24.app.domain.repository.ReminderScheduler
import uz.reja24.app.domain.repository.RoutineSettingsRepository
import uz.reja24.app.domain.repository.UserPreferencesRepository
import uz.reja24.app.service.RoutineOrchestrator
import javax.inject.Inject

data class SettingsUiState(
    val preferences: UserPreferences,
    val routine: RoutineSettings,
    val blockedApps: List<BlockedApp>
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val routineSettingsRepository: RoutineSettingsRepository,
    private val blockedAppRepository: BlockedAppRepository,
    private val reminderScheduler: ReminderScheduler,
    private val routineOrchestrator: RoutineOrchestrator,
    private val timeProvider: TimeProvider
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState?> = combine(
        userPreferencesRepository.preferences,
        routineSettingsRepository.observeSettings(),
        blockedAppRepository.observeAll()
    ) { prefs, routine, apps ->
        SettingsUiState(prefs, routine, apps.filter { it.isBlocked })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _errors = MutableSharedFlow<Unit>(extraBufferCapacity = 2)
    val errors: SharedFlow<Unit> = _errors.asSharedFlow()

    /** Rejim qulfi sababli rad etilgan o'zgarish — qachon ochilishi bilan. */
    private val _locked = MutableSharedFlow<LocalTime>(extraBufferCapacity = 2)
    val locked: SharedFlow<LocalTime> = _locked.asSharedFlow()

    /** Hozir qulflangan bo'lsa — qulf ochiladigan vaqt (uyg'onish), aks holda null. Har 30 soniyada tekshiriladi. */
    val lockedUntil: StateFlow<LocalTime?> = combine(
        routineSettingsRepository.observeSettings(),
        flow { while (true) { emit(Unit); delay(LOCK_CHECK_MS) } }
    ) { routine, _ ->
        routine.wakeTime.takeIf { RoutineLock.isLocked(routine, timeProvider.now().toLocalTime()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun lockedNow(routine: RoutineSettings): Boolean = RoutineLock.isLocked(routine, timeProvider.now().toLocalTime())

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            safely { userPreferencesRepository.setLanguage(language) }
            // Ilova darhol yangi tilga o'tadi (qayta ishga tushirish shart emas)
            LocaleHelper.applyLanguage(language)
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { safely { userPreferencesRepository.setThemeMode(mode) } }
    }

    fun setTaskVoice(enabled: Boolean) {
        viewModelScope.launch { safely { userPreferencesRepository.setTaskVoice(enabled) } }
    }

    /** Ikkinchi eslatma vaqti o'zgarsa — rejalar eslatmalari qayta qo'yiladi. */
    fun setTaskSecondReminder(minutes: Int) {
        viewModelScope.launch {
            safely {
                userPreferencesRepository.setTaskSecondReminder(minutes)
                reminderScheduler.rescheduleAll()
            }
        }
    }

    fun setVoiceGender(gender: VoiceGender) {
        viewModelScope.launch { safely { userPreferencesRepository.setVoiceGender(gender) } }
    }

    fun setBackgroundStyle(style: BackgroundStyle) {
        viewModelScope.launch { safely { userPreferencesRepository.setBackgroundStyle(style) } }
    }

    /** Rejim sozlamasini o'zgartiradi va eslatmalarni qayta rejalashtiradi. */
    fun updateRoutine(transform: (RoutineSettings) -> RoutineSettings) {
        val current = uiState.value?.routine ?: return
        val next = transform(current)
        if (lockedNow(current) && RoutineLock.weakens(current, next)) {
            _locked.tryEmit(current.wakeTime)
            return
        }
        viewModelScope.launch {
            safely {
                routineSettingsRepository.saveSettings(next)
                reminderScheduler.rescheduleAll()
                routineOrchestrator.refreshMonitorService()
            }
        }
    }

    /** "Faqat kerakli" rejimni o'chirish — blokni bo'shashtiradi: qulf vaqtida mumkin emas. */
    fun setNightAllowOnly(enabled: Boolean) {
        val routine = uiState.value?.routine
        if (!enabled && routine != null && lockedNow(routine)) {
            _locked.tryEmit(routine.wakeTime)
            return
        }
        viewModelScope.launch { safely { userPreferencesRepository.setNightAllowOnly(enabled) } }
    }

    fun removeBlockedApp(packageName: String) {
        val routine = uiState.value?.routine
        if (routine != null && lockedNow(routine)) {
            _locked.tryEmit(routine.wakeTime)
            return
        }
        viewModelScope.launch { safely { blockedAppRepository.setBlocked(packageName, false) } }
    }

    private suspend fun safely(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            _errors.emit(Unit)
        }
    }

    private companion object {
        const val LOCK_CHECK_MS = 30_000L
    }
}
