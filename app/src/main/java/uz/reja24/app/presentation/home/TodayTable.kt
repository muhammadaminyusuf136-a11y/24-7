package uz.reja24.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uz.reja24.app.R
import uz.reja24.app.core.util.TimeFormat
import uz.reja24.app.domain.model.DailyTask
import uz.reja24.app.domain.model.RoutineSettings
import uz.reja24.app.domain.prayer.Prayer
import uz.reja24.app.domain.prayer.PrayerDay
import uz.reja24.app.presentation.components.RejaCard
import uz.reja24.app.presentation.components.SectionLabel
import uz.reja24.app.presentation.components.prayerNameRes
import uz.reja24.app.presentation.components.toComposeColor
import uz.reja24.app.presentation.theme.CategorySleep
import uz.reja24.app.presentation.theme.PrayerGreen
import uz.reja24.app.presentation.theme.RejaTheme
import uz.reja24.app.presentation.theme.SpaceGrotesk
import uz.reja24.app.presentation.theme.SuccessGreen
import java.time.LocalDateTime
import java.time.LocalTime

/** Jadval qatorining turi. */
enum class TableRowKind { TASK, PRAYER, SLEEP }

/** Jadval qatorining holati: o'tgan, hozir davom etayotgan, hali boshlanmagan. */
enum class TableRowState { PAST, NOW, LATER }

/**
 * Bugungi jadvalning bitta qatori. Daqiqalar — bugungi 00:00 dan (kechagi uyqu rejimi manfiy boshlanadi,
 * yarim tundan o'tadigan reja 1440 dan oshib tugaydi).
 */
data class TableRow(
    val key: String,
    val kind: TableRowKind,
    val startMinute: Int,
    val endMinute: Int,
    val title: String?,
    val colorHex: String?,
    val prayer: Prayer? = null,
    val isDone: Boolean = false,
    val state: TableRowState
) {
    val startTime: LocalTime get() = LocalTime.of(Math.floorMod(startMinute, DAY) / 60, Math.floorMod(startMinute, DAY) % 60)
    val endTime: LocalTime get() = LocalTime.of(Math.floorMod(endMinute, DAY) / 60, Math.floorMod(endMinute, DAY) % 60)

    private companion object {
        const val DAY = 24 * 60
    }
}

/** Bugungi jadval qatorlari (sof mantiq — testlanadi). */
object TodayTableModel {

    private const val DAY = 24 * 60

    fun rows(
        tasks: List<DailyTask>,
        settings: RoutineSettings,
        prayers: PrayerDay?,
        now: LocalDateTime
    ): List<TableRow> {
        val nowMinute = now.hour * 60 + now.minute
        fun state(start: Int, end: Int) = when {
            end <= nowMinute -> TableRowState.PAST
            start <= nowMinute -> TableRowState.NOW
            else -> TableRowState.LATER
        }
        val rows = mutableListOf<TableRow>()

        tasks.forEach { task ->
            val start = task.entry.startTime.hour * 60 + task.entry.startTime.minute
            val end = start + task.entry.durationMinutes
            rows += TableRow(
                key = "task-${task.entry.id}",
                kind = TableRowKind.TASK,
                startMinute = start,
                endMinute = end,
                title = task.entry.title,
                colorHex = task.entry.colorHex,
                isDone = task.isCompleted,
                state = state(start, end)
            )
        }

        prayers?.let { day ->
            Prayer.OBLIGATORY.forEach { prayer ->
                val at = day.time(prayer)
                if (at.toLocalDate() != now.toLocalDate()) return@forEach
                val start = at.hour * 60 + at.minute
                val end = start + TimelineModel.PRAYER_MINUTES
                rows += TableRow("prayer-$prayer", TableRowKind.PRAYER, start, end, null, null, prayer, state = state(start, end))
            }
        }

        if (settings.isRoutineActive) {
            val block = settings.blockStartTime.hour * 60 + settings.blockStartTime.minute
            val wake = settings.wakeTime.hour * 60 + settings.wakeTime.minute
            val length = Math.floorMod(wake - block, DAY)
            if (length > 0) {
                // Kechqurun boshlanib, tunda davom etayotgan kechagi rejim (masalan soat 02:00 da)
                if (block + length > DAY && nowMinute < wake) {
                    rows += TableRow("sleep-yesterday", TableRowKind.SLEEP, block - DAY, block - DAY + length, null, null,
                        state = state(block - DAY, block - DAY + length))
                }
                rows += TableRow("sleep", TableRowKind.SLEEP, block, block + length, null, null, state = state(block, block + length))
            }
        }
        return rows.sortedWith(compareBy({ it.startMinute }, { it.kind.ordinal }))
    }
}

