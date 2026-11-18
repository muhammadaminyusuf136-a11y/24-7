@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package uz.reja24.app.presentation.schedule

import androidx.compose.material.icons.outlined.AutoAwesome
import uz.reja24.app.domain.prayer.AzkarTime
import uz.reja24.app.presentation.components.dayNumbers
import uz.reja24.app.presentation.theme.MissedRed
import uz.reja24.app.presentation.theme.MissedRedOnDark
import androidx.compose.material.icons.outlined.Cancel
import uz.reja24.app.presentation.components.mainDateText
import uz.reja24.app.presentation.components.mainRangeText
import uz.reja24.app.presentation.components.secondDateText
import uz.reja24.app.presentation.components.secondRangeText
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.reja24.app.R
import uz.reja24.app.core.util.TimeFormat
import uz.reja24.app.domain.model.DailyTask
import uz.reja24.app.domain.model.RoutineSettings
import uz.reja24.app.domain.model.WeeklyEvaluator
import uz.reja24.app.domain.prayer.Prayer
import uz.reja24.app.domain.prayer.PrayerDay
import uz.reja24.app.presentation.components.prayerNameRes
import uz.reja24.app.presentation.components.ConfirmDialog
import uz.reja24.app.presentation.components.toComposeColor
import uz.reja24.app.presentation.theme.CategorySleep
import uz.reja24.app.presentation.theme.PrayerGreen
import uz.reja24.app.presentation.theme.RejaTheme
import uz.reja24.app.presentation.theme.SuccessGreen
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun ScheduleScreen(
    onAddEntry: (LocalDate) -> Unit,
    onEditEntry: (Long) -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Reja bajarilganda — motivatsion xabar (tasodifiy)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val message = when (event) {
                ScheduleEvent.TaskCompleted ->
                    context.resources.getStringArray(R.array.completion_messages).random()
                is ScheduleEvent.WeekRepeated -> context.getString(R.string.week_repeat_done, event.count.toString())
                ScheduleEvent.Error -> context.getString(R.string.error_generic)
            }
            snackbar.showSnackbar(message)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Sarlavha + "+" tugmasi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.schedule_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = RejaTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                FilledIconButton(
                    onClick = { onAddEntry(selectedDate) },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = RejaTheme.colors.textPrimary,
                        contentColor = RejaTheme.colors.background
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.cd_add_entry))
                }
            }
            ModeSelector(mode = mode, onSelect = viewModel::setMode)
            AnimatedContent(
                targetState = mode,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                label = "schedule-mode"
            ) { target ->
                when (target) {
                    ScheduleMode.DAY -> DayView(viewModel, onEditEntry)
                    ScheduleMode.WEEK -> WeekGrid(viewModel)
                    ScheduleMode.MONTH -> MonthView(viewModel)
                    ScheduleMode.YEAR -> YearGoalsView(viewModel)
                }
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ModeSelector(mode: ScheduleMode, onSelect: (ScheduleMode) -> Unit) {
    val labels = mapOf(
        ScheduleMode.DAY to R.string.mode_day,
        ScheduleMode.WEEK to R.string.mode_week,
        ScheduleMode.MONTH to R.string.mode_month,
        ScheduleMode.YEAR to R.string.mode_year
    )
    Row(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ScheduleMode.entries.forEach { item ->
            RejaChip(
                selected = item == mode,
                label = stringResource(labels.getValue(item)),
                onClick = { onSelect(item) }
            )
        }
    }
}

/** Tanlangan chip — qora fon (dizayn bo'yicha). */
@Composable
fun RejaChip(selected: Boolean, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = RejaTheme.colors
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.heightIn(min = 40.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.surface,
            labelColor = colors.textSecondary,
            selectedContainerColor = colors.textPrimary,
            selectedLabelColor = colors.background
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = colors.border,
            selectedBorderColor = colors.textPrimary
        )
    )
}

// ============================ KUNLIK ============================

