package uz.reja24.app.presentation.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.reja24.app.R
import uz.reja24.app.core.util.TimeFormat
import uz.reja24.app.domain.model.DailyTask
import uz.reja24.app.domain.model.RoutineSettings
import uz.reja24.app.domain.model.WeeklyEvaluator
import uz.reja24.app.presentation.components.ConfirmDialog
import uz.reja24.app.presentation.components.toComposeColor
import uz.reja24.app.presentation.theme.CategorySleep
import uz.reja24.app.presentation.theme.RejaTheme
import java.time.LocalTime
import java.util.Locale

private val HOUR_WIDTH: Dp = 46.dp
private val ROW_HEIGHT: Dp = 52.dp
private val HEADER_HEIGHT: Dp = 26.dp
private val DAY_COLUMN_WIDTH: Dp = 88.dp
private const val MINUTES_IN_DAY = 24 * 60

/**
 * Haftalik jadval (Excel'dagi kabi): 7 kun × 24 soat.
 * Kunlar ustuni joyida turadi, soatlar chapga-o'ngga suriladi. Kun bosilsa — o'sha kunning rejasi ochiladi.
 */
@Composable
internal fun WeekGrid(viewModel: ScheduleViewModel) {
    val days by viewModel.weekDays.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val today = viewModel.today
    val locale = LocalConfiguration.current.locales[0]
    val colors = RejaTheme.colors
    val density = LocalDensity.current
    val hourScroll = rememberScrollState()
    val nowTime = LocalTime.now()
    val nowMinute = nowTime.hour * 60 + nowTime.minute
    val onceEntries by viewModel.weekOnceEntries.collectAsStateWithLifecycle()
    var confirmRepeat by remember { mutableStateOf(false) }

    // Ochilganda hozirgi soatdan 2 soat oldingi joyga suriladi
    LaunchedEffect(Unit) {
        val target = with(density) { (HOUR_WIDTH * (nowTime.hour - 2).coerceAtLeast(0)).roundToPx() }
        hourScroll.scrollTo(target)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Hafta tanlash
        val weekStart = WeeklyEvaluator.weekStartOf(selectedDate)
        val weekEnd = weekStart.plusDays(6)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.selectDate(selectedDate.minusWeeks(1)) }) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = stringResource(R.string.cd_previous_week))
            }
            Text(
                text = if (weekStart.month == weekEnd.month) {
                    "${weekStart.dayOfMonth}–${TimeFormat.dayMonth(weekEnd, locale)}"
                } else {
                    "${TimeFormat.dayMonth(weekStart, locale)} – ${TimeFormat.dayMonth(weekEnd, locale)}"
                },
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.selectDate(selectedDate.plusWeeks(1)) }) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = stringResource(R.string.cd_next_week))
            }
        }

        // Bu haftada "bir marta" rejalar bor — ularni har hafta takrorlanadigan qilish taklifi
        if (onceEntries.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.accent.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.week_repeat_hint, onceEntries.size.toString()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary
                    )
                    Button(
                        onClick = { confirmRepeat = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.textPrimary, contentColor = colors.background),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Repeat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.week_repeat_button))
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row {
                // Kunlar ustuni — joyida turadi
                Column(Modifier.width(DAY_COLUMN_WIDTH)) {
                    Spacer(Modifier.height(HEADER_HEIGHT))
                    days.forEach { day ->
                        val isToday = day.date == today
                        Column(
                            modifier = Modifier
                                .height(ROW_HEIGHT)
                                .fillMaxWidth()
                                .background(if (isToday) colors.accent.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { viewModel.openDay(day.date) }
                                .padding(horizontal = 10.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = TimeFormat.weekday(day.date, locale).replaceFirstChar { it.titlecase(locale) },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isToday) colors.accentText else colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.week_tasks_done, day.completedCount.toString(), day.taskCount.toString()),
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                                color = colors.textMuted,
                                maxLines = 1
                            )
                        }
                    }
                }
                // Soatlar — chapga-o'ngga suriladi (arab tilida ham vaqt chapdan o'ngga)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(
                        Modifier
                            .weight(1f)
                            .horizontalScroll(hourScroll)
                    ) {
                        Column(Modifier.width(HOUR_WIDTH * 24)) {
                            Row(Modifier.height(HEADER_HEIGHT)) {
                                (0 until 24).forEach { hour ->
                                    Box(Modifier.width(HOUR_WIDTH).fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                                        Text(
                                            text = String.format(Locale.ROOT, "%02d:00", hour),
                                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                                            color = if (hour == nowTime.hour) colors.accentText else colors.textMuted,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                }
                            }
                            days.forEach { day ->
                                DayRow(
                                    tasks = day.tasks,
                                    settings = settings,
                                    isToday = day.date == today,
                                    nowMinute = nowMinute,
                                    onClick = { viewModel.openDay(day.date) }
                                )
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.week_grid_hint),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted
        )
    }

    if (confirmRepeat) {
        ConfirmDialog(
            title = stringResource(R.string.week_repeat_button),
            text = stringResource(R.string.week_repeat_confirm, onceEntries.size.toString()),
            confirmLabel = stringResource(R.string.week_repeat_ok),
            onDismiss = { confirmRepeat = false },
            onConfirm = {
                confirmRepeat = false
                viewModel.repeatWeekEveryWeek()
            }
        )
    }
}

@Composable
private fun DayRow(
    tasks: List<DailyTask>,
    settings: RoutineSettings,
    isToday: Boolean,
    nowMinute: Int,
    onClick: () -> Unit
) {
    val colors = RejaTheme.colors
    Box(
        Modifier
            .width(HOUR_WIDTH * 24)
            .height(ROW_HEIGHT)
            .background(if (isToday) colors.accent.copy(alpha = 0.06f) else Color.Transparent)
            .clickable(onClick = onClick)
    ) {
        // Soat chiziqlari va uyqu vaqti
        Canvas(Modifier.fillMaxSize()) {
            val pxPerMinute = HOUR_WIDTH.toPx() / 60f
            if (settings.isRoutineActive) {
                val start = settings.blockStartTime.hour * 60 + settings.blockStartTime.minute
                val end = settings.wakeTime.hour * 60 + settings.wakeTime.minute
                val ranges = if (start < end) listOf(start to end) else listOf(start to MINUTES_IN_DAY, 0 to end)
                ranges.forEach { (s, e) ->
                    drawRect(
                        color = CategorySleep.copy(alpha = 0.16f),
                        topLeft = Offset(s * pxPerMinute, 0f),
                        size = androidx.compose.ui.geometry.Size((e - s) * pxPerMinute, size.height)
                    )
                }
            }
            for (hour in 0..24) {
                drawLine(colors.border, Offset(hour * 60 * pxPerMinute, 0f), Offset(hour * 60 * pxPerMinute, size.height), 1.dp.toPx())
            }
            drawLine(colors.border, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            if (isToday) {
                val x = nowMinute * pxPerMinute
                drawLine(colors.accent, Offset(x, 0f), Offset(x, size.height), 2.dp.toPx())
            }
        }
        // Rejalar
        tasks.forEach { task ->
            val startMin = task.entry.startTime.hour * 60 + task.entry.startTime.minute
            val endMin = (startMin + task.entry.durationMinutes).coerceAtMost(MINUTES_IN_DAY)
            val x = HOUR_WIDTH * (startMin / 60f)
            val w = HOUR_WIDTH * ((endMin - startMin) / 60f) - 2.dp
            if (w > 0.dp) {
                Box(
                    Modifier
                        .offset(x = x + 1.dp, y = 4.dp)
                        .size(width = w, height = ROW_HEIGHT - 8.dp)
                        .alpha(if (task.isCompleted) 0.6f else 1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(task.entry.colorHex.toComposeColor())
                        .padding(horizontal = 5.dp, vertical = 3.dp)
                ) {
                    if (w >= 30.dp) {
                        Text(
                            text = task.entry.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp,
                                lineHeight = 12.sp
                            ),
                            color = Color.White,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
