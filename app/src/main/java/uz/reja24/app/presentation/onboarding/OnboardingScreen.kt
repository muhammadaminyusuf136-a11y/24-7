package uz.reja24.app.presentation.onboarding

import uz.reja24.app.presentation.settings.VoiceGenderRow
import uz.reja24.app.presentation.settings.SwitchRow
import uz.reja24.app.presentation.settings.DateDisplayChips
import uz.reja24.app.presentation.settings.BackgroundChips
import uz.reja24.app.service.alarm.ReminderSound
import uz.reja24.app.core.util.NightGrayscale
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.PaddingValues
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.reja24.app.R
import uz.reja24.app.core.util.TimeFormat
import uz.reja24.app.domain.model.AppLanguage
import uz.reja24.app.presentation.components.RejaCard
import uz.reja24.app.presentation.components.RejaTimePickerDialog
import uz.reja24.app.presentation.settings.AppPickerContent
import uz.reja24.app.presentation.settings.LanguageCard
import uz.reja24.app.presentation.settings.PermissionChecklist
import uz.reja24.app.presentation.theme.RejaTheme
import java.time.LocalTime

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = RejaTheme.colors

    LaunchedEffect(Unit) { viewModel.finished.collect { onFinished() } }
    BackHandler(enabled = state.stepIndex > 0) { viewModel.back() }

    val progress by animateFloatAsState(
        targetValue = (state.stepIndex + 1) / state.stepCount.toFloat(),
        animationSpec = tween(300),
        label = "onboarding-progress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Qadam ko'rsatkichi
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .semantics {
                    contentDescription = "${state.stepIndex + 1} / ${state.stepCount}"
                },
            color = colors.accent,
            trackColor = colors.timelineEmpty,
            strokeCap = StrokeCap.Round
        )
        Spacer(Modifier.height(20.dp))

        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                (fadeIn(tween(250)) + slideInHorizontally(tween(250)) { it / 6 }) togetherWith fadeOut(tween(200))
            },
            modifier = Modifier.weight(1f),
            label = "onboarding-step"
        ) { step ->
            when (step) {
                OnboardingStep.WELCOME -> WelcomeStep()
                OnboardingStep.PROBLEM -> ProblemStep()
                OnboardingStep.LANGUAGE -> LanguageStep(selected = state.language, onSelect = viewModel::selectLanguage)
                OnboardingStep.ROUTINE -> RoutineStep(
                    active = state.routine.isRoutineActive,
                    onActive = viewModel::setRoutineActive,
                    blockStart = state.routine.blockStartTime,
                    sleep = state.routine.sleepTime,
                    wake = state.routine.wakeTime,
                    onBlockStart = viewModel::setBlockStart,
                    onSleep = viewModel::setSleep,
                    onWake = viewModel::setWake
                )
                OnboardingStep.LOOK -> LookStep(state, viewModel)
                OnboardingStep.VOICE -> VoiceStep(state, viewModel)
                OnboardingStep.NIGHT -> NightStep(state, viewModel)
                OnboardingStep.PRAYER -> PrayerStep(state, viewModel)
                OnboardingStep.APPS -> AppsStep()
                OnboardingStep.PERMISSIONS -> PermissionsStep()
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = viewModel::next,
            enabled = !state.isFinishing,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.textPrimary, contentColor = colors.background),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = stringResource(if (state.isLast) R.string.onboarding_start else R.string.action_next),
                style = MaterialTheme.typography.titleMedium
            )
        }
        // Majburlash yo'q — har bir qadamni o'tkazib yuborish mumkin
        TextButton(
            onClick = viewModel::next,
            enabled = !state.isFinishing,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Text(stringResource(R.string.action_skip), color = colors.textMuted)
        }
    }
}

@Composable
private fun StepTitle(title: String, subtitle: String? = null) {
    Text(title, style = MaterialTheme.typography.headlineMedium, color = RejaTheme.colors.textPrimary)
    if (subtitle != null) {
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = RejaTheme.colors.textSecondary)
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun WelcomeStep() {
    val colors = RejaTheme.colors
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LogoMark()
        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            color = colors.textPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.app_slogan),
            style = MaterialTheme.typography.titleMedium,
            color = colors.accentText,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_text),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/** Logotip: 24 soatlik doira + soat millari (Canvas — rasm ishlatilmaydi). */
