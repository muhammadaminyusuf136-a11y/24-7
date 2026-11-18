@file:OptIn(ExperimentalLayoutApi::class)

package uz.reja24.app.presentation.settings

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.reja24.app.BuildConfig
import uz.reja24.app.R
import uz.reja24.app.core.util.DurationText
import uz.reja24.app.core.util.NightGrayscale
import uz.reja24.app.core.util.TimeFormat
import uz.reja24.app.domain.model.AppLanguage
import uz.reja24.app.domain.model.BackgroundStyle
import uz.reja24.app.domain.model.RoutineSettings
import uz.reja24.app.domain.model.DateDisplay
import uz.reja24.app.domain.model.VoiceGender
import uz.reja24.app.presentation.components.LockBanner
import uz.reja24.app.presentation.components.RejaCard
import uz.reja24.app.presentation.components.RejaTimePickerDialog
import uz.reja24.app.presentation.components.SectionLabel
import uz.reja24.app.presentation.schedule.RejaChip
import uz.reja24.app.presentation.theme.GraceBackground
import uz.reja24.app.presentation.theme.GreenBackground
import uz.reja24.app.presentation.theme.NavyBackground
import uz.reja24.app.presentation.theme.LightBackground
import uz.reja24.app.presentation.theme.PrayerGreen
import uz.reja24.app.presentation.theme.RejaTheme
import uz.reja24.app.service.alarm.ReminderSound
import kotlinx.coroutines.launch
import java.time.LocalTime

/** Qaysi vaqt tahrirlanmoqda. */
private enum class TimeTarget { BLOCK, SLEEP, WAKE }

/** Qaysi eslatma bosqichi tahrirlanmoqda. */
private enum class StageTarget { STAGE_1, STAGE_2, STAGE_3 }

