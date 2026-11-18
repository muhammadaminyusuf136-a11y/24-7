@file:OptIn(ExperimentalLayoutApi::class)

package uz.reja24.app.presentation.prayer

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.reja24.app.R
import uz.reja24.app.core.util.DeviceLocation
import uz.reja24.app.core.util.DurationText
import uz.reja24.app.core.util.LocaleHelper
import uz.reja24.app.core.util.TimeFormat
import uz.reja24.app.domain.prayer.AsrMadhab
import uz.reja24.app.domain.prayer.CalculationMethod
import uz.reja24.app.domain.prayer.Cities
import uz.reja24.app.domain.prayer.PlaceResult
import uz.reja24.app.domain.prayer.PlaceSearch
import uz.reja24.app.domain.prayer.Prayer
import uz.reja24.app.domain.prayer.PrayerOverview
import uz.reja24.app.domain.prayer.PrayerSchedule
import uz.reja24.app.domain.prayer.PrayerSettings
import uz.reja24.app.domain.prayer.ResolvedLocation
import uz.reja24.app.presentation.components.AlarmSoundRow
import uz.reja24.app.presentation.components.RejaCard
import uz.reja24.app.presentation.components.SectionLabel
import uz.reja24.app.presentation.components.SwitchSettingRow
import uz.reja24.app.presentation.components.ValueSettingRow
import uz.reja24.app.presentation.components.madhabNameRes
import uz.reja24.app.presentation.components.methodNameRes
import uz.reja24.app.presentation.components.prayerNameRes
import uz.reja24.app.presentation.home.locationName
import uz.reja24.app.presentation.schedule.RejaChip
import uz.reja24.app.presentation.settings.SubScreenTopBar
import uz.reja24.app.presentation.theme.PrayerGreen
import uz.reja24.app.presentation.theme.RejaTheme
import uz.reja24.app.service.prayer.PrayerLocationUpdater

private val REMINDER_OPTIONS = listOf(0, 5, 10, 15, 20, 30)

@Composable
fun PrayerSettingsScreen(
    onBack: () -> Unit,
    viewModel: PrayerSettingsViewModel = hiltViewModel()
) {
    val overview by viewModel.overview.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var hasPermission by remember { mutableStateOf(DeviceLocation.hasPermission(context)) }
    var pickCity by remember { mutableStateOf(false) }
    var pickMethod by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { hasPermission = DeviceLocation.hasPermission(context) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) viewModel.refreshLocation()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val message = when (event) {
                PrayerEvent.Error -> R.string.error_generic
                is PrayerEvent.LocationResult -> when (event.result) {
                    PrayerLocationUpdater.Result.UPDATED, PrayerLocationUpdater.Result.UNCHANGED -> R.string.prayer_location_updated
                    PrayerLocationUpdater.Result.NO_PERMISSION -> R.string.prayer_location_permission_desc
                    PrayerLocationUpdater.Result.LOCATION_OFF -> R.string.prayer_location_off
                    PrayerLocationUpdater.Result.NOT_FOUND -> R.string.prayer_location_not_found
                    PrayerLocationUpdater.Result.MANUAL_MODE -> null
                }
            }
            if (message != null) snackbar.showSnackbar(context.getString(message))
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            SubScreenTopBar(title = stringResource(R.string.prayer_settings_title), onBack = onBack)
            val current = overview
            if (current == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RejaTheme.colors.accent)
                }
            } else {
                PrayerSettingsContent(
                    overview = current,
                    today = viewModel.today,
                    hasPermission = hasPermission,
                    refreshing = refreshing,
                    onUpdate = viewModel::update,
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                    onRefreshLocation = viewModel::refreshLocation,
                    onPickCity = { pickCity = true },
                    onPickMethod = { pickMethod = true }
                )
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    val current = overview
    if (pickCity) {
        PlaceSearchDialog(
            viewModel = viewModel,
            onDismiss = { pickCity = false },
            onPick = { place ->
                pickCity = false
                viewModel.update { it.copy(autoLocation = false, manualPlace = place.toManualPlace()) }
            }
        )
    }
    if (pickMethod && current != null) {
        MethodDialog(
            selected = current.settings.method,
            autoMethod = current.location?.let { Cities.defaultsFor(it.countryCode).first },
            onDismiss = { pickMethod = false },
            onPick = { method ->
                pickMethod = false
                viewModel.update { it.copy(method = method) }
            }
        )
    }
}