@Composable
fun LogoMark(modifier: Modifier = Modifier) {
    val colors = RejaTheme.colors
    val description = stringResource(R.string.cd_app_logo)
    Canvas(
        modifier = modifier
            .size(104.dp)
            .semantics { contentDescription = description }
    ) {
        val strokeWidth = 9.dp.toPx()
        val radius = size.minDimension / 2 - strokeWidth
        drawCircle(color = colors.accent, radius = radius, style = Stroke(width = strokeWidth))
        drawLine(
            color = colors.textPrimary,
            start = center,
            end = androidx.compose.ui.geometry.Offset(center.x, center.y - radius * 0.55f),
            strokeWidth = strokeWidth * 0.8f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = colors.textPrimary,
            start = center,
            end = androidx.compose.ui.geometry.Offset(center.x + radius * 0.4f, center.y + radius * 0.25f),
            strokeWidth = strokeWidth * 0.8f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun ProblemStep() {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        StepTitle(stringResource(R.string.onboarding_problem_title))
        ProblemPoint(Icons.Outlined.PhoneAndroid, stringResource(R.string.onboarding_problem_1))
        ProblemPoint(Icons.Outlined.Bedtime, stringResource(R.string.onboarding_problem_2))
        ProblemPoint(Icons.Outlined.WbSunny, stringResource(R.string.onboarding_problem_3))
        Spacer(Modifier.height(12.dp))
        RejaCard(
            modifier = Modifier.fillMaxWidth(),
            color = RejaTheme.colors.accent.copy(alpha = 0.10f),
            borderColor = RejaTheme.colors.accent.copy(alpha = 0.4f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = RejaTheme.colors.accentText)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.onboarding_problem_solution),
                    style = MaterialTheme.typography.bodyLarge,
                    color = RejaTheme.colors.textPrimary
                )
            }
        }
    }
}

@Composable
private fun ProblemPoint(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(RejaTheme.colors.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = RejaTheme.colors.textPrimary)
        }
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = RejaTheme.colors.textPrimary)
    }
}

@Composable
private fun LanguageStep(selected: AppLanguage?, onSelect: (AppLanguage) -> Unit) {
    val names = mapOf(
        AppLanguage.UZBEK to R.string.language_name_uz,
        AppLanguage.ENGLISH to R.string.language_name_en,
        AppLanguage.RUSSIAN to R.string.language_name_ru,
        AppLanguage.ARABIC to R.string.language_name_ar
    )
    Column {
        StepTitle(stringResource(R.string.onboarding_language_title))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppLanguage.entries.forEach { language ->
                LanguageCard(
                    name = stringResource(names.getValue(language)),
                    selected = language == selected,
                    onClick = { onSelect(language) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun RoutineStep(
    active: Boolean,
    onActive: (Boolean) -> Unit,
    blockStart: LocalTime,
    sleep: LocalTime,
    wake: LocalTime,
    onBlockStart: (LocalTime) -> Unit,
    onSleep: (LocalTime) -> Unit,
    onWake: (LocalTime) -> Unit
) {
    var editing by remember { mutableStateOf<Int?>(null) }
    val items = listOf(
        Triple(stringResource(R.string.routine_block_start), stringResource(R.string.routine_block_start_desc), blockStart),
        Triple(stringResource(R.string.routine_sleep), stringResource(R.string.routine_sleep_desc), sleep),
        Triple(stringResource(R.string.routine_wake), stringResource(R.string.routine_wake_desc), wake)
    )
    Column(Modifier.verticalScroll(rememberScrollState())) {
        StepTitle(stringResource(R.string.onboarding_routine_title), stringResource(R.string.onboarding_routine_subtitle))
        // Bloklash hammaga ham kerak emas: o'zi tartibli uxlaydiganlar o'chirib qo'yadi, kun tartibi baribir ishlaydi
        RejaCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
            SwitchRow(
                title = stringResource(R.string.onboarding_routine_switch),
                subtitle = stringResource(if (active) R.string.onboarding_routine_switch_on else R.string.onboarding_routine_switch_off),
                checked = active,
                onChange = onActive
            )
        }
        Spacer(Modifier.height(10.dp))
        if (active) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEachIndexed { index, (title, desc, time) ->
                RejaCard(onClick = { editing = index }, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.titleMedium, color = RejaTheme.colors.textPrimary)
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = RejaTheme.colors.textMuted)
                        }
                        Text(
                            text = TimeFormat.time(time),
                            style = MaterialTheme.typography.headlineMedium,
                            color = RejaTheme.colors.accentText
                        )
                    }
                }
            }
        }
    }
    editing?.let { index ->
        val (title, _, time) = items[index]
        RejaTimePickerDialog(
            title = title,
            initial = time,
            onDismiss = { editing = null },
            onConfirm = { picked ->
                when (index) {
                    0 -> onBlockStart(picked)
                    1 -> onSleep(picked)
                    else -> onWake(picked)
                }
                editing = null
            }
        )
    }
}

