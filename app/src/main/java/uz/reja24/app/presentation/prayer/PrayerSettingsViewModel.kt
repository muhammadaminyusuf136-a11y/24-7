package uz.reja24.app.presentation.prayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.reja24.app.core.util.TimeProvider
import uz.reja24.app.data.place.PlaceSearchRepository
import uz.reja24.app.domain.model.AppLanguage
import uz.reja24.app.domain.prayer.PlaceResult
import uz.reja24.app.domain.prayer.PlaceSearch
import uz.reja24.app.domain.prayer.PrayerOverview
import uz.reja24.app.domain.prayer.PrayerSettings
import uz.reja24.app.domain.repository.PrayerSettingsRepository
import uz.reja24.app.domain.repository.ReminderScheduler
import uz.reja24.app.service.prayer.PrayerLocationUpdater
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

/** Internetsiz qidiruv natijasi: qaysi so'rov uchun va topilgan joylar. */
data class SearchResults(val query: String, val places: List<PlaceResult>)

/** Onlayn qidiruv holati. */
sealed interface OnlineSearchState {
    data object Idle : OnlineSearchState
    data object Loading : OnlineSearchState
    data class Found(val places: List<PlaceResult>) : OnlineSearchState
    data object NotFound : OnlineSearchState
    data object Unavailable : OnlineSearchState
}

sealed interface PrayerEvent {
    data class LocationResult(val result: PrayerLocationUpdater.Result) : PrayerEvent
    data object Error : PrayerEvent
}

@HiltViewModel
class PrayerSettingsViewModel @Inject constructor(
    private val prayerSettingsRepository: PrayerSettingsRepository,
    private val reminderScheduler: ReminderScheduler,
    private val locationUpdater: PrayerLocationUpdater,
    private val placeSearchRepository: PlaceSearchRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    val today: LocalDate get() = timeProvider.today()

    val overview: StateFlow<PrayerOverview?> = prayerSettingsRepository.settings
        .map { PrayerOverview.build(it, timeProvider.zone(), listOf(timeProvider.today())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _events = MutableSharedFlow<PrayerEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<PrayerEvent> = _events.asSharedFlow()

    private val _searchResults = MutableStateFlow(SearchResults("", emptyList()))
    /** Natijalar qaysi so'rov uchunligi bilan — "topilmadi" faqat qidiruv tugagach ko'rsatiladi. */
    val searchResults: StateFlow<SearchResults> = _searchResults.asStateFlow()

    private val _online = MutableStateFlow<OnlineSearchState>(OnlineSearchState.Idle)
    val online: StateFlow<OnlineSearchState> = _online.asStateFlow()

    private var searchJob: Job? = null
    private var onlineJob: Job? = null

    /** Har harf yozilganda — oldingi qidiruv bekor qilinadi, qisqa pauzadan keyin qidiriladi. */
    fun search(query: String, language: AppLanguage) {
        searchJob?.cancel()
        onlineJob?.cancel()
        _online.value = OnlineSearchState.Idle
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _searchResults.value = SearchResults(query, placeSearchRepository.searchOffline(query, language))
        }
    }

    fun searchOnline(query: String, locale: Locale) {
        if (query.trim().length < PlaceSearch.MIN_QUERY) return
        onlineJob?.cancel()
        _online.value = OnlineSearchState.Loading
        onlineJob = viewModelScope.launch {
            _online.value = when (val result = placeSearchRepository.searchOnline(query.trim(), locale)) {
                is PlaceSearchRepository.OnlineResult.Found -> OnlineSearchState.Found(result.places)
                PlaceSearchRepository.OnlineResult.NotFound -> OnlineSearchState.NotFound
                PlaceSearchRepository.OnlineResult.Unavailable -> OnlineSearchState.Unavailable
            }
        }
    }

    /** Sozlamani o'zgartiradi va eslatmalarni darhol qayta rejalashtiradi. */
    fun update(transform: (PrayerSettings) -> PrayerSettings) {
        viewModelScope.launch {
            try {
                prayerSettingsRepository.update(transform)
                reminderScheduler.rescheduleAll()
            } catch (e: Exception) {
                _events.emit(PrayerEvent.Error)
            }
        }
    }

    fun refreshLocation() {
        if (_refreshing.value) return
        _refreshing.value = true
        viewModelScope.launch {
            try {
                val result = locationUpdater.refresh()
                if (result == PrayerLocationUpdater.Result.UPDATED) reminderScheduler.rescheduleAll()
                _events.emit(PrayerEvent.LocationResult(result))
            } catch (e: Exception) {
                _events.emit(PrayerEvent.Error)
            } finally {
                _refreshing.value = false
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 150L
    }
}
