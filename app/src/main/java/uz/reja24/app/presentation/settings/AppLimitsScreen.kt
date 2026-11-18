@file:OptIn(ExperimentalLayoutApi::class)

package uz.reja24.app.presentation.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.reja24.app.R
import uz.reja24.app.core.util.InstalledApp
import uz.reja24.app.core.util.InstalledAppsProvider
import uz.reja24.app.core.util.SystemWhitelist
import uz.reja24.app.core.util.TimeProvider
import uz.reja24.app.domain.model.DayControl
import uz.reja24.app.domain.repository.AppRule
import uz.reja24.app.domain.repository.AppRuleRepository
import uz.reja24.app.presentation.components.SectionLabel
import uz.reja24.app.presentation.schedule.RejaChip
import uz.reja24.app.presentation.theme.RejaTheme
import javax.inject.Inject

data class AppLimitsUiState(
    val isLoading: Boolean = true,
    val rules: List<AppRule> = emptyList(),
    /** Bugun ishlatilgan daqiqalar: paket → daqiqa. */
    val usedMinutes: Map<String, Int> = emptyMap(),
    /** Qoida qo'shish mumkin bo'lgan ilovalar (qoidasi borlar va tizim ilovalari chiqarilgan). */
    val candidates: List<InstalledApp> = emptyList()
)

/** Kunduzgi nazorat: har bir ilova uchun alohida kunlik vaqt chegarasi va "o'ylab ko'r" pauzasi. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AppLimitsViewModel @Inject constructor(
    private val installedAppsProvider: InstalledAppsProvider,
    private val repository: AppRuleRepository,
    timeProvider: TimeProvider,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val installed = MutableStateFlow<List<InstalledApp>?>(null)

    /** Kun almashsa — bugungi hisob yangilansin. */
    private val today = flow {
        while (true) {
            emit(timeProvider.today())
            delay(60_000)
        }
    }.distinctUntilChanged()

    val uiState: StateFlow<AppLimitsUiState> = combine(
        installed,
        repository.observeRules(),
        today.flatMapLatest { repository.observeUsage(it) }
    ) { apps, rules, usage ->
        val ruled = rules.mapTo(HashSet()) { it.packageName }
        val system = SystemWhitelist.packages(context)
        AppLimitsUiState(
            isLoading = apps == null,
            rules = rules,
            usedMinutes = usage.mapValues { (_, seconds) -> (seconds / 60).toInt() },
            candidates = apps.orEmpty().filter { it.packageName !in ruled && it.packageName !in system }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppLimitsUiState())

    init {
        viewModelScope.launch { installed.value = installedAppsProvider.launcherApps() }
    }

    /** Yangi ilova: standart — kuniga 30 daqiqa va pauza yoqilgan. */
    fun add(app: InstalledApp) = save(AppRule(app.packageName, app.label, DEFAULT_LIMIT, pauseOn = true))

    fun setLimit(rule: AppRule, minutes: Int) = save(rule.copy(dailyLimitMinutes = minutes))

    fun setPause(rule: AppRule, on: Boolean) = save(rule.copy(pauseOn = on))

    fun remove(rule: AppRule) {
        viewModelScope.launch { runCatching { repository.removeRule(rule.packageName) } }
    }

    private fun save(rule: AppRule) {
        viewModelScope.launch { runCatching { repository.saveRule(rule) } }
    }

    suspend fun icon(packageName: String, sizePx: Int): ImageBitmap? = installedAppsProvider.icon(packageName, sizePx)

    companion object {
        const val DEFAULT_LIMIT = 30
        val LIMIT_OPTIONS = listOf(0, 15, 30, 45, 60, 90, 120)
    }
}

@Composable
fun AppLimitsScreen(onBack: () -> Unit, viewModel: AppLimitsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = RejaTheme.colors
    Column(Modifier.fillMaxSize()) {
        SubScreenTopBar(title = stringResource(R.string.day_control_title), onBack = onBack)
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.accent)
            }
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.day_control_hint, DayControl.PAUSE_SECONDS.toString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            }
            if (state.rules.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.day_control_rules), modifier = Modifier.padding(top = 8.dp)) }
                items(state.rules, key = { "rule-" + it.packageName }) { rule ->
                    RuleCard(
                        rule = rule,
                        usedMinutes = state.usedMinutes[rule.packageName] ?: 0,
                        loadIcon = viewModel::icon,
                        onLimit = { viewModel.setLimit(rule, it) },
                        onPause = { viewModel.setPause(rule, it) },
                        onRemove = { viewModel.remove(rule) }
                    )
                }
            }
            item { SectionLabel(stringResource(R.string.day_control_add), modifier = Modifier.padding(top = 8.dp)) }
            items(state.candidates, key = { "app-" + it.packageName }) { app ->
                CandidateRow(app = app, loadIcon = viewModel::icon, onAdd = { viewModel.add(app) })
            }
        }
    }
}

@Composable
private fun AppIcon(packageName: String, loadIcon: suspend (String, Int) -> ImageBitmap?) {
    val sizePx = with(LocalDensity.current) { 36.dp.roundToPx() }
    val icon by produceState<ImageBitmap?>(initialValue = null, packageName) { value = loadIcon(packageName, sizePx) }
    val bitmap = icon
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(36.dp))
    } else {
        Icon(Icons.Outlined.Apps, contentDescription = null, tint = RejaTheme.colors.textMuted, modifier = Modifier.size(36.dp))
    }
}

@Composable
private fun RuleCard(
    rule: AppRule,
    usedMinutes: Int,
    loadIcon: suspend (String, Int) -> ImageBitmap?,
    onLimit: (Int) -> Unit,
    onPause: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    val colors = RejaTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(16.dp))
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(rule.packageName, loadIcon)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(rule.appName, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Text(
                    text = if (rule.hasLimit) {
                        stringResource(R.string.day_control_used_of, usedMinutes.toString(), rule.dailyLimitMinutes.toString())
                    } else {
                        stringResource(R.string.day_control_used, usedMinutes.toString())
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (rule.hasLimit && usedMinutes >= rule.dailyLimitMinutes) colors.accentText else colors.textMuted
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.day_control_remove, rule.appName), tint = colors.textMuted)
            }
        }
        Text(stringResource(R.string.day_control_limit), style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AppLimitsViewModel.LIMIT_OPTIONS.forEach { minutes ->
                RejaChip(
                    selected = rule.dailyLimitMinutes == minutes,
                    label = if (minutes == 0) stringResource(R.string.day_control_no_limit)
                    else stringResource(R.string.day_control_minutes, minutes.toString()),
                    onClick = { onLimit(minutes) }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable { onPause(!rule.pauseOn) }
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.day_control_pause), style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                Text(
                    stringResource(R.string.day_control_pause_desc, DayControl.PAUSE_SECONDS.toString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            }
            Switch(
                checked = rule.pauseOn,
                onCheckedChange = onPause,
                colors = SwitchDefaults.colors(checkedTrackColor = colors.accent, checkedThumbColor = Color.White)
            )
        }
    }
}

@Composable
private fun CandidateRow(app: InstalledApp, loadIcon: suspend (String, Int) -> ImageBitmap?, onAdd: () -> Unit) {
    val colors = RejaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(colors.surface, RoundedCornerShape(14.dp))
            .clickable(onClick = onAdd)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(app.packageName, loadIcon)
        Spacer(Modifier.width(12.dp))
        Text(app.label, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.day_control_add_app, app.label), tint = colors.accent)
    }
}