@Composable
private fun PrayerSettingsContent(
    overview: PrayerOverview,
    today: java.time.LocalDate,
    hasPermission: Boolean,
    refreshing: Boolean,
    onUpdate: ((PrayerSettings) -> PrayerSettings) -> Unit,
    onRequestPermission: () -> Unit,
    onRefreshLocation: () -> Unit,
    onPickCity: () -> Unit,
    onPickMethod: () -> Unit
) {
    val settings = overview.settings
    val location = overview.location
    val context = LocalContext.current
    val colors = RejaTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        RejaCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
            SwitchSettingRow(
                title = stringResource(R.string.prayer_enabled),
                subtitle = stringResource(R.string.prayer_enabled_desc),
                checked = settings.isEnabled,
                onChange = { on -> onUpdate { it.copy(isEnabled = on) } }
            )
        }
        if (!settings.isEnabled) return@Column

        // ---------- Joylashuv ----------
        SectionLabel(stringResource(R.string.prayer_location))
        RejaCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = PrayerGreen)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = location?.let { locationName(it) } ?: stringResource(R.string.prayer_location_unknown),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    if (location != null) {
                        Text(
                            text = stringResource(
                                when (location.source) {
                                    ResolvedLocation.Source.DETECTED -> R.string.prayer_location_source_detected
                                    ResolvedLocation.Source.TIME_ZONE -> R.string.prayer_location_source_zone
                                    ResolvedLocation.Source.MANUAL -> R.string.prayer_location_source_manual
                                }
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                }
            }
            Spacer(Modifier.padding(top = 10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RejaChip(
                    selected = settings.autoLocation,
                    label = stringResource(R.string.prayer_location_auto),
                    onClick = { onUpdate { it.copy(autoLocation = true) } }
                )
                RejaChip(
                    selected = !settings.autoLocation,
                    label = stringResource(R.string.prayer_location_manual),
                    onClick = onPickCity
                )
            }
            if (settings.autoLocation) {
                Spacer(Modifier.padding(top = 8.dp))
                Text(
                    text = stringResource(R.string.prayer_location_auto_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
                Spacer(Modifier.padding(top = 4.dp))
                if (hasPermission) {
                    TextButton(onClick = onRefreshLocation, enabled = !refreshing) {
                        Text(stringResource(if (refreshing) R.string.prayer_location_refreshing else R.string.prayer_location_refresh))
                    }
                } else {
                    TextButton(onClick = onRequestPermission) {
                        Text(stringResource(R.string.prayer_location_permission))
                    }
                    Text(
                        text = stringResource(R.string.prayer_location_permission_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                }
            }
        }

        // ---------- Bugungi vaqtlar ----------
        val day = overview.days[today]
        if (day != null) {
            SectionLabel(stringResource(R.string.prayer_today))
            RejaCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 4.dp)) {
                Prayer.entries.forEachIndexed { index, prayer ->
                    if (index > 0) HorizontalDivider(color = colors.border, modifier = Modifier.padding(horizontal = 16.dp))
                    PrayerTimeRow(
                        prayer = prayer,
                        time = TimeFormat.time(day.time(prayer).toLocalTime()),
                        notify = prayer !in settings.mutedPrayers,
                        alarm = prayer in settings.alarmPrayers,
                        onToggleNotify = { on ->
                            onUpdate { s -> s.copy(mutedPrayers = if (on) s.mutedPrayers - prayer else s.mutedPrayers + prayer) }
                        },
                        onToggleAlarm = { on ->
                            onUpdate { s -> s.copy(alarmPrayers = if (on) s.alarmPrayers + prayer else s.alarmPrayers - prayer) }
                        }
                    )
                }
            }
            Text(
                text = stringResource(R.string.prayer_icons_hint),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )
        }

        // ---------- Eslatmalar ----------
        SectionLabel(stringResource(R.string.prayer_reminder))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            REMINDER_OPTIONS.forEach { minutes ->
                RejaChip(
                    selected = settings.reminderMinutes == minutes,
                    label = if (minutes == 0) stringResource(R.string.editor_reminder_off) else DurationText.format(context.resources, minutes),
                    onClick = { onUpdate { it.copy(reminderMinutes = minutes) } }
                )
            }
        }
        RejaCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
            SwitchSettingRow(
                title = stringResource(R.string.prayer_notify_at_time),
                subtitle = stringResource(R.string.prayer_notify_at_time_desc),
                checked = settings.notifyAtTime,
                onChange = { on -> onUpdate { it.copy(notifyAtTime = on) } }
            )
            HorizontalDivider(color = colors.border, modifier = Modifier.padding(horizontal = 16.dp))
            SwitchSettingRow(
                title = stringResource(R.string.azkar_setting),
                subtitle = stringResource(R.string.azkar_setting_desc),
                checked = settings.azkarOn,
                onChange = { on -> onUpdate { it.copy(azkarOn = on) } }
            )
            HorizontalDivider(color = colors.border, modifier = Modifier.padding(horizontal = 16.dp))
            AlarmSoundRow(
                title = stringResource(R.string.prayer_alarm_sound),
                soundUri = settings.alarmSoundUri,
                onPicked = { uri -> onUpdate { it.copy(alarmSoundUri = uri) } }
            )
        }

        // ---------- Hisoblash ----------
        SectionLabel(stringResource(R.string.prayer_calculation))
        RejaCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
            val method = location?.let { PrayerSchedule.methodFor(settings, it) }
            ValueSettingRow(
                title = stringResource(R.string.prayer_method),
                value = when {
                    method == null -> "—"
                    settings.method == null -> stringResource(R.string.prayer_auto, stringResource(methodNameRes(method)))
                    else -> stringResource(methodNameRes(method))
                },
                onClick = onPickMethod
            )
        }
        Text(stringResource(R.string.prayer_madhab), style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val autoMadhab = location?.let { Cities.defaultsFor(it.countryCode).second }
            RejaChip(
                selected = settings.madhab == null,
                label = if (autoMadhab != null) {
                    stringResource(R.string.prayer_auto, stringResource(madhabNameRes(autoMadhab)))
                } else {
                    stringResource(R.string.prayer_auto_plain)
                },
                onClick = { onUpdate { it.copy(madhab = null) } }
            )
            listOf(AsrMadhab.HANAFI, AsrMadhab.SHAFII).forEach { madhab ->
                RejaChip(
                    selected = settings.madhab == madhab,
                    label = stringResource(madhabNameRes(madhab)),
                    onClick = { onUpdate { it.copy(madhab = madhab) } }
                )
            }
        }

        // ---------- Daqiqaga to'g'rilash ----------
        SectionLabel(stringResource(R.string.prayer_adjust))
        Text(stringResource(R.string.prayer_adjust_desc), style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
        RejaCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 4.dp)) {
            Prayer.entries.forEachIndexed { index, prayer ->
                if (index > 0) HorizontalDivider(color = colors.border, modifier = Modifier.padding(horizontal = 16.dp))
                val value = settings.adjustments[prayer] ?: 0
                StepperRow(
                    title = stringResource(prayerNameRes(prayer)),
                    valueText = stringResource(R.string.prayer_adjust_value, signed(value)),
                    canDecrease = value > -PrayerSettings.MAX_ADJUSTMENT_MINUTES,
                    canIncrease = value < PrayerSettings.MAX_ADJUSTMENT_MINUTES,
                    onChange = { delta ->
                        onUpdate { s ->
                            val next = ((s.adjustments[prayer] ?: 0) + delta)
                                .coerceIn(-PrayerSettings.MAX_ADJUSTMENT_MINUTES, PrayerSettings.MAX_ADJUSTMENT_MINUTES)
                            s.copy(adjustments = s.adjustments + (prayer to next))
                        }
                    }
                )
            }
        }

        // ---------- Hijriy sana ----------
        SectionLabel(stringResource(R.string.hijri_adjust))
        RejaCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 4.dp)) {
            val value = settings.hijriAdjustmentDays
            StepperRow(
                title = stringResource(R.string.hijri_adjust_title),
                valueText = stringResource(R.string.hijri_adjust_value, signed(value)),
                canDecrease = value > -PrayerSettings.MAX_HIJRI_ADJUSTMENT,
                canIncrease = value < PrayerSettings.MAX_HIJRI_ADJUSTMENT,
                onChange = { delta ->
                    onUpdate { s ->
                        s.copy(
                            hijriAdjustmentDays = (s.hijriAdjustmentDays + delta)
                                .coerceIn(-PrayerSettings.MAX_HIJRI_ADJUSTMENT, PrayerSettings.MAX_HIJRI_ADJUSTMENT)
                        )
                    }
                }
            )
        }
        Text(stringResource(R.string.hijri_adjust_desc), style = MaterialTheme.typography.bodySmall, color = colors.textMuted)

        Text(
            text = stringResource(R.string.prayer_accuracy_note),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}