@Composable
fun SettingsScreen(
    onOpenAppPicker: () -> Unit,
    onOpenReliability: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenPrayer: () -> Unit,
    onOpenAllowedApps: () -> Unit,
    onOpenAppLimits: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var timeTarget by remember { mutableStateOf<TimeTarget?>(null) }
    var stageTarget by remember { mutableStateOf<StageTarget?>(null) }
    var secondReminderDialog by remember { mutableStateOf(false) }
    var previewing by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.errors.collect { snackbar.showSnackbar(context.getString(R.string.error_generic)) }
    }
    LaunchedEffect(Unit) {
        viewModel.locked.collect { until -> snackbar.showSnackbar(context.getString(R.string.routine_locked_toast, TimeFormat.time(until))) }
    }
    val lockedUntil by viewModel.lockedUntil.collectAsStateWithLifecycle()

    // Rington tanlash oynasi (Android'ning o'zi)
    val ringtoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.let {
                IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            }
            viewModel.updateRoutine { routine -> routine.copy(ringtoneUri = uri?.toString()) }
        }
    }

    val current = state
    if (current == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = RejaTheme.colors.accent)
        }
        return
    }
    val routine = current.routine
    val prefs = current.preferences

    /** Tanlangan ovoz bilan namuna matnni o'qib beradi (rejim va rejalar eslatmasi uchun). */
    fun previewVoice(@androidx.annotation.StringRes sampleRes: Int) {
        if (previewing) return
        previewing = true
        val locale = configuration.locales[0]
        val sample = context.getString(sampleRes)
        scope.launch {
            val result = ReminderSound.speak(context, sample, locale, prefs.voiceGender)
            previewing = false
            when (result) {
                ReminderSound.SpeakResult.SPOKEN -> Unit
                ReminderSound.SpeakResult.LANGUAGE_MISSING ->
                    snackbar.showSnackbar(context.getString(R.string.voice_result_language_missing))
                ReminderSound.SpeakResult.ENGINE_MISSING ->
                    snackbar.showSnackbar(context.getString(R.string.voice_result_engine_missing))
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = RejaTheme.colors.textPrimary
            )

            // ---------- Til ----------
            Section(stringResource(R.string.settings_language)) {
                LanguageGrid(selected = prefs.language.takeIf { prefs.isLanguageChosen }, onSelect = viewModel::setLanguage)
            }

            // ---------- Rang (mavzu va fon bitta tanlov) ----------
            Section(stringResource(R.string.settings_background)) {
                BackgroundChips(selected = prefs.backgroundStyle, onSelect = viewModel::setBackgroundStyle)
                Text(
                    text = stringResource(R.string.settings_background_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = RejaTheme.colors.textMuted
                )
            }

            // ---------- Sana: ikkalasi / milodiy / hijriy ----------
            Section(stringResource(R.string.settings_date)) {
                DateDisplayChips(selected = prefs.dateDisplay, onSelect = viewModel::setDateDisplay)
                Text(
                    text = stringResource(R.string.settings_date_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = RejaTheme.colors.textMuted
                )
            }

            // ---------- Namoz vaqtlari ----------
            RejaCard(onClick = onOpenPrayer, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Mosque, contentDescription = null, tint = PrayerGreen)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.prayer_settings_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = RejaTheme.colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = RejaTheme.colors.textMuted)
                }
            }

            // ---------- Rejim ----------
            Section(stringResource(R.string.settings_routine)) {
                lockedUntil?.let { until -> LockBanner(stringResource(R.string.routine_locked_banner, TimeFormat.time(until))) }
                RejaCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    SwitchRow(
                        title = stringResource(R.string.settings_routine_active),
                        subtitle = null,
                        checked = routine.isRoutineActive,
                        onChange = { on -> viewModel.updateRoutine { it.copy(isRoutineActive = on) } }
                    )
                    Divider()
                    ValueRow(stringResource(R.string.routine_block_start), TimeFormat.time(routine.blockStartTime)) { timeTarget = TimeTarget.BLOCK }
                    Divider()
                    ValueRow(stringResource(R.string.routine_sleep), TimeFormat.time(routine.sleepTime)) { timeTarget = TimeTarget.SLEEP }
                    Divider()
                    ValueRow(stringResource(R.string.routine_wake), TimeFormat.time(routine.wakeTime)) { timeTarget = TimeTarget.WAKE }
                    Divider()
                    SwitchRow(
                        title = stringResource(R.string.settings_strict_mode),
                        subtitle = stringResource(R.string.settings_strict_mode_desc),
                        checked = routine.isStrictModeOn,
                        onChange = { on -> viewModel.updateRoutine { it.copy(isStrictModeOn = on) } }
                    )
                }
            }

            // ---------- Ovozli eslatmalar (rejim va rejalar uchun bitta) ----------
            Section(stringResource(R.string.settings_voice_section)) {
                RejaCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    val voiceOn = routine.isVoiceReminderOn || prefs.taskVoiceOn
                    SwitchRow(
                        title = stringResource(R.string.settings_voice),
                        subtitle = stringResource(R.string.settings_voice_desc),
                        checked = voiceOn,
                        onChange = viewModel::setVoiceAll
                    )
                    if (voiceOn) {
                        VoiceGenderRow(
                            selected = prefs.voiceGender,
                            previewing = previewing,
                            onSelect = viewModel::setVoiceGender,
                            onPreview = { previewVoice(R.string.task_voice_preview_text) }
                        )
                    }
                }
            }

            // ---------- Eslatmalar ----------
            Section(stringResource(R.string.settings_reminders)) {
                RejaCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    ValueRow(stringResource(R.string.settings_stage_1), DurationText.format(context.resources, routine.reminderStage1Minutes)) { stageTarget = StageTarget.STAGE_1 }
                    Divider()
                    ValueRow(stringResource(R.string.settings_stage_2), DurationText.format(context.resources, routine.reminderStage2Minutes)) { stageTarget = StageTarget.STAGE_2 }
                    Divider()
                    ValueRow(stringResource(R.string.settings_stage_3), DurationText.format(context.resources, routine.reminderStage3Minutes)) { stageTarget = StageTarget.STAGE_3 }
                    Divider()
                    ValueRow(
                        title = stringResource(R.string.settings_ringtone),
                        value = ringtoneTitle(routine.ringtoneUri)
                    ) {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_ALARM)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            .putExtra(
                                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                routine.ringtoneUri?.let { Uri.parse(it) }
                            )
                        runCatching { ringtoneLauncher.launch(intent) }
                    }
                }
            }

            // ---------- Reja eslatmalari ----------
            Section(stringResource(R.string.settings_task_reminders)) {
                RejaCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    ValueRow(
                        stringResource(R.string.settings_task_second),
                        if (prefs.taskSecondReminderMinutes == 0) stringResource(R.string.editor_reminder_off)
                        else DurationText.format(context.resources, prefs.taskSecondReminderMinutes)
                    ) { secondReminderDialog = true }
                }
                Text(
                    text = stringResource(R.string.settings_task_reminders_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = RejaTheme.colors.textMuted
                )
            }

            // ---------- Bloklanadigan ilovalar ----------
            Section(stringResource(R.string.settings_blocked_apps)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    current.blockedApps.forEach { app ->
                        InputChip(
                            selected = false,
                            onClick = { viewModel.removeBlockedApp(app.packageName) },
                            label = { Text(app.appName) },
                            trailingIcon = {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.cd_remove_app, app.appName),
                                    modifier = Modifier.size(InputChipDefaults.IconSize)
                                )
                            },
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                    RejaChip(
                        selected = true,
                        label = "+ " + stringResource(R.string.action_add),
                        onClick = onOpenAppPicker
                    )
                }
            }

            // ---------- Tungi "faqat kerakli" rejim ----------
            Section(stringResource(R.string.allow_only_section)) {
                RejaCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    SwitchRow(
                        title = stringResource(R.string.allow_only_switch),
                        subtitle = stringResource(R.string.allow_only_desc),
                        checked = prefs.nightAllowOnly,
                        onChange = viewModel::setNightAllowOnly
                    )
                    if (prefs.nightAllowOnly) {
                        Divider()
                        ValueRow(
                            title = stringResource(R.string.allow_only_apps_title),
                            value = prefs.allowedPackages.size.toString(),
                            onClick = onOpenAllowedApps
                        )
                    }
                    Divider()
                    val grayPermitted = remember { NightGrayscale.isPermitted(context) }
                    SwitchRow(
                        title = stringResource(R.string.grayscale_switch),
                        subtitle = stringResource(if (grayPermitted) R.string.grayscale_desc else R.string.grayscale_no_permission),
                        checked = prefs.nightGrayscale && grayPermitted,
                        onChange = { on -> if (grayPermitted) viewModel.setNightGrayscale(on) }
                    )
                }
            }

            // ---------- Kunduzgi nazorat: kunlik chegara va "o'ylab ko'r" pauzasi ----------
            Section(stringResource(R.string.day_control_section)) {
                RejaCard(onClick = onOpenAppLimits, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.HourglassBottom, contentDescription = null, tint = RejaTheme.colors.accent)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.day_control_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = RejaTheme.colors.textPrimary
                            )
                            Text(
                                text = stringResource(R.string.day_control_card_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = RejaTheme.colors.textMuted
                            )
                        }
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = RejaTheme.colors.textMuted)
                    }
                }
            }

            // ---------- Zaxira nusxa ----------
            Section(stringResource(R.string.backup_section)) {
                BackupSection()
            }

            // ---------- Ishonchlilik ----------
            RejaCard(onClick = onOpenReliability, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Shield, contentDescription = null, tint = RejaTheme.colors.accent)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.settings_reliability_button),
                        style = MaterialTheme.typography.titleSmall,
                        color = RejaTheme.colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = RejaTheme.colors.textMuted)
                }
            }

            // ---------- Maxfiylik va versiya ----------
            RejaCard(onClick = onOpenPrivacy, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Policy, contentDescription = null, tint = RejaTheme.colors.textSecondary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.privacy_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = RejaTheme.colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = RejaTheme.colors.textMuted)
                }
            }
            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = RejaTheme.colors.textMuted,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    timeTarget?.let { target ->
        val (title, initial) = when (target) {
            TimeTarget.BLOCK -> stringResource(R.string.routine_block_start) to routine.blockStartTime
            TimeTarget.SLEEP -> stringResource(R.string.routine_sleep) to routine.sleepTime
            TimeTarget.WAKE -> stringResource(R.string.routine_wake) to routine.wakeTime
        }
        RejaTimePickerDialog(
            title = title,
            initial = initial,
            onDismiss = { timeTarget = null },
            onConfirm = { time ->
                viewModel.updateRoutine { it.withTime(target, time) }
                timeTarget = null
            }
        )
    }

    if (secondReminderDialog) {
        StageDialog(
            current = prefs.taskSecondReminderMinutes,
            title = stringResource(R.string.settings_task_second_dialog),
            options = listOf(0, 5, 10, 15),
            onDismiss = { secondReminderDialog = false },
            onSelect = { minutes ->
                viewModel.setTaskSecondReminder(minutes)
                secondReminderDialog = false
            }
        )
    }

    stageTarget?.let { target ->
        val currentValue = when (target) {
            StageTarget.STAGE_1 -> routine.reminderStage1Minutes
            StageTarget.STAGE_2 -> routine.reminderStage2Minutes
            StageTarget.STAGE_3 -> routine.reminderStage3Minutes
        }
        StageDialog(
            current = currentValue,
            onDismiss = { stageTarget = null },
            onSelect = { minutes ->
                viewModel.updateRoutine {
                    when (target) {
                        StageTarget.STAGE_1 -> it.copy(reminderStage1Minutes = minutes)
                        StageTarget.STAGE_2 -> it.copy(reminderStage2Minutes = minutes)
                        StageTarget.STAGE_3 -> it.copy(reminderStage3Minutes = minutes)
                    }
                }
                stageTarget = null
            }
        )
    }
}

