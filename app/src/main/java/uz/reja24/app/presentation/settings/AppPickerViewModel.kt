package uz.reja24.app.presentation.settings

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.reja24.app.core.util.InstalledApp
import uz.reja24.app.core.util.InstalledAppsProvider
import uz.reja24.app.domain.model.BlockedApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import uz.reja24.app.core.util.TimeProvider
import uz.reja24.app.domain.model.RoutineLock
import uz.reja24.app.domain.repository.BlockedAppRepository
import uz.reja24.app.domain.repository.RoutineSettingsRepository
import uz.reja24.app.domain.repository.UserPreferencesRepository
import java.time.LocalTime
import javax.inject.Inject

data class AppPickerUiState(
    val isLoading: Boolean,
    val query: String,
    val apps: List<InstalledApp>,
    val blocked: Set<String>,
    /** Rejim qulfi: bu vaqtgacha ilovani ro'yxatdan chiqarib bo'lmaydi (null — qulf yo'q). */
    val lockedUntil: LocalTime? = null
)

/** Bloklanadigan ilovalarni tanlash (qidiruv bilan). Tanishuvda ham, sozlamalarda ham ishlatiladi. */
@HiltViewModel
class AppPickerViewModel @Inject constructor(
    private val installedAppsProvider: InstalledAppsProvider,
    private val blockedAppRepository: BlockedAppRepository,
    routineSettingsRepository: RoutineSettingsRepository,
    userPreferencesRepository: UserPreferencesRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    /** Tanishuv paytida qulf yo'q (hali rejim sozlanmoqda); keyin — bloklash vaqtida qulf. */
    private val lockedUntil = combine(
        routineSettingsRepository.observeSettings(),
        userPreferencesRepository.preferences,
        flow { while (true) { emit(Unit); delay(LOCK_CHECK_MS) } }
    ) { routine, prefs, _ ->
        routine.wakeTime.takeIf {
            prefs.isOnboardingCompleted && RoutineLock.isLocked(routine, timeProvider.now().toLocalTime())
        }
    }

    private val installed = MutableStateFlow<List<InstalledApp>?>(null)
    private val query = MutableStateFlow("")

    val uiState: StateFlow<AppPickerUiState> = combine(
        installed,
        query,
        blockedAppRepository.observeBlockedPackages(),
        lockedUntil
    ) { apps, q, blocked, locked ->
        val filtered = apps.orEmpty().filter { q.isBlank() || it.label.contains(q.trim(), ignoreCase = true) }
        // Tanlanganlar ro'yxat boshida
        AppPickerUiState(
            isLoading = apps == null,
            query = q,
            apps = filtered.sortedByDescending { it.packageName in blocked },
            blocked = blocked,
            lockedUntil = locked
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPickerUiState(true, "", emptyList(), emptySet()))

    private val _queryText = MutableStateFlow("")
    val queryText: StateFlow<String> = _queryText.asStateFlow()

    init {
        viewModelScope.launch {
            val apps = installedAppsProvider.launcherApps()
            installed.value = apps
            // Birinchi marta (jadval butunlay bo'sh): telefondagi chalg'ituvchi ilovalarni taklif qilamiz
            if (blockedAppRepository.observeAll().first().isEmpty()) {
                val suggested = apps
                    .filter { it.packageName in InstalledAppsProvider.SUGGESTED_DISTRACTIONS }
                    .map { BlockedApp(it.packageName, it.label, isBlocked = true) }
                if (suggested.isNotEmpty()) {
                    runCatching { blockedAppRepository.saveApps(suggested) }
                }
            }
        }
    }

    fun onQueryChange(value: String) {
        _queryText.value = value
        query.value = value
    }

    fun toggle(app: InstalledApp) {
        viewModelScope.launch {
            runCatching {
                if (app.packageName in uiState.value.blocked) {
                    // Rejim qulfi: bloklash vaqtida ro'yxatdan chiqarib bo'lmaydi (qo'shish mumkin)
                    if (uiState.value.lockedUntil != null) return@runCatching
                    blockedAppRepository.setBlocked(app.packageName, false)
                } else {
                    blockedAppRepository.saveApp(BlockedApp(app.packageName, app.label, isBlocked = true))
                }
            }
        }
    }

    suspend fun icon(packageName: String, sizePx: Int): ImageBitmap? =
        installedAppsProvider.icon(packageName, sizePx)

    private companion object {
        const val LOCK_CHECK_MS = 30_000L
    }
}
