package uz.reja24.app.presentation.emergency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.reja24.app.core.util.TimeProvider
import uz.reja24.app.domain.model.MathProblem
import uz.reja24.app.domain.repository.StreakRepository
import uz.reja24.app.domain.usecase.EmergencyAccessUseCase
import javax.inject.Inject
import kotlin.random.Random

/** Favqulodda kirish bosqichlari. */
sealed interface EmergencyStage {
    data object Loading : EmergencyStage
    /** 1–2: to'xtatuvchi motivatsion xabar. */
    data class Intro(val messageIndex: Int, val streakDays: Int, val remaining: Int) : EmergencyStage
    /** 3: 60 soniya sanoq — o'tkazib yuborib bo'lmaydi. */
    data class Countdown(val secondsLeft: Int) : EmergencyStage
    /** 4: matematik misol. */
    data class Question(val problem: MathProblem, val wrongAttempt: Boolean) : EmergencyStage
    /** 5–6: 5 daqiqalik ruxsat berildi, −30 ball. */
    data object Granted : EmergencyStage
    /** 7–8: bugungi limit tugagan. */
    data object LimitReached : EmergencyStage
}

@HiltViewModel
class EmergencyViewModel @Inject constructor(
    private val emergencyAccessUseCase: EmergencyAccessUseCase,
    private val streakRepository: StreakRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _stage = MutableStateFlow<EmergencyStage>(EmergencyStage.Loading)
    val stage: StateFlow<EmergencyStage> = _stage.asStateFlow()

    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            val remaining = runCatching { emergencyAccessUseCase.remainingToday() }.getOrDefault(0)
            if (remaining <= 0) {
                _stage.value = EmergencyStage.LimitReached
                return@launch
            }
            val streak = runCatching { streakRepository.getSummary(timeProvider.today()).current }.getOrDefault(0)
            var index = Random.nextInt(0, EMERGENCY_MESSAGE_COUNT)
            // Streak haqidagi xabar faqat streak bo'lsa ko'rsatiladi
            if (index == STREAK_MESSAGE_INDEX && streak <= 0) index = 0
            _stage.value = EmergencyStage.Intro(index, streak, remaining)
        }
    }

    /** "Baribir davom etaman" — 60 soniyalik sanoq boshlanadi. */
    fun startCountdown() {
        if (countdownJob?.isActive == true) return
        countdownJob = viewModelScope.launch {
            for (second in COUNTDOWN_SECONDS downTo 1) {
                _stage.value = EmergencyStage.Countdown(second)
                delay(1_000)
            }
            _stage.value = EmergencyStage.Question(MathProblem.generate(), wrongAttempt = false)
        }
    }

    fun submitAnswer(answer: String) {
        val current = _stage.value as? EmergencyStage.Question ?: return
        if (!current.problem.isCorrect(answer)) {
            // Noto'g'ri — yangi misol
            _stage.value = EmergencyStage.Question(MathProblem.generate(), wrongAttempt = true)
            return
        }
        viewModelScope.launch {
            val granted = runCatching { emergencyAccessUseCase.grant() }.getOrDefault(false)
            _stage.value = if (granted) EmergencyStage.Granted else EmergencyStage.LimitReached
        }
    }

    companion object {
        const val COUNTDOWN_SECONDS = 60
        /** strings.xml dagi emergency_messages massivi uzunligi. */
        const val EMERGENCY_MESSAGE_COUNT = 5
        /** "{N} kunlik ketma-ketliging xavf ostida" xabari indeksi. */
        const val STREAK_MESSAGE_INDEX = 1
    }
}