/** Ko'rinish: ilova rangi va sana (milodiy / hijriy). */
@Composable
private fun LookStep(state: OnboardingState, viewModel: OnboardingViewModel) {
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StepTitle(stringResource(R.string.onboarding_look_title), stringResource(R.string.onboarding_look_subtitle))
        StepLabel(stringResource(R.string.settings_background))
        BackgroundChips(selected = state.preferences.backgroundStyle, onSelect = viewModel::setStyle)
        Spacer(Modifier.height(8.dp))
        StepLabel(stringResource(R.string.settings_date))
        DateDisplayChips(selected = state.preferences.dateDisplay, onSelect = viewModel::setDateDisplay)
        Text(stringResource(R.string.settings_date_hint), style = MaterialTheme.typography.bodySmall, color = RejaTheme.colors.textMuted)
    }
}

/** Ovozli eslatmalar: yoqish, ovoz turi va eshitib ko'rish. */
@Composable
private fun VoiceStep(state: OnboardingState, viewModel: OnboardingViewModel) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val scope = rememberCoroutineScope()
    var previewing by remember { mutableStateOf(false) }
    val voiceOn = state.routine.isVoiceReminderOn || state.preferences.taskVoiceOn
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StepTitle(stringResource(R.string.onboarding_voice_title), stringResource(R.string.onboarding_voice_subtitle))
        RejaCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
            SwitchRow(
                title = stringResource(R.string.settings_voice),
                subtitle = stringResource(R.string.settings_voice_desc),
                checked = voiceOn,
                onChange = viewModel::setVoice
            )
            if (voiceOn) {
                VoiceGenderRow(
                    selected = state.preferences.voiceGender,
                    previewing = previewing,
                    onSelect = viewModel::setVoiceGender,
                    onPreview = {
                        previewing = true
                        scope.launch {
                            val result = ReminderSound.speak(
                                context, context.getString(R.string.task_voice_preview_text), locale, state.preferences.voiceGender
                            )
                            previewing = false
                            if (result != ReminderSound.SpeakResult.SPOKEN) {
                                Toast.makeText(context, R.string.voice_result_language_missing, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

/** Tungi himoya: qattiq rejim, "faqat kerakli" ilovalar, oq-qora ekran. */
@Composable
private fun NightStep(state: OnboardingState, viewModel: OnboardingViewModel) {
    val context = LocalContext.current
    val grayPermitted = remember { NightGrayscale.isPermitted(context) }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StepTitle(stringResource(R.string.onboarding_night_title), stringResource(R.string.onboarding_night_subtitle))
        RejaCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
            SwitchRow(
                title = stringResource(R.string.settings_strict_mode),
                subtitle = stringResource(R.string.settings_strict_mode_desc),
                checked = state.routine.isStrictModeOn,
                onChange = viewModel::setStrict
            )
            HorizontalDivider(color = RejaTheme.colors.border, modifier = Modifier.padding(horizontal = 16.dp))
            SwitchRow(
                title = stringResource(R.string.allow_only_switch),
                subtitle = stringResource(R.string.allow_only_desc),
                checked = state.preferences.nightAllowOnly,
                onChange = viewModel::setAllowOnly
            )
            HorizontalDivider(color = RejaTheme.colors.border, modifier = Modifier.padding(horizontal = 16.dp))
            SwitchRow(
                title = stringResource(R.string.grayscale_switch),
                subtitle = stringResource(if (grayPermitted) R.string.grayscale_desc else R.string.grayscale_no_permission),
                checked = state.preferences.nightGrayscale && grayPermitted,
                onChange = { on -> if (grayPermitted) viewModel.setGrayscale(on) }
            )
        }
    }
}

/** Namoz vaqtlari va zikrlar (joy telefondan avtomatik aniqlanadi). */
@Composable
private fun PrayerStep(state: OnboardingState, viewModel: OnboardingViewModel) {
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StepTitle(stringResource(R.string.onboarding_prayer_title), stringResource(R.string.onboarding_prayer_subtitle))
        RejaCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
            SwitchRow(
                title = stringResource(R.string.onboarding_prayer_switch),
                subtitle = stringResource(R.string.onboarding_prayer_switch_desc),
                checked = state.prayer.isEnabled,
                onChange = viewModel::setPrayerEnabled
            )
            if (state.prayer.isEnabled) {
                HorizontalDivider(color = RejaTheme.colors.border, modifier = Modifier.padding(horizontal = 16.dp))
                SwitchRow(
                    title = stringResource(R.string.azkar_setting),
                    subtitle = stringResource(R.string.azkar_setting_desc),
                    checked = state.prayer.azkarOn,
                    onChange = viewModel::setAzkar
                )
            }
        }
    }
}

@Composable
private fun StepLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = RejaTheme.colors.textMuted)
}

@Composable
private fun AppsStep() {
    Column {
        StepTitle(stringResource(R.string.onboarding_apps_title), stringResource(R.string.onboarding_apps_subtitle))
        AppPickerContent(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun PermissionsStep() {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StepTitle(stringResource(R.string.onboarding_permissions_title), stringResource(R.string.onboarding_permissions_subtitle))
        PermissionChecklist()
    }
}