/** "+3", "0", "−2" — raqamlar har doim lotincha. */
private fun signed(value: Int): String = when {
    value > 0 -> "+$value"
    value < 0 -> "−${-value}"
    else -> "0"
}

@Composable
private fun PrayerTimeRow(
    prayer: Prayer,
    time: String,
    notify: Boolean,
    alarm: Boolean,
    onToggleNotify: (Boolean) -> Unit,
    onToggleAlarm: (Boolean) -> Unit
) {
    val colors = RejaTheme.colors
    val name = stringResource(prayerNameRes(prayer))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Text(time, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        if (prayer.isObligatory) {
            val notifyLabel = stringResource(R.string.cd_prayer_notify, name)
            IconToggleButton(
                checked = notify,
                onCheckedChange = onToggleNotify,
                modifier = Modifier.semantics { contentDescription = notifyLabel }
            ) {
                Icon(
                    if (notify) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff,
                    contentDescription = null,
                    tint = if (notify) PrayerGreen else colors.textMuted
                )
            }
            val alarmLabel = stringResource(R.string.cd_prayer_alarm, name)
            IconToggleButton(
                checked = alarm,
                onCheckedChange = onToggleAlarm,
                modifier = Modifier.semantics { contentDescription = alarmLabel }
            ) {
                Icon(
                    Icons.Outlined.Alarm,
                    contentDescription = null,
                    tint = if (alarm) colors.accent else colors.textMuted.copy(alpha = 0.5f)
                )
            }
        } else {
            // Quyosh chiqishi namoz emas — eslatma tugmalari yo'q, lekin ustunlar tekis tursin
            Spacer(Modifier.width(96.dp))
        }
    }
}

