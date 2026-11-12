package uz.reja24.app.presentation.schedule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.reja24.app.core.util.TimeProvider
import uz.reja24.app.domain.model.Category
import uz.reja24.app.domain.model.RepeatType
import uz.reja24.app.domain.model.ScheduleEntry
import uz.reja24.app.domain.repository.ReminderScheduler
import uz.reja24.app.domain.repository.ScheduleRepository
import uz.reja24.app.presentation.navigation.Route
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class EditorState(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val startTime: LocalTime = LocalTime.of(9, 0),
    val endTime: LocalTime = LocalTime.of(10, 0),
    val category: Category = Category.WORK,
    val repeatType: RepeatType = RepeatType.ONCE,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val date: LocalDate = LocalDate.now(),
    val reminderMinutes: Int = ScheduleEntry.DEFAULT_REMINDER_MINUTES,
    val alarmEnabled: Boolean = false,
    /** null — telefonning standart budilnik ovozi. */
    val alarmSoundUri: String? = null,
    val isActive: Boolean = true,
    val titleError: Boolean = false,
    val daysError: Boolean = false,
    /** Boshlanish va tugash bir xil — reja 24 soatlik bo'lib qolardi. */
    val timeError: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false
) {
    val isNew: Boolean get() = id == 0L
}

sealed interface EditorEvent {
    data object Saved : EditorEvent
    data object Deleted : EditorEvent
    data object Error : EditorEvent
}

@HiltViewModel
class EntryEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val scheduleRepository: ScheduleRepository,
    private val reminderScheduler: ReminderScheduler,
    timeProvider: TimeProvider
) : ViewModel() {

    // Type-safe navigatsiya argumentlari SavedStateHandle'da xususiyat nomi bilan saqlanadi
    private val entryId: Long = savedStateHandle.get<Long>("entryId") ?: 0L
    private val initialDate: LocalDate = savedStateHandle.get<Long>("dateEpochDay")
        ?.takeIf { it != Route.NO_DATE }
        ?.let(LocalDate::ofEpochDay)
        ?: timeProvider.today()

    // Yangi reja sukut bo'yicha har hafta shu kuni takrorlanadi — bir marta belgilagan kun tartibi
    // keyingi haftada ham saqlanib qoladi ("Bir marta"ni alohida tanlash mumkin)
    private val _state = MutableStateFlow(
        EditorState(
            date = initialDate,
            repeatType = RepeatType.WEEKLY,
            daysOfWeek = setOf(initialDate.dayOfWeek),
            isLoading = entryId != 0L
        )
    )
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<EditorEvent>(extraBufferCapacity = 2)
    val events: SharedFlow<EditorEvent> = _events.asSharedFlow()

    init {
        if (entryId != 0L) {
            viewModelScope.launch {
                val entry = try {
                    scheduleRepository.getEntry(entryId)
                } catch (e: Exception) {
                    null
                }
                if (entry == null) {
                    _state.update { it.copy(isLoading = false) }
                    _events.emit(EditorEvent.Error)
                } else {
                    _state.value = EditorState(
                        id = entry.id,
                        title = entry.title,
                        description = entry.description,
                        startTime = entry.startTime,
                        endTime = entry.endTime,
                        category = entry.category,
                        repeatType = entry.repeatType,
                        daysOfWeek = entry.daysOfWeek,
                        date = entry.specificDate ?: initialDate,
                        reminderMinutes = entry.reminderMinutesBefore,
                        alarmEnabled = entry.alarmEnabled,
                        alarmSoundUri = entry.alarmSoundUri,
                        isActive = entry.isActive
                    )
                }
            }
        }
    }

    fun onTitleChange(value: String) = _state.update { it.copy(title = value.take(MAX_TITLE), titleError = false) }
    fun onDescriptionChange(value: String) = _state.update { it.copy(description = value.take(MAX_DESCRIPTION)) }
    fun onStartTimeChange(value: LocalTime) = _state.update { it.copy(startTime = value, timeError = false) }
    fun onEndTimeChange(value: LocalTime) = _state.update { it.copy(endTime = value, timeError = false) }
    fun onCategoryChange(value: Category) = _state.update { it.copy(category = value) }
    fun onDateChange(value: LocalDate) = _state.update { current ->
        // Haftalik rejada faqat eski sananing kuni tanlangan bo'lsa — yangi sananing kuniga o'tadi
        val days = if (current.repeatType == RepeatType.WEEKLY && current.daysOfWeek == setOf(current.date.dayOfWeek)) {
            setOf(value.dayOfWeek)
        } else {
            current.daysOfWeek
        }
        current.copy(date = value, daysOfWeek = days)
    }
    fun onReminderChange(value: Int) = _state.update { it.copy(reminderMinutes = value.coerceIn(0, 24 * 60)) }

    fun onAlarmEnabledChange(value: Boolean) = _state.update { it.copy(alarmEnabled = value) }
    fun onAlarmSoundChange(uri: String?) = _state.update { it.copy(alarmSoundUri = uri) }

    fun onRepeatTypeChange(value: RepeatType) = _state.update { current ->
        // Haftalik tanlanganda — tanlangan sananing hafta kuni avtomatik belgilanadi
        val days = if (value == RepeatType.WEEKLY && current.daysOfWeek.isEmpty()) {
            setOf(current.date.dayOfWeek)
        } else {
            current.daysOfWeek
        }
        current.copy(repeatType = value, daysOfWeek = days, daysError = false)
    }

    fun onToggleDay(day: DayOfWeek) = _state.update { current ->
        val days = if (day in current.daysOfWeek) current.daysOfWeek - day else current.daysOfWeek + day
        current.copy(daysOfWeek = days, daysError = false)
    }

    fun save() {
        val current = _state.value
        if (current.isSaving) return
        val titleError = current.title.isBlank()
        val daysError = current.repeatType == RepeatType.WEEKLY && current.daysOfWeek.isEmpty()
        val timeError = current.startTime == current.endTime
        if (titleError || daysError || timeError) {
            _state.update { it.copy(titleError = titleError, daysError = daysError, timeError = timeError) }
            return
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                scheduleRepository.saveEntry(current.toEntry())
                reminderScheduler.rescheduleAll()
                _events.emit(EditorEvent.Saved)
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false) }
                _events.emit(EditorEvent.Error)
            }
        }
    }

    fun delete() {
        val id = _state.value.id
        if (id == 0L) return
        viewModelScope.launch {
            try {
                scheduleRepository.deleteEntry(id)
                reminderScheduler.rescheduleAll()
                _events.emit(EditorEvent.Deleted)
            } catch (e: Exception) {
                _events.emit(EditorEvent.Error)
            }
        }
    }

    private fun EditorState.toEntry() = ScheduleEntry(
        id = id,
        title = title.trim(),
        description = description.trim(),
        startTime = startTime,
        endTime = endTime,
        category = category,
        colorHex = category.defaultColorHex,
        isActive = isActive,
        repeatType = repeatType,
        daysOfWeek = if (repeatType == RepeatType.WEEKLY) daysOfWeek else emptySet(),
        // Bir martalik, oylik va yillik rejalar uchun sana muhim; boshqalar shu kundan boshlanadi
        specificDate = date,
        reminderMinutesBefore = reminderMinutes,
        alarmEnabled = alarmEnabled,
        alarmSoundUri = alarmSoundUri
    )

    companion object {
        const val MAX_TITLE = 60
        const val MAX_DESCRIPTION = 300
    }
}