/**
 * Bosh sahifadagi "Bugungi jadval": vaqt | reja | holat. Hozirgi qator ajratilgan,
 * o'tganlar xira (lekin o'chib ketmaydi). Bosilsa — Reja bo'limi ochiladi.
 */
@Composable
fun TodayTable(state: HomeUiState, onOpenPlan: () -> Unit, modifier: Modifier = Modifier) {
    val colors = RejaTheme.colors
    val rows = TodayTableModel.rows(state.tasks, state.settings, state.prayer.days[state.today], state.now)
    val nowMinute = state.now.hour * 60 + state.now.minute

    Column(modifier) {
        SectionLabel(stringResource(R.string.home_table_title))
        Spacer(Modifier.height(8.dp))
        RejaCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp)
        ) {
            // Sarlavha qatori
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                HeaderCell(stringResource(R.string.home_table_time), Modifier.width(TIME_WIDTH))
                HeaderCell(stringResource(R.string.home_table_plan), Modifier.weight(1f))
                HeaderCell(stringResource(R.string.home_table_state), Modifier.width(STATE_WIDTH), TextAlign.End)
            }
            HorizontalDivider(color = colors.border)
            if (rows.isEmpty()) {
                Text(
                    text = stringResource(R.string.widget_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                    modifier = Modifier.padding(14.dp)
                )
            }
            rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider(color = colors.border.copy(alpha = 0.6f), modifier = Modifier.padding(horizontal = 14.dp))
                TableRowView(row, nowMinute, onOpenPlan)
            }
        }
    }
}

private val TIME_WIDTH = 96.dp
private val PrayerGreenOnDark = androidx.compose.ui.graphics.Color(0xFF7FD1A8)
private val STATE_WIDTH = 92.dp

@Composable
private fun HeaderCell(text: String, modifier: Modifier, align: TextAlign = TextAlign.Start) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = RejaTheme.colors.textMuted,
        textAlign = align,
        modifier = modifier
    )
}

@Composable
private fun TableRowView(row: TableRow, nowMinute: Int, onClick: () -> Unit) {
    val colors = RejaTheme.colors
    val isNow = row.state == TableRowState.NOW
    val title = when (row.kind) {
        TableRowKind.TASK -> row.title.orEmpty()
        TableRowKind.PRAYER -> stringResource(prayerNameRes(row.prayer ?: Prayer.FAJR))
        TableRowKind.SLEEP -> stringResource(R.string.sleep_block_title)
    }
    val barColor = when (row.kind) {
        TableRowKind.TASK -> row.colorHex.orEmpty().toComposeColor()
        TableRowKind.PRAYER -> PrayerGreen
        TableRowKind.SLEEP -> CategorySleep
    }
    val status = when {
        row.kind == TableRowKind.TASK && row.isDone -> "✓"
        isNow && row.kind == TableRowKind.PRAYER -> stringResource(R.string.home_prayer_entered)
        isNow -> stringResource(R.string.home_time_left, TimeFormat.hoursMinutes((row.endMinute - nowMinute).toLong()))
        row.state == TableRowState.PAST -> stringResource(R.string.home_state_done)
        else -> ""
    }
    val statusColor = when {
        row.kind == TableRowKind.TASK && row.isDone -> SuccessGreen
        isNow -> colors.accentText
        else -> colors.textMuted
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .then(
                if (isNow) Modifier.background(colors.accent.copy(alpha = 0.12f), RoundedCornerShape(12.dp)) else Modifier
            )
            .clickable(onClick = onClick)
            .semantics { if (isNow) stateDescription = status }
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .alpha(if (row.state == TableRowState.PAST) 0.55f else 1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = TimeFormat.time(row.startTime) + "–" + TimeFormat.time(row.endTime),
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = SpaceGrotesk),
            fontWeight = if (isNow) FontWeight.Bold else FontWeight.Medium,
            color = if (isNow) colors.textPrimary else colors.textSecondary,
            modifier = Modifier.width(TIME_WIDTH)
        )
        Box(
            Modifier
                .width(4.dp)
                .height(20.dp)
                .background(barColor, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isNow) FontWeight.SemiBold else FontWeight.Normal,
            // Qorong'i fonda to'q yashil o'qilmaydi — ochroq yashil
            color = if (row.kind == TableRowKind.PRAYER) (if (colors.isDark) PrayerGreenOnDark else PrayerGreen) else colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = status,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isNow) FontWeight.Bold else FontWeight.Normal,
            color = statusColor,
            textAlign = TextAlign.End,
            maxLines = 2,
            modifier = Modifier.width(STATE_WIDTH)
        )
    }
}