private sealed interface DayRow {
    data class Task(val task: DailyTask) : DayRow
    data class Sleep(val start: LocalTime, val end: LocalTime) : DayRow
    data class PrayerTime(val prayer: Prayer, val time: LocalTime) : DayRow
    data object Now : DayRow
    data class Azkar(val time: AzkarTime, val start: LocalTime) : DayRow
}

@Composable
private fun DayView(viewModel: ScheduleViewModel, onEditEntry: (Long) -> Unit) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val tasks by viewModel.dayTasks.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val prayers by viewModel.dayPrayers.collectAsStateWithLifecycle()
    val misses by viewModel.dayMisses.collectAsStateWithLifecycle()
    val azkarOn by viewModel.azkarOn.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<DailyTask?>(null) }
    val today = viewModel.today
    val now = LocalTime.now()

    Column(Modifier.fillMaxSize()) {
        WeekDayChips(selected = selectedDate, today = today, onSelect = viewModel::selectDate)
        DayDateHeader(selectedDate)
        val rows = buildDayRows(tasks, settings, prayers, azkarOn, isToday = selectedDate == today, now = now)
        if (tasks.isEmpty()) {
            Text(
                text = stringResource(R.string.schedule_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = RejaTheme.colors.textMuted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(rows, key = { row ->
                when (row) {
                    is DayRow.Task -> "task-${row.task.entry.id}"
                    is DayRow.Sleep -> "sleep"
                    is DayRow.PrayerTime -> "prayer-${row.prayer}"
                    is DayRow.Azkar -> "azkar-${row.time.name}"
                    DayRow.Now -> "now"
                }
            }) { row ->
                when (row) {
                    is DayRow.Task -> TaskRow(
                        task = row.task,
                        isCurrent = selectedDate == today && row.task.isRunningAt(now),
                        isMissed = !row.task.isCompleted && row.task.entry.id in misses,
                        onToggle = { viewModel.toggleCompleted(row.task) },
                        onClick = { onEditEntry(row.task.entry.id) },
                        onLongClick = { toDelete = row.task }
                    )
                    is DayRow.Sleep -> SleepRow(row.start, row.end)
                    is DayRow.PrayerTime -> PrayerRow(row.prayer, row.time)
                    is DayRow.Azkar -> AzkarRow(row.time, row.start)
                    DayRow.Now -> NowLine()
                }
            }
        }
    }

    toDelete?.let { task ->
        ConfirmDialog(
            title = stringResource(R.string.delete_entry_title),
            text = stringResource(R.string.delete_entry_text, task.entry.title),
            confirmLabel = stringResource(R.string.action_delete),
            onDismiss = { toDelete = null },
            onConfirm = {
                viewModel.deleteEntry(task.entry.id)
                toDelete = null
            }
        )
    }
}

private fun DailyTask.isRunningAt(time: LocalTime): Boolean {
    val start = entry.startTime
    val end = entry.endTime
    return if (entry.crossesMidnight) !time.isBefore(start) || time.isBefore(end)
    else !time.isBefore(start) && time.isBefore(end)
}

/** Rejalar + namoz vaqtlari + uyqu bloki + "hozir" chizig'i — vaqt bo'yicha tartiblangan. */
private fun buildDayRows(
    tasks: List<DailyTask>,
    settings: RoutineSettings,
    prayers: PrayerDay?,
    azkarOn: Boolean,
    isToday: Boolean,
    now: LocalTime
): List<DayRow> {
    val timed = mutableListOf<Pair<LocalTime, DayRow>>()
    tasks.forEach { timed += it.entry.startTime to DayRow.Task(it) }
    prayers?.let { day ->
        Prayer.OBLIGATORY.forEach { prayer ->
            val time = day.time(prayer).toLocalTime()
            timed += time to DayRow.PrayerTime(prayer, time)
        }
        if (azkarOn) {
            AzkarTime.entries.forEach { azkar ->
                val start = azkar.startAt(day).toLocalTime()
                timed += start to DayRow.Azkar(azkar, start)
            }
        }
    }
    if (settings.isRoutineActive) {
        timed += settings.blockStartTime to DayRow.Sleep(settings.blockStartTime, settings.wakeTime)
    }
    if (isToday) timed += now to DayRow.Now
    // "Hozir" bir xil vaqtdagi rejadan oldin turadi
    return timed.sortedWith(compareBy({ it.first }, { if (it.second is DayRow.Now) 0 else 1 })).map { it.second }
}

/** Tanlangan kun: "Seshanba, 17 noyabr 2026-yil" va ostida hijriy sana. */
@Composable
private fun DayDateHeader(date: LocalDate) {
    val colors = RejaTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = TimeFormat.weekday(date, locale).replaceFirstChar { it.titlecase(locale) } + ", " + mainDateText(date),
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary
        )
        secondDateText(date)?.let { hijri ->
            Text(text = hijri, style = MaterialTheme.typography.bodySmall, color = colors.hijri)
        }
    }
}

