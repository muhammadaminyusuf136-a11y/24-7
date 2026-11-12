@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package uz.reja24.app.presentation.schedule

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
import java.util.Locale

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
                is ScheduleEvent.TaskCompleted -> {
                    val template = context.resources.getStringArray(R.array.completion_messages).random()
                    String.format(Locale.ROOT, template, event.pointsGained.toString())
                }
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
}

@Composable
private fun DayView(viewModel: ScheduleViewModel, onEditEntry: (Long) -> Unit) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val tasks by viewModel.dayTasks.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val prayers by viewModel.dayPrayers.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<DailyTask?>(null) }
    val today = viewModel.today
    val now = LocalTime.now()

    Column(Modifier.fillMaxSize()) {
        WeekDayChips(selected = selectedDate, today = today, onSelect = viewModel::selectDate)
        val rows = buildDayRows(tasks, settings, prayers, isToday = selectedDate == today, now = now)
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
                    DayRow.Now -> "now"
                }
            }) { row ->
                when (row) {
                    is DayRow.Task -> TaskRow(
                        task = row.task,
                        isCurrent = selectedDate == today && row.task.isRunningAt(now),
                        onToggle = { viewModel.toggleCompleted(row.task) },
                        onClick = { onEditEntry(row.task.entry.id) },
                        onLongClick = { toDelete = row.task }
                    )
                    is DayRow.Sleep -> SleepRow(row.start, row.end)
                    is DayRow.PrayerTime -> PrayerRow(row.prayer, row.time)
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
    }
    if (settings.isRoutineActive) {
        timed += settings.blockStartTime to DayRow.Sleep(settings.blockStartTime, settings.wakeTime)
    }
    if (isToday) timed += now to DayRow.Now
    // "Hozir" bir xil vaqtdagi rejadan oldin turadi
    return timed.sortedWith(compareBy({ it.first }, { if (it.second is DayRow.Now) 0 else 1 })).map { it.second }
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
                    .size(width = 46.dp, height = 62.dp)
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
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = content
                )
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: DailyTask,
    isCurrent: Boolean,
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
                    text = "${TimeFormat.time(task.entry.startTime)} – ${TimeFormat.time(task.entry.endTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            }
            IconButton(onClick = onToggle, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = stringResource(
                        if (task.isCompleted) R.string.cd_mark_not_done else R.string.cd_mark_done
                    ),
                    tint = if (task.isCompleted) SuccessGreen else colors.textMuted
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

/** Baho darajasiga qarab kun rangi. */
@Composable
fun scoreLevelColor(stars: Int?): Color {
    val colors = RejaTheme.colors
    return when (stars) {
        null -> Color.Transparent
        5 -> SuccessGreen.copy(alpha = 0.55f)
        4 -> SuccessGreen.copy(alpha = 0.35f)
        3 -> colors.accent.copy(alpha = 0.25f)
        2 -> colors.accent.copy(alpha = 0.40f)
        else -> colors.accent.copy(alpha = 0.55f)
    }
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
            Text(
                text = TimeFormat.monthYear(month.atDay(1), locale).replaceFirstChar { it.titlecase(locale) },
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.showMonth(month.plusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = stringResource(R.string.cd_next_month))
            }
        }
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
                    MonthCell(day, isToday = day.date == today, onClick = { viewModel.openDay(day.date) })
                }
            }
        }
    }
}

@Composable
private fun MonthCell(day: DaySummary, isToday: Boolean, onClick: () -> Unit) {
    val colors = RejaTheme.colors
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .background(scoreLevelColor(day.score?.starsEarned), RoundedCornerShape(12.dp))
            .border(
                width = if (isToday) 1.5.dp else 1.dp,
                color = if (isToday) colors.textPrimary else colors.border,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = colors.textPrimary
        )
        // Nuqtalar: rejalar soni (ko'pi bilan 3 ta)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(6.dp)) {
            repeat(day.taskCount.coerceAtMost(3)) {
                Box(
                    Modifier
                        .size(4.dp)
                        .background(colors.accent, CircleShape)
                )
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