@Composable
private fun StepperRow(
    title: String,
    valueText: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onChange: (Int) -> Unit
) {
    val colors = RejaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange(-1) }, enabled = canDecrease) {
            Text("−", style = MaterialTheme.typography.titleLarge, color = if (canDecrease) colors.textPrimary else colors.textMuted)
        }
        Text(
            text = valueText,
            style = MaterialTheme.typography.titleSmall,
            color = colors.accentText,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(84.dp)
        )
        IconButton(onClick = { onChange(1) }, enabled = canIncrease) {
            Text("+", style = MaterialTheme.typography.titleLarge, color = if (canIncrease) colors.textPrimary else colors.textMuted)
        }
    }
}

@Composable
private fun PlaceSearchDialog(
    viewModel: PrayerSettingsViewModel,
    onDismiss: () -> Unit,
    onPick: (PlaceResult) -> Unit
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val language = LocaleHelper.currentLanguage(context)
    val colors = RejaTheme.colors
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val online by viewModel.online.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    // Oyna har ochilganda oldingi natijalar tozalanadi
    LaunchedEffect(Unit) { viewModel.search("", language) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.prayer_location_manual),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { value ->
                        query = value.take(MAX_QUERY)
                        viewModel.search(query, language)
                    },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    placeholder = { Text(stringResource(R.string.prayer_city_search)) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                // Eski so'rov natijasi yangi so'rov yozilayotganda ham ko'rinib turadi (miltillamaydi)
                val results = searchResults.places
                val searchDone = searchResults.query == query
                LazyColumn(Modifier.weight(1f)) {
                    items(results, key = { "o-${it.name}-${it.latitude}-${it.longitude}" }) { place ->
                        PlaceRow(place, locale, onPick)
                    }
                    if (searchDone && query.trim().length >= PlaceSearch.MIN_QUERY && results.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.prayer_search_offline_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textMuted,
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                            )
                        }
                    }
                    // ---------- Onlayn qidiruv ----------
                    if (query.trim().length >= PlaceSearch.MIN_QUERY) {
                        item {
                            HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 8.dp))
                            TextButton(
                                onClick = { viewModel.searchOnline(query, locale) },
                                enabled = online !is OnlineSearchState.Loading
                            ) {
                                Icon(Icons.Outlined.TravelExplore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(
                                        if (online is OnlineSearchState.Loading) R.string.prayer_search_searching
                                        else R.string.prayer_search_online
                                    )
                                )
                            }
                            Text(
                                text = stringResource(R.string.prayer_search_online_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                        when (val state = online) {
                            is OnlineSearchState.Found -> items(state.places, key = { "n-${it.name}-${it.latitude}-${it.longitude}" }) { place ->
                                PlaceRow(place, locale, onPick)
                            }
                            OnlineSearchState.NotFound -> item {
                                Text(
                                    stringResource(R.string.prayer_search_online_failed),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textMuted,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            OnlineSearchState.Unavailable -> item {
                                Text(
                                    stringResource(R.string.prayer_search_online_unavailable),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textMuted,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            else -> Unit
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.prayer_data_credit),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun PlaceRow(place: PlaceResult, locale: java.util.Locale, onPick: (PlaceResult) -> Unit) {
    val colors = RejaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable { onPick(place) }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = PrayerGreen, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(place.name, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
            val country = PlaceSearch.countryName(place.countryCode, locale)
            if (country.isNotEmpty()) {
                Text(country, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
        }
    }
}

private const val MAX_QUERY = 40

@Composable
private fun MethodDialog(
    selected: CalculationMethod?,
    autoMethod: CalculationMethod?,
    onDismiss: () -> Unit,
    onPick: (CalculationMethod?) -> Unit
) {
    val colors = RejaTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text(stringResource(R.string.prayer_method)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val options: List<CalculationMethod?> = listOf(null) + CalculationMethod.entries
                options.forEach { method ->
                    val label = when {
                        method != null -> stringResource(methodNameRes(method))
                        autoMethod != null -> stringResource(R.string.prayer_auto, stringResource(methodNameRes(autoMethod)))
                        else -> stringResource(R.string.prayer_auto_plain)
                    }
                    val isSelected = method == selected
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) colors.background else colors.textPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) colors.textPrimary else colors.surface, RoundedCornerShape(12.dp))
                            .clickable { onPick(method) }
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        }
    )
}