@Composable
private fun WeekDayChips(selected: LocalDate, today: LocalDate, onSelect: (LocalDate) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val weekStart = WeeklyEvaluator.weekStartOf(selected)
    val colors = RejaTheme.colors
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items((0L..6L).map { weekStart.plusDays(it) }) { date ->
            val isSelected = date == selected
            val isToday = date == today
            val background = when {
                isToday -> colors.textPrimary
                isSelected -> colors.accent.copy(alpha = 0.16f)
                else -> colors.surface
            }
            val content = if (isToday) colors.background else colors.textPrimary
            Column(
                modifier = Modifier
                    .size(width = 46.dp, height = 70.dp)
                    .background(background, RoundedCornerShape(14.dp))
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) colors.accent else colors.border,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelect(date) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = TimeFormat.shortWeekday(date, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = content.copy(alpha = 0.7f),
                    maxLines = 1
                )
                val numbers = dayNumbers(date)
                Text(
                    text = numbers.main,
                    style = MaterialTheme.typography.titleLarge,
                    color = content
                )
                numbers.hijriSmall?.let { hijriDay ->
                    // Hijriy kun — kichik va boshqa rangda (bugungi to'q katakda och rang)
                    Text(
                        text = hijriDay,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday) content.copy(alpha = 0.75f) else colors.hijri,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: DailyTask,
    isCurrent: Boolean,
    isMissed: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = RejaTheme.colors
    val categoryColor = task.entry.colorHex.toComposeColor()
    Row(verticalAlignment = Alignment.Top) {
        // Chapda soat
        Text(
            text = TimeFormat.time(task.entry.startTime),
            style = MaterialTheme.typography.labelLarge,
            color = colors.textSecondary,
            modifier = Modifier
                .width(52.dp)
                .padding(top = 14.dp)
        )
        // O'ngda karta
        Row(
            modifier = Modifier
                .weight(1f)
                .alpha(if (task.isCompleted) 0.62f else 1f)
                .background(colors.surface, RoundedCornerShape(16.dp))
                .border(
                    width = if (isCurrent) 1.5.dp else 1.dp,
                    color = if (isCurrent) colors.textPrimary else colors.border,
                    shape = RoundedCornerShape(16.dp)
                )
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .drawBehind {
                    // Chap chegara — toifa rangida (4dp). RTL da o'ng tomonga o'tadi.
                    val barWidth = 4.dp.toPx()
                    val x = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl) size.width - barWidth else 0f
                    drawRoundRect(
                        color = categoryColor,
                        topLeft = androidx.compose.ui.geometry.Offset(x, 0f),
                        size = androidx.compose.ui.geometry.Size(barWidth, size.height),
                        cornerRadius = CornerRadius(barWidth, barWidth)
                    )
                }
                .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = task.entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${TimeFormat.time(task.entry.startTime)} – ${TimeFormat.time(task.entry.endTime)}" +
                        if (isMissed) " · " + stringResource(R.string.schedule_missed) else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isMissed) (if (colors.isDark) MissedRedOnDark else MissedRed) else colors.textMuted
                )
            }
            // ✓ — bajarildi, qizil ✗ — Muhosabada "bajarilmadi" deb belgilangan; bosilsa ✓ ga o'tadi
            IconButton(onClick = onToggle, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = when {
                        task.isCompleted -> Icons.Outlined.CheckCircle
                        isMissed -> Icons.Outlined.Cancel
                        else -> Icons.Outlined.RadioButtonUnchecked
                    },
                    contentDescription = stringResource(
                        if (task.isCompleted) R.string.cd_mark_not_done else R.string.cd_mark_done
                    ),
                    tint = when {
                        task.isCompleted -> SuccessGreen
                        isMissed -> if (colors.isDark) MissedRedOnDark else MissedRed
                        else -> colors.textMuted
                    }
                )
            }
        }
    }
}