@Composable
internal fun VoiceGenderRow(
    selected: VoiceGender,
    previewing: Boolean,
    onSelect: (VoiceGender) -> Unit,
    onPreview: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.settings_voice_gender), style = MaterialTheme.typography.bodyLarge, color = RejaTheme.colors.textPrimary)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RejaChip(selected = selected == VoiceGender.FEMALE, label = stringResource(R.string.voice_female), onClick = { onSelect(VoiceGender.FEMALE) })
            RejaChip(selected = selected == VoiceGender.MALE, label = stringResource(R.string.voice_male), onClick = { onSelect(VoiceGender.MALE) })
            RejaChip(
                selected = false,
                label = stringResource(if (previewing) R.string.voice_previewing else R.string.voice_preview),
                onClick = { if (!previewing) onPreview() }
            )
        }
    }
}

private fun RoutineSettings.withTime(target: TimeTarget, time: LocalTime): RoutineSettings = when (target) {
    TimeTarget.BLOCK -> copy(blockStartTime = time)
    TimeTarget.SLEEP -> copy(sleepTime = time)
    TimeTarget.WAKE -> copy(wakeTime = time)
}

@Composable
private fun ringtoneTitle(uri: String?): String {
    val context = LocalContext.current
    if (uri == null) return stringResource(R.string.settings_ringtone_default)
    return remember(uri) {
        runCatching { RingtoneManager.getRingtone(context, Uri.parse(uri))?.getTitle(context) }.getOrNull()
    } ?: stringResource(R.string.settings_ringtone_custom)
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(title)
        content()
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(color = RejaTheme.colors.border, modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun ValueRow(title: String, value: String, onClick: () -> Unit) {
    val colors = RejaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = colors.accentText)
    }
}

