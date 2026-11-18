@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package uz.reja24.app.presentation.schedule

import uz.reja24.app.presentation.components.mainDateText
import uz.reja24.app.presentation.components.secondDateText
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.reja24.app.R
import uz.reja24.app.core.util.DurationText
import uz.reja24.app.core.util.TimeFormat
import uz.reja24.app.domain.model.Category
import uz.reja24.app.domain.model.RepeatType
import uz.reja24.app.presentation.components.AlarmSoundRow
import uz.reja24.app.presentation.components.ConfirmDialog
import uz.reja24.app.presentation.components.RejaCard
import uz.reja24.app.presentation.components.RejaTimePickerDialog
import uz.reja24.app.presentation.components.SectionLabel
import uz.reja24.app.presentation.components.SwitchSettingRow
import uz.reja24.app.presentation.components.categoryNameRes
import uz.reja24.app.presentation.components.toComposeColor
import uz.reja24.app.presentation.settings.SubScreenTopBar
import uz.reja24.app.presentation.theme.RejaTheme
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun EntryEditorScreen(
    onBack: () -> Unit,
    viewModel: EntryEditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val colors = RejaTheme.colors

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                EditorEvent.Saved, EditorEvent.Deleted -> onBack()
                EditorEvent.Error -> snackbar.showSnackbar(context.getString(R.string.error_generic))
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            SubScreenTopBar(
                title = stringResource(if (state.isNew) R.string.editor_title_new else R.string.editor_title_edit),
                onBack = onBack,
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                }
            )
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.accent)
                }
            } else Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    focusedLabelColor = colors.accentText,
                    cursorColor = colors.accent
                )
                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::onTitleChange,
                    label = { Text(stringResource(R.string.editor_field_title)) },
                    isError = state.titleError,
                    supportingText = if (state.titleError) {
                        { Text(stringResource(R.string.error_title_required)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = { Text(stringResource(R.string.editor_field_description)) },
                    minLines = 2,
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                // Vaqt
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimeField(
                        label = stringResource(R.string.editor_start),
                        value = TimeFormat.time(state.startTime),
                        onClick = { pickStart = true },
                        modifier = Modifier.weight(1f)
                    )
                    TimeField(
                        label = stringResource(R.string.editor_end),
                        value = TimeFormat.time(state.endTime),
                        onClick = { pickEnd = true },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (state.timeError) {
                    Text(
                        text = stringResource(R.string.error_time_same),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Toifa
                SectionLabel(stringResource(R.string.editor_category))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Category.entries.forEach { category ->
                        CategoryChip(
                            category = category,
                            selected = state.category == category,
                            onClick = { viewModel.onCategoryChange(category) }
                        )
                    }
                }

                // Takrorlanish
                SectionLabel(stringResource(R.string.editor_repeat))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RepeatType.entries.forEach { type ->
                        RejaChip(
                            selected = state.repeatType == type,
                            label = stringResource(repeatNameRes(type)),
                            onClick = { viewModel.onRepeatTypeChange(type) }
                        )
                    }
                }
                if (state.repeatType == RepeatType.WEEKLY) {
                    WeekdayPicker(selected = state.daysOfWeek, onToggle = viewModel::onToggleDay)
                    if (state.daysError) {
                        Text(
                            text = stringResource(R.string.error_days_required),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                TimeField(
                    label = stringResource(
                        when (state.repeatType) {
                            RepeatType.ONCE, RepeatType.MONTHLY, RepeatType.YEARLY -> R.string.editor_date
                            else -> R.string.editor_start_date
                        }
                    ),
                    value = mainDateText(state.date) + (secondDateText(state.date)?.let { " · $it" } ?: ""),
                    onClick = { pickDate = true },
                    modifier = Modifier.fillMaxWidth()
                )

                // Eslatma
                SectionLabel(stringResource(R.string.editor_reminder))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    REMINDER_OPTIONS.forEach { minutes ->
                        RejaChip(
                            selected = state.reminderMinutes == minutes,
                            label = if (minutes == 0) {
                                stringResource(R.string.editor_reminder_off)
                            } else {
                                DurationText.format(context.resources, minutes)
                            },
                            onClick = { viewModel.onReminderChange(minutes) }
                        )
                    }
                }

                // Budilnik
                SectionLabel(stringResource(R.string.editor_alarm))
                RejaCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                    SwitchSettingRow(
                        title = stringResource(R.string.editor_alarm_switch),
                        subtitle = stringResource(R.string.editor_alarm_desc),
                        checked = state.alarmEnabled,
                        onChange = viewModel::onAlarmEnabledChange
                    )
                    if (state.alarmEnabled) {
                        HorizontalDivider(color = colors.border, modifier = Modifier.padding(horizontal = 16.dp))
                        AlarmSoundRow(
                            title = stringResource(R.string.editor_alarm_sound),
                            soundUri = state.alarmSoundUri,
                            onPicked = viewModel::onAlarmSoundChange
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = viewModel::save,
                    enabled = !state.isSaving,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.textPrimary,
                        contentColor = colors.background
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(stringResource(R.string.action_save), style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (pickStart) {
        RejaTimePickerDialog(
            title = stringResource(R.string.editor_start),
            initial = state.startTime,
            onDismiss = { pickStart = false },
            onConfirm = {
                viewModel.onStartTimeChange(it)
                pickStart = false
            }
        )
    }
    if (pickEnd) {
        RejaTimePickerDialog(
            title = stringResource(R.string.editor_end),
            initial = state.endTime,
            onDismiss = { pickEnd = false },
            onConfirm = {
                viewModel.onEndTimeChange(it)
                pickEnd = false
            }
        )
    }
    if (pickDate) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        viewModel.onDateChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    pickDate = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_entry_title),
            text = stringResource(R.string.delete_entry_text, state.title),
            confirmLabel = stringResource(R.string.action_delete),
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            }
        )
    }
}

private val REMINDER_OPTIONS = listOf(0, 5, 10, 15, 30, 60)

fun repeatNameRes(type: RepeatType): Int = when (type) {
    RepeatType.ONCE -> R.string.repeat_once
    RepeatType.DAILY -> R.string.repeat_daily
    RepeatType.WEEKDAYS -> R.string.repeat_weekdays
    RepeatType.WEEKLY -> R.string.repeat_weekly
    RepeatType.MONTHLY -> R.string.repeat_monthly
    RepeatType.YEARLY -> R.string.repeat_yearly
}

@Composable
private fun TimeField(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = RejaTheme.colors
    RejaCard(onClick = onClick, modifier = modifier, cornerRadius = 14.dp) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
    }
}

@Composable
private fun CategoryChip(category: Category, selected: Boolean, onClick: () -> Unit) {
    val colors = RejaTheme.colors
    val color = category.defaultColorHex.toComposeColor()
    RejaCard(
        onClick = onClick,
        cornerRadius = 14.dp,
        color = if (selected) color.copy(alpha = 0.16f) else colors.surface,
        borderColor = if (selected) color else colors.border,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(categoryNameRes(category)),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary
            )
        }
    }
}

@Composable
private fun WeekdayPicker(selected: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    // Istalgan dushanbadan boshlab 7 kun — faqat nomlarini olish uchun
    val monday = LocalDate.of(2024, 1, 1)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DayOfWeek.values().forEachIndexed { index, day ->
            RejaChip(
                selected = day in selected,
                label = TimeFormat.shortWeekday(monday.plusDays(index.toLong()), locale),
                onClick = { onToggle(day) }
            )
        }
    }
}