@Composable
private fun SleepRow(start: LocalTime, end: LocalTime) {
    val colors = RejaTheme.colors
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = TimeFormat.time(start),
            style = MaterialTheme.typography.labelLarge,
            color = colors.textSecondary,
            modifier = Modifier
                .width(52.dp)
                .padding(top = 14.dp)
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .drawBehind {
                    // Punktir chegara
                    drawRoundRect(
                        color = CategorySleep,
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                        ),
                        cornerRadius = CornerRadius(16.dp.toPx())
                    )
                }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Bedtime, contentDescription = null, tint = CategorySleep)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.sleep_block_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                Text(
                    text = stringResource(R.string.sleep_block_subtitle, TimeFormat.time(start), TimeFormat.time(end)),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            }
        }
    }
}

/** Ertalabki / kechki zikr — namoz kabi avtomatik qator. */
@Composable
private fun AzkarRow(time: AzkarTime, start: LocalTime) {
    val colors = RejaTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = TimeFormat.time(start),
            style = MaterialTheme.typography.labelLarge,
            color = PrayerGreen,
            modifier = Modifier.width(52.dp)
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, PrayerGreen.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = PrayerGreen, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(if (time == AzkarTime.MORNING) R.string.azkar_morning else R.string.azkar_evening),
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary
            )
        }
    }
}

@Composable
private fun PrayerRow(prayer: Prayer, time: LocalTime) {
    val colors = RejaTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = TimeFormat.time(time),
            style = MaterialTheme.typography.labelLarge,
            color = PrayerGreen,
            modifier = Modifier.width(52.dp)
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .background(PrayerGreen.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                .border(1.dp, PrayerGreen.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Mosque, contentDescription = null, tint = PrayerGreen, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(prayerNameRes(prayer)),
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary
            )
        }
    }
}

@Composable
private fun NowLine() {
    val accent = RejaTheme.colors.accent
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = TimeFormat.time(LocalTime.now()),
            style = MaterialTheme.typography.labelMedium,
            color = RejaTheme.colors.accentText,
            modifier = Modifier.width(52.dp)
        )
        Box(
            Modifier
                .size(9.dp)
                .background(accent, CircleShape)
        )
        Box(
            Modifier
                .weight(1f)
                .height(2.dp)
                .background(accent)
        )
    }
}

// ============================ OYLIK ============================

/**
 * Kun katagi rangi: hammasi bajarildi — yashil, qisman — sariq, hech biri — qizil.
 * Kelajak kunlar va rejasiz kunlar — rangsiz. Bugun hali tugamagan: bajarilmaganlar qizil emas.
 */
internal enum class DayStatus { NONE, DONE, PARTIAL, MISSED }

internal fun dayStatus(taskCount: Int, completed: Int, isPast: Boolean, isToday: Boolean): DayStatus = when {
    taskCount == 0 || (!isPast && !isToday) -> DayStatus.NONE
    completed >= taskCount -> DayStatus.DONE
    completed > 0 -> DayStatus.PARTIAL
    isToday -> DayStatus.NONE
    else -> DayStatus.MISSED
}