@Composable
internal fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = RejaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = colors.accent)
        )
    }
}

/** Tillar 2×2 grid — har biri o'z tilida yozilgan. Tanlangani qora fonda + belgi. */
@Composable
private fun LanguageGrid(selected: AppLanguage?, onSelect: (AppLanguage) -> Unit) {
    val names = mapOf(
        AppLanguage.UZBEK to R.string.language_name_uz,
        AppLanguage.ENGLISH to R.string.language_name_en,
        AppLanguage.RUSSIAN to R.string.language_name_ru,
        AppLanguage.ARABIC to R.string.language_name_ar
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AppLanguage.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { language ->
                    LanguageCard(
                        name = stringResource(names.getValue(language)),
                        selected = language == selected,
                        onClick = { onSelect(language) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageCard(name: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = RejaTheme.colors
    Row(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) colors.textPrimary else colors.surface)
            .border(1.dp, if (selected) colors.textPrimary else colors.border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) colors.background else colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(colors.accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
internal fun BackgroundChips(selected: BackgroundStyle, onSelect: (BackgroundStyle) -> Unit) {
    val options = listOf(
        BackgroundStyle.POWER to R.string.background_power,
        BackgroundStyle.GREEN to R.string.background_green,
        BackgroundStyle.GRACE to R.string.background_grace,
        BackgroundStyle.PURE to R.string.background_pure
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (style, label) ->
            SwatchChip(
                label = stringResource(label),
                selected = style == selected,
                onClick = { onSelect(style) },
                swatch = { BackgroundPreview(style) }
            )
        }
    }
}

/** Orqa fonning kichik namunasi — o'sha fonning o'z rangi. */
@Composable
private fun BackgroundPreview(style: BackgroundStyle) {
    val color = when (style) {
        BackgroundStyle.POWER -> NavyBackground
        BackgroundStyle.GREEN -> GreenBackground
        BackgroundStyle.GRACE -> GraceBackground
        BackgroundStyle.PURE -> LightBackground
    }
    Box(Modifier.fillMaxSize().background(color))
}

/** Sanalar: ikkalasi (milodiy + hijriy) / faqat milodiy / faqat hijriy. */
@Composable
internal fun DateDisplayChips(selected: DateDisplay, onSelect: (DateDisplay) -> Unit) {
    val options = listOf(
        DateDisplay.BOTH to R.string.date_display_both,
        DateDisplay.GREGORIAN to R.string.date_display_gregorian,
        DateDisplay.HIJRI to R.string.date_display_hijri
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (display, label) ->
            RejaChip(selected = display == selected, label = stringResource(label), onClick = { onSelect(display) })
        }
    }
}

@Composable
private fun SwatchChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    swatch: @Composable () -> Unit
) {
    val colors = RejaTheme.colors
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) colors.textPrimary else colors.surface)
            .border(1.dp, if (selected) colors.textPrimary else colors.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .border(1.dp, colors.border, CircleShape)
        ) { swatch() }
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) colors.background else colors.textPrimary)
    }
}

@Composable
private fun StageDialog(
    current: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    title: String = stringResource(R.string.settings_stage_dialog_title),
    /** 0 — "Yo'q" */
    options: List<Int> = listOf(5, 10, 15, 20, 30, 45, 60, 90, 120)
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RejaTheme.colors.surface,
        title = { Text(title) },
        text = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { minutes ->
                    RejaChip(
                        selected = minutes == current,
                        label = if (minutes == 0) stringResource(R.string.editor_reminder_off)
                        else DurationText.format(context.resources, minutes),
                        onClick = { onSelect(minutes) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        }
    )
}
