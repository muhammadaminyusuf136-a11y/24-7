package uz.reja24.app.presentation.onboarding

import uz.reja24.app.domain.repository.PrayerSettingsRepository
import uz.reja24.app.domain.prayer.PrayerSettings
import uz.reja24.app.domain.model.VoiceGender
import uz.reja24.app.domain.model.UserPreferences
import uz.reja24.app.domain.model.DateDisplay
import uz.reja24.app.domain.model.BackgroundStyle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.reja24.app.core.util.LocaleHelper
import uz.reja24.app.domain.model.AppLanguage
import uz.reja24.app.domain.model.RoutineSettings
import uz.reja24.app.domain.repository.ReminderScheduler
import uz.reja24.app.domain.repository.RoutineSettingsRepository
import uz.reja24.app.domain.repository.UserPreferencesRepository
import uz.reja24.app.service.RoutineOrchestrator
import java.time.LocalTime
import javax.inject.Inject

/**
 * Birinchi ochilishdagi qadamlar. Til eng birinchi — keyingi hamma narsa shu tilda.
 * Ilovaning barcha muhim sozlamalari shu yerda bir marta tartib bilan o'tiladi (keyin Sozlamalardan o'zgartirsa bo'ladi).
 */
enum class OnboardingStep { LANGUAGE, WELCOME, PROBLEM, LOOK, ROUTINE, VOICE, NIGHT, PRAYER, APPS, PERMISSIONS }

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.LANGUAGE,
    val routine: RoutineSettings = RoutineSettings(),
    val language: AppLanguage? = null,
    val preferences: UserPreferences = UserPreferences(),
    val prayer: PrayerSettings = PrayerSettings(),
    val isFinishing: Boolean = false
) {
    val stepIndex: Int get() = step.ordinal
    val stepCount: Int get() = OnboardingStep.entries.size
    val isLast: Boolean get() = step == OnboardingStep.entries.last()
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val routineSettingsRepository: RoutineSettingsRepository,
    private val reminderScheduler: ReminderScheduler,
    private val routineOrchestrator: RoutineOrchestrator,
    private val prayerSettingsRepository: PrayerSettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private val _finished = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val finished: SharedFlow<Unit> = _finished.asSharedFlow()

    init {
        viewModelScope.launch {
            val routine = runCatching { routineSettingsRepository.getSettings() }.getOrDefault(RoutineSettings())
            val prefs = runCatching { userPreferencesRepository.preferences.first() }.getOrNull()
            _state.update {
                it.copy(routine = routine, language = prefs?.takeIf { p -> p.isLanguageChosen }?.language)
            }
        }
        // Rang, sana, ovoz va namoz sozlamalari darhol saqlanadi — ekran shu zahoti o'zgaradi
        viewModelScope.launch { userPreferencesRepository.preferences.collect { p -> _state.update { it.copy(preferences = p) } } }
        viewModelScope.launch { prayerSettingsRepository.settings.collect { p -> _state.update { it.copy(prayer = p) } } }
    }

    fun setStyle(style: BackgroundStyle) = save { userPreferencesRepository.setBackgroundStyle(style) }
    fun setDateDisplay(display: DateDisplay) = save { userPreferencesRepository.setDateDisplay(display) }
    fun setVoiceGender(gender: VoiceGender) = save { userPreferencesRepository.setVoiceGender(gender) }
    fun setAllowOnly(on: Boolean) = save { userPreferencesRepository.setNightAllowOnly(on) }
    fun setGrayscale(on: Boolean) = save { userPreferencesRepository.setNightGrayscale(on) }
    fun setPrayerEnabled(on: Boolean) = save { prayerSettingsRepository.update { it.copy(isEnabled = on) } }
    fun setAzkar(on: Boolean) = save { prayerSettingsRepository.update { it.copy(azkarOn = on) } }

    /** Ovozli eslatma: rejim eslatmalari va har bir reja eslatmasi birga. */
    fun setVoice(on: Boolean) {
        _state.update { it.copy(routine = it.routine.copy(isVoiceReminderOn = on)) }
        save { userPreferencesRepository.setTaskVoice(on) }
    }

    /** Bloklash kerak emas (o'zi tartibli uxlaydiganlar) — kun tartibi, namoz va eslatmalar baribir ishlaydi. */
    fun setRoutineActive(on: Boolean) = _state.update { it.copy(routine = it.routine.copy(isRoutineActive = on)) }

    fun setStrict(on: Boolean) = _state.update { it.copy(routine = it.routine.copy(isStrictModeOn = on)) }

    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch { runCatching { block() } }
    }

    fun next() {
        val current = _state.value
        if (current.isLast) finish() else _state.update { it.copy(step = OnboardingStep.entries[it.stepIndex + 1]) }
    }

    fun back() {
        _state.update { if (it.stepIndex > 0) it.copy(step = OnboardingStep.entries[it.stepIndex - 1]) else it }
    }

    fun selectLanguage(language: AppLanguage) {
        _state.update { it.copy(language = language) }
        viewModelScope.launch {
            runCatching { userPreferencesRepository.setLanguage(language) }
            // Ekran shu zahoti tanlangan tilda qayta chiziladi, qadam saqlanib qoladi
            LocaleHelper.applyLanguage(language)
        }
    }

    fun setBlockStart(time: LocalTime) = _state.update { it.copy(routine = it.routine.copy(blockStartTime = time)) }
    fun setSleep(time: LocalTime) = _state.update { it.copy(routine = it.routine.copy(sleepTime = time)) }
    fun setWake(time: LocalTime) = _state.update { it.copy(routine = it.routine.copy(wakeTime = time)) }

    fun finish() {
        if (_state.value.isFinishing) return
        _state.update { it.copy(isFinishing = true) }
        viewModelScope.launch {
            try {
                routineSettingsRepository.saveSettings(_state.value.routine)
                userPreferencesRepository.setOnboardingCompleted(true)
                reminderScheduler.rescheduleAll()
                routineOrchestrator.refreshMonitorService()
            } catch (e: Exception) {
                // Saqlashda xato bo'lsa ham foydalanuvchini to'xtatmaymiz — sozlamalardan o'zgartirish mumkin
                runCatching { userPreferencesRepository.setOnboardingCompleted(true) }
            }
            _finished.emit(Unit)
        }
    }
}