private val DayPartialAmber = Color(0xFFB7791F)

private fun DayStatus.fill(): Color? = when (this) {
    DayStatus.DONE -> SuccessGreen
    DayStatus.PARTIAL -> DayPartialAmber
    DayStatus.MISSED -> MissedRed
    DayStatus.NONE -> null
}


@Composable
private fun MonthView(viewModel: ScheduleViewModel) {
    val month by viewModel.month.collectAsStateWithLifecycle()
    val days by viewModel.monthDays.collectAsStateWithLifecycle()
    val today = viewModel.today
    val locale = LocalConfiguration.current.locales[0]
    val colors = RejaTheme.colors

    Column(Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.showMonth(month.minusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = stringResource(R.string.cd_previous_month))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = mainRangeText(
                        month.atDay(1), month.atEndOfMonth(),
                        TimeFormat.monthYear(month.atDay(1), locale).replaceFirstChar { it.titlecase(locale) }
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
                secondRangeText(month.atDay(1), month.atEndOfMonth())?.let { hijri ->
                    Text(text = hijri, style = MaterialTheme.typography.bodySmall, color = colors.hijri)
                }
            }
            IconButton(onClick = { viewModel.showMonth(month.plusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = stringResource(R.string.cd_next_month))
            }
        }
        MonthLegend()
        Spacer(Modifier.height(8.dp))
        // Hafta kunlari sarlavhasi (dushanbadan)
        Row(Modifier.fillMaxWidth()) {
            val monday = WeeklyEvaluator.weekStartOf(today)
            (0L..6L).forEach { offset ->
                Text(
                    text = TimeFormat.shortWeekday(monday.plusDays(offset), locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        val leadingBlanks = month.atDay(1).dayOfWeek.value - 1
        val cells: List<DaySummary?> = List(leadingBlanks) { null } + days
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            contentPadding = PaddingValues(bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(cells.size) { index ->
                val day = cells[index]
                if (day == null) {
                    Spacer(Modifier.aspectRatio(1f))
                } else {
                    MonthCell(day, isToday = day.date == today, isPast = day.date.isBefore(today), onClick = { viewModel.openDay(day.date) })
                }
            }
        }
    }
}

@Composable
private fun MonthCell(day: DaySummary, isToday: Boolean, isPast: Boolean, onClick: () -> Unit) {
    val colors = RejaTheme.colors
    val fill = dayStatus(day.taskCount, day.completedCount, isPast, isToday).fill()
    // Rangli katakda raqamlar oq (rang to'q, o'qilishi aniq)
    val textColor = if (fill != null) Color.White else colors.textPrimary
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .background(fill ?: colors.surface, RoundedCornerShape(12.dp))
            .border(
                width = if (isToday) 2.dp else 1.dp,
                color = if (isToday) colors.textPrimary else (fill ?: colors.border),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val numbers = dayNumbers(day.date)
        Text(
            text = numbers.main,
            style = MaterialTheme.typography.labelLarge,
            color = textColor
        )
        numbers.hijriSmall?.let { hijriDay ->
            Text(
                text = hijriDay,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 11.sp),
                color = if (fill != null) Color.White.copy(alpha = 0.85f) else colors.hijri,
                maxLines = 1
            )
        }
    }
}

/** Oy kalendari ranglarining izohi. */
@Composable
private fun MonthLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(
            SuccessGreen to R.string.month_legend_done,
            DayPartialAmber to R.string.month_legend_partial,
            MissedRed to R.string.month_legend_missed
        ).forEach { (color, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(color, RoundedCornerShape(3.dp))
                )
                Spacer(Modifier.width(5.dp))
                Text(stringResource(label), style = MaterialTheme.typography.labelSmall, color = RejaTheme.colors.textSecondary)
            }
        }
    }
}

// Yillik maqsadlar alohida faylda (YearGoalsView.kt)

@Composable
internal fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = RejaTheme.colors.textMuted)
    }
}
