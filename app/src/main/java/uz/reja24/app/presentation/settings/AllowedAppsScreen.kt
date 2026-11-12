package uz.reja24.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.reja24.app.R
import uz.reja24.app.core.util.InstalledApp
import uz.reja24.app.core.util.InstalledAppsProvider
import uz.reja24.app.core.util.TimeFormat
import uz.reja24.app.core.util.TimeProvider
import uz.reja24.app.domain.model.RoutineLock
import uz.reja24.app.domain.repository.RoutineSettingsRepository
import uz.reja24.app.domain.repository.UserPreferencesRepository
import uz.reja24.app.presentation.components.LockBanner
import uz.reja24.app.presentation.theme.RejaTheme
import java.time.LocalTime
import javax.inject.Inject

data class AllowedAppsUiState(
    val isLoading: Boolean = true,
    val apps: List<InstalledApp> = emptyList(),
    val allowed: Set<String> = emptySet(),
    /** Rejim qulfi: bu vaqtgacha yangi ilovaga ruxsat berib bo'lmaydi (olib tashlash mumkin). */
    val lockedUntil: LocalTime? = null
)

/** Tungi "faqat kerakli" rejimda ochiq qoladigan ilovalarni tanlash. */
@HiltViewModel
class AllowedAppsViewModel @Inject constructor(
    private val installedAppsProvider: InstalledAppsProvider,
    private val userPreferencesRepository: UserPreferencesRepository,
    routineSettingsRepository: RoutineSettingsRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val installed = MutableStateFlow<List<InstalledApp>?>(null)

    val uiState: StateFlow<AllowedAppsUiState> = combine(
        installed,
        userPreferencesRepository.preferences,
        routineSettingsRepository.observeSettings(),
        flow { while (true) { emit(Unit); delay(LOCK_CHECK_MS) } }
    ) { apps, prefs, routine, _ ->
        AllowedAppsUiState(
            isLoading = apps == null,
            // Ruxsat etilganlar ro'yxat boshida
            apps = apps.orEmpty().sortedByDescending { it.packageName in prefs.allowedPackages },
            allowed = prefs.allowedPackages,
            lockedUntil = routine.wakeTime.takeIf { RoutineLock.isLocked(routine, timeProvider.now().toLocalTime()) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AllowedAppsUiState())

    init {
        viewModelScope.launch { installed.value = installedAppsProvider.launcherApps() }
    }

    fun toggle(app: InstalledApp) {
        val state = uiState.value
        val isAllowed = app.packageName in state.allowed
        // Ruxsat berish — blokni bo'shashtiradi: qulf vaqtida mumkin emas. Olib tashlash doim mumkin.
        if (!isAllowed && state.lockedUntil != null) return
        viewModelScope.launch {
            runCatching {
                userPreferencesRepository.setAllowedPackages(
                    if (isAllowed) state.allowed - app.packageName else state.allowed + app.packageName
                )
            }
        }
    }

    suspend fun icon(packageName: String, sizePx: Int): ImageBitmap? = installedAppsProvider.icon(packageName, sizePx)

    private companion object {
        const val LOCK_CHECK_MS = 30_000L
    }
}

@Composable
fun AllowedAppsScreen(onBack: () -> Unit, viewModel: AllowedAppsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = RejaTheme.colors
    Column(Modifier.fillMaxSize()) {
        SubScreenTopBar(title = stringResource(R.string.allow_only_apps_title), onBack = onBack)
        Text(
            text = stringResource(R.string.allow_only_apps_hint),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        state.lockedUntil?.let { until ->
            LockBanner(
                text = stringResource(R.string.allow_only_locked_banner, TimeFormat.time(until)),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.accent)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.apps, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        checked = app.packageName in state.allowed,
                        loadIcon = viewModel::icon,
                        onToggle = { viewModel.toggle(app) }
                    )
                }
            }
        }
    }
}
