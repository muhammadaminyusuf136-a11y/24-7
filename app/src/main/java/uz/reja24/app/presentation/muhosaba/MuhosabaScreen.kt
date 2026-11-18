@file:OptIn(ExperimentalLayoutApi::class)

package uz.reja24.app.presentation.muhosaba

import uz.reja24.app.presentation.theme.PrayerGreen
import uz.reja24.app.presentation.theme.MissedRed
import uz.reja24.app.presentation.theme.MissedRedOnDark
import uz.reja24.app.presentation.components.prayerNameRes
import uz.reja24.app.domain.repository.DayNote
import uz.reja24.app.domain.repository.PrayerStatus
import uz.reja24.app.domain.prayer.Prayer
import androidx.compose.material.icons.outlined.Schedule
import uz.reja24.app.presentation.components.mainDateText
import uz.reja24.app.presentation.components.secondDateText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.reja24.app.R
import uz.reja24.app.core.util.TimeFormat
import uz.reja24.app.domain.model.DailyScore
import uz.reja24.app.domain.repository.MuhosabaItem
import uz.reja24.app.presentation.components.RejaCard
import uz.reja24.app.presentation.components.SectionLabel
import uz.reja24.app.presentation.components.toComposeColor
import uz.reja24.app.presentation.schedule.RejaChip
import uz.reja24.app.presentation.theme.RejaTheme
import uz.reja24.app.presentation.theme.SuccessGreen
import java.time.LocalDate

/** «Bajarilmadi» rangi — ikkala mavzuda ham oq belgi aniq ko'rinadi. */

/** Daftarda aralash kun (✓ ham, ✗ ham bor) — oq raqam ustida o'qiladigan to'q sariq. */
private val PartialAmber = Color(0xFFB7791F)

/** Kunning hikmati: har kuni navbat bilan (sana bo'yicha), matn va manba alohida massivlarda. */
@Composable
private fun quoteOfDay(date: LocalDate): Pair<String, String> {
    val quotes = stringArrayResource(R.array.muhosaba_quotes)
    val sources = stringArrayResource(R.array.muhosaba_quote_sources)
    val index = Math.floorMod(date.toEpochDay(), quotes.size.toLong()).toInt()
    return quotes[index] to sources.getOrElse(index) { "" }
}

/** "Muhosaba" bo'limi: bugungi hisob (✓ / ✗), ilova bilgan narsalar, hafta va oy xulosasi. */
@Composable
fun MuhosabaScreen(viewModel: MuhosabaViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val current = state
    if (current == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = RejaTheme.colors.accent)
        }
        return
    }
    val resources = LocalContext.current.resources
    MuhosabaContent(
        state = current,
        onPreviousDay = viewModel::previousDay,
        onNextDay = viewModel::nextDay,
        onMark = viewModel::setMark,
        onPlanMark = viewModel::setPlanMark,
        onRemove = viewModel::removeItem,
        onAdd = viewModel::addItem,
        onAddSamples = { viewModel.addItems(resources.getStringArray(R.array.muhosaba_sample_items).toList()) },
        onPrayerMark = viewModel::setPrayerMark,
        onSaveNote = viewModel::saveNote
    )
}

@Composable
internal fun MuhosabaContent(
    state: MuhosabaUiState,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onMark: (Long, Boolean) -> Unit,
    onPlanMark: (Long, Boolean) -> Unit,
    onRemove: (Long) -> Unit,
    onAdd: (String) -> Unit,
    onAddSamples: () -> Unit,
    onPrayerMark: (Prayer, PrayerStatus) -> Unit = { _, _ -> },
    onSaveNote: (String, String) -> Unit = { _, _ -> }
) {
    val current = state
    val colors = RejaTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    var editing by rememberSaveable { mutableStateOf(false) }
    var monthly by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.muhosaba_title),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary
        )

        // Hikmat
        val (quote, source) = quoteOfDay(current.date)
        RejaCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
            Text(text = quote, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            Spacer(Modifier.height(6.dp))
            Text(text = source, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
        }

        // Kun tanlash
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPreviousDay) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = stringResource(R.string.muhosaba_previous_day))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (current.isToday) stringResource(R.string.timeline_today) else TimeFormat.weekday(current.date, locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textMuted
                )
                Text(
                    text = mainDateText(current.date),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                secondDateText(current.date)?.let { hijri ->
                    Text(text = hijri, style = MaterialTheme.typography.bodySmall, color = colors.hijri)
                }
            }
            IconButton(onClick = onNextDay, enabled = !current.isToday) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.muhosaba_next_day),
                    tint = if (current.isToday) colors.textMuted.copy(alpha = 0.4f) else colors.textPrimary
                )
            }
        }

        // Kun tartibi: Reja bo'limidagi rejalar, har biriga ✓ yoki ✗
        SectionLabel(stringResource(R.string.muhosaba_plans))
        if (current.plans.isEmpty()) {
            RejaCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.muhosaba_plans_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            }
        } else {
            RejaCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 4.dp)) {
                current.plans.forEachIndexed { index, plan ->
                    if (index > 0) HorizontalDivider(color = colors.border, modifier = Modifier.padding(horizontal = 16.dp))
                    PlanRow(plan = plan, onMark = { done -> onPlanMark(plan.task.entry.id, done) })
                }
            }
            Text(
                text = stringResource(
                    R.string.muhosaba_marked_count,
                    current.plans.count { it.mark != null }.toString(),
                    current.plans.size.toString()
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )
        }

        // 5 vaqt namoz: vaqtida / kechikib / qazo
        SectionLabel(stringResource(R.string.muhosaba_prayers))
        RejaCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 4.dp)) {
            Prayer.OBLIGATORY.forEachIndexed { index, prayer ->
                if (index > 0) HorizontalDivider(color = colors.border, modifier = Modifier.padding(horizontal = 16.dp))
                PrayerMarkRow(prayer, current.prayers[prayer], onMark = { status -> onPrayerMark(prayer, status) })
            }
        }
        Text(
            text = stringResource(R.string.muhosaba_prayers_hint),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted
        )

        // O'z ro'yxati
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(stringResource(R.string.muhosaba_my_list), modifier = Modifier.weight(1f))
            if (current.items.isNotEmpty()) {
                TextButton(onClick = { editing = !editing }) {
                    Text(stringResource(if (editing) R.string.muhosaba_edit_done else R.string.muhosaba_edit))
                }
            }
        }
        if (current.items.isEmpty()) {
            EmptyList(onAddSamples = onAddSamples)
        } else {
            RejaCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 4.dp)) {
                current.items.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider(color = colors.border, modifier = Modifier.padding(horizontal = 16.dp))
                    ItemRow(
                        item = item,
                        mark = current.marks[item.id],
                        editing = editing,
                        onMark = { done -> onMark(item.id, done) },
                        onRemove = { onRemove(item.id) }
                    )
                }
            }
            val marked = current.marks.count { (id, _) -> current.items.any { it.id == id } }
            Text(
                text = stringResource(R.string.muhosaba_marked_count, marked.toString(), current.items.size.toString()),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )
        }
        if (editing || current.items.isEmpty()) AddItemField(onAdd = onAdd)

        // Ilova o'zi bilgan narsalar
        SectionLabel(stringResource(R.string.muhosaba_facts))
        FactsCard(current.facts)

        // Kechki yozuv: bugun nima yaxshi bo'ldi, nimani tuzataman
        SectionLabel(stringResource(R.string.muhosaba_note))
        NoteCard(date = current.date, note = current.note, onSave = onSaveNote)

        // Hafta / oy xulosasi va daftar
        val planSummary = if (monthly) current.planMonth else current.planWeek
        val prayerTally = if (monthly) current.prayerMonth else current.prayerWeek
        val hasPrayerMarks = current.prayerMonth.let { it.onTime + it.late + it.missed > 0 }
        if (current.items.isNotEmpty() || planSummary.isNotEmpty() || hasPrayerMarks) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.muhosaba_summary), modifier = Modifier.weight(1f))
                RejaChip(selected = !monthly, label = stringResource(R.string.muhosaba_week), onClick = { monthly = false })
                RejaChip(selected = monthly, label = stringResource(R.string.muhosaba_month), onClick = { monthly = true })
            }
            if (planSummary.isNotEmpty()) {
                RejaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionLabel(stringResource(R.string.muhosaba_plans))
                        planSummary.forEach {
                            SummaryBar(
                                title = it.title,
                                barColor = it.colorHex.toComposeColor(),
                                done = it.done,
                                missed = it.missed,
                                total = it.scheduled
                            )
                        }
                    }
                }
            }
            if (hasPrayerMarks) {
                RejaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionLabel(stringResource(R.string.muhosaba_prayers))
                        PrayerTallyBar(prayerTally)
                    }
                }
            }
            if (current.items.isNotEmpty()) {
                RejaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionLabel(stringResource(R.string.muhosaba_my_list))
                        (if (monthly) current.month else current.week).forEach {
                            SummaryBar(title = it.item.title, barColor = null, done = it.done, missed = it.missed, total = it.days)
                        }
                    }
                }
            }
            Text(
                text = stringResource(R.string.muhosaba_summary_hint),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )

            // Daftar: kunma-kun ✓ va ✗ (kundalik daftar kabi to'lib boradi)
            SectionLabel(stringResource(R.string.muhosaba_ledger))
            RejaCard(modifier = Modifier.fillMaxWidth()) {
                if (monthly) LedgerMonth(current.ledger) else LedgerWeek(current.ledger.takeLast(MuhosabaViewModel.WEEK_DAYS))
            }
            Text(
                text = stringResource(R.string.muhosaba_ledger_hint),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun EmptyList(onAddSamples: () -> Unit) {
    val colors = RejaTheme.colors
    RejaCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.muhosaba_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary
        )
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onAddSamples,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.textPrimary, contentColor = colors.background)
        ) { Text(stringResource(R.string.muhosaba_add_samples)) }
    }
}

@Composable
private fun ItemRow(
    item: MuhosabaItem,
    mark: Boolean?,
    editing: Boolean,
    onMark: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    val colors = RejaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        )
        if (editing) {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(R.string.action_delete) + ": " + item.title,
                    tint = colors.textMuted
                )
            }
        } else {
            MarkButton(
                selected = mark == true,
                color = SuccessGreen,
                description = stringResource(R.string.muhosaba_mark_done, item.title),
                onClick = { onMark(true) }
            ) { tint -> Icon(Icons.Outlined.Check, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(8.dp))
            MarkButton(
                selected = mark == false,
                color = MissedRed,
                description = stringResource(R.string.muhosaba_mark_missed, item.title),
                onClick = { onMark(false) }
            ) { tint -> Icon(Icons.Outlined.Close, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(4.dp))
        }
    }
}

@Composable
private fun MarkButton(
    selected: Boolean,
    color: Color,
    description: String,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit
) {
    val colors = RejaTheme.colors
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .background(if (selected) color else Color.Transparent, CircleShape)
            .border(1.5.dp, if (selected) color else colors.border, CircleShape)
            .semantics { contentDescription = description }
    ) { icon(if (selected) Color.White else colors.textMuted) }
}

@Composable
private fun AddItemField(onAdd: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(MuhosabaViewModel.MAX_TITLE) },
            singleLine = true,
            placeholder = { Text(stringResource(R.string.muhosaba_add_placeholder)) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = {
                onAdd(text)
                text = ""
            },
            enabled = text.isNotBlank(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RejaTheme.colors.textPrimary,
                contentColor = RejaTheme.colors.background
            ),
            modifier = Modifier.height(52.dp)
        ) { Text(stringResource(R.string.action_add)) }
    }
}

@Composable
private fun FactsCard(facts: DailyScore) {
    RejaCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FactRow(
                label = stringResource(R.string.muhosaba_fact_night_phone),
                value = stringResource(if (facts.usedPhoneAfterSleep) R.string.muhosaba_yes else R.string.muhosaba_no),
                good = !facts.usedPhoneAfterSleep
            )
            FactRow(
                label = stringResource(R.string.muhosaba_fact_woke),
                value = stringResource(if (facts.wokeOnTime) R.string.muhosaba_yes else R.string.muhosaba_no),
                good = facts.wokeOnTime
            )
            FactRow(
                label = stringResource(R.string.muhosaba_fact_bypass),
                // Ball o'rniga belgi: har bir favqulodda kirish — bitta ✗
                value = if (facts.bypassCount == 0) stringResource(R.string.muhosaba_no) else "✗".repeat(facts.bypassCount),
                good = facts.bypassCount == 0
            )
        }
    }
}

@Composable
private fun FactRow(label: String, value: String, good: Boolean) {
    val colors = RejaTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .background(if (good) SuccessGreen else colors.accent, CircleShape)
        )
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
    }
}

/** Kun tartibidagi bitta reja: rang chizig'i, vaqt, nomi va ✓ / ✗ tugmalari. */
@Composable
private fun PlanRow(plan: PlanMark, onMark: (Boolean) -> Unit) {
    val colors = RejaTheme.colors
    val entry = plan.task.entry
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(30.dp)
                .background(entry.colorHex.toComposeColor(), RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(10.dp))
        Column(
            Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(text = entry.title, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
            Text(
                text = TimeFormat.time(entry.startTime) + " – " + TimeFormat.time(entry.endTime),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )
        }
        MarkButton(
            selected = plan.mark == true,
            color = SuccessGreen,
            description = stringResource(R.string.muhosaba_mark_done, entry.title),
            onClick = { onMark(true) }
        ) { tint -> Icon(Icons.Outlined.Check, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(8.dp))
        MarkButton(
            selected = plan.mark == false,
            color = MissedRed,
            description = stringResource(R.string.muhosaba_mark_missed, entry.title),
            onClick = { onMark(false) }
        ) { tint -> Icon(Icons.Outlined.Close, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(4.dp))
    }
}

/** Namoz qatori: nomi va uchta tugma — vaqtida (✓), kechikib (soat), qazo (✗). */
@Composable
private fun PrayerMarkRow(prayer: Prayer, status: PrayerStatus?, onMark: (PrayerStatus) -> Unit) {
    val colors = RejaTheme.colors
    val name = stringResource(prayerNameRes(prayer))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(30.dp)
                .background(PrayerGreen, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(10.dp))
        Column(
            Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(text = name, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
            val label = when (status) {
                PrayerStatus.ON_TIME -> stringResource(R.string.prayer_status_on_time)
                PrayerStatus.LATE -> stringResource(R.string.prayer_status_late)
                PrayerStatus.MISSED -> stringResource(R.string.prayer_status_missed)
                null -> null
            }
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (status) {
                        PrayerStatus.ON_TIME -> SuccessGreen
                        PrayerStatus.LATE -> PartialAmber
                        else -> if (colors.isDark) MissedRedOnDark else MissedRed
                    }
                )
            }
        }
        MarkButton(
            selected = status == PrayerStatus.ON_TIME,
            color = SuccessGreen,
            description = stringResource(R.string.prayer_mark_on_time, name),
            onClick = { onMark(PrayerStatus.ON_TIME) }
        ) { tint -> Icon(Icons.Outlined.Check, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(6.dp))
        MarkButton(
            selected = status == PrayerStatus.LATE,
            color = PartialAmber,
            description = stringResource(R.string.prayer_mark_late, name),
            onClick = { onMark(PrayerStatus.LATE) }
        ) { tint -> Icon(Icons.Outlined.Schedule, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(6.dp))
        MarkButton(
            selected = status == PrayerStatus.MISSED,
            color = MissedRed,
            description = stringResource(R.string.prayer_mark_missed, name),
            onClick = { onMark(PrayerStatus.MISSED) }
        ) { tint -> Icon(Icons.Outlined.Close, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(4.dp))
    }
}

/** Namozlar xulosasi: "vaqtida 30 · kechikib 3 · qazo 1" va uch rangli chiziq. */
@Composable
private fun PrayerTallyBar(tally: PrayerTally) {
    val colors = RejaTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(
                R.string.muhosaba_prayer_tally,
                tally.onTime.toString(), tally.late.toString(), tally.missed.toString(), tally.total.toString()
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary
        )
        Row(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(colors.timelineEmpty, RoundedCornerShape(4.dp))
        ) {
            listOf(tally.onTime to SuccessGreen, tally.late to PartialAmber, tally.missed to MissedRed).forEach { (count, color) ->
                if (count > 0) {
                    Box(
                        Modifier
                            .weight(count.toFloat())
                            .height(8.dp)
                            .background(color, RoundedCornerShape(4.dp))
                    )
                }
            }
            val rest = tally.total - tally.onTime - tally.late - tally.missed
            if (rest > 0) Spacer(Modifier.weight(rest.toFloat()))
        }
    }
}

/** Kechki yozuv: ikki savol va "Saqlash". Kun almashsa — o'sha kunning yozuvi ko'rsatiladi. */
@Composable
private fun NoteCard(date: LocalDate, note: DayNote?, onSave: (String, String) -> Unit) {
    val colors = RejaTheme.colors
    var good by rememberSaveable(date, note) { mutableStateOf(note?.good.orEmpty()) }
    var improve by rememberSaveable(date, note) { mutableStateOf(note?.improve.orEmpty()) }
    val changed = good.trim() != note?.good.orEmpty() || improve.trim() != note?.improve.orEmpty()
    RejaCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = good,
                onValueChange = { good = it.take(MuhosabaViewModel.MAX_NOTE) },
                label = { Text(stringResource(R.string.muhosaba_note_good)) },
                shape = RoundedCornerShape(14.dp),
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = improve,
                onValueChange = { improve = it.take(MuhosabaViewModel.MAX_NOTE) },
                label = { Text(stringResource(R.string.muhosaba_note_improve)) },
                shape = RoundedCornerShape(14.dp),
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(if (changed) R.string.muhosaba_note_unsaved else R.string.muhosaba_note_saved)
                        .takeIf { changed || note != null }.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { onSave(good, improve) },
                    enabled = changed,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.textPrimary,
                        contentColor = colors.background
                    )
                ) { Text(stringResource(R.string.action_save)) }
            }
        }
    }
}

/** Xulosa qatori: nomi, "✓ soni / jami kun" va yashil-qizil chiziq. barColor — rejaning o'z rangi (nuqta). */
@Composable
private fun SummaryBar(title: String, barColor: Color?, done: Int, missed: Int, total: Int) {
    val colors = RejaTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (barColor != null) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(barColor, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
            Text(
                text = stringResource(R.string.muhosaba_days_of, done.toString(), total.toString()),
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary
            )
        }
        // Yashil — ✓ kunlar, qizil — ✗ kunlar, bo'sh — belgilanmagan
        Row(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(colors.timelineEmpty, RoundedCornerShape(4.dp))
        ) {
            if (done > 0) {
                Box(
                    Modifier
                        .weight(done.toFloat())
                        .height(8.dp)
                        .background(SuccessGreen, RoundedCornerShape(4.dp))
                )
            }
            if (missed > 0) {
                Box(
                    Modifier
                        .weight(missed.toFloat())
                        .height(8.dp)
                        .background(MissedRed, RoundedCornerShape(4.dp))
                )
            }
            val rest = total - done - missed
            if (rest > 0) Spacer(Modifier.weight(rest.toFloat()))
        }
    }
}

/** Kunning rangi daftarda: hammasi ✓ — yashil, hammasi ✗ — qizil, aralash — sariq, belgisiz — bo'sh. */
@Composable
private fun ledgerColor(day: DayLedger): Color = when {
    day.done == 0 && day.missed == 0 -> RejaTheme.colors.timelineEmpty
    day.missed == 0 -> SuccessGreen
    day.done == 0 -> MissedRed
    else -> PartialAmber
}

/** Haftalik daftar: 7 qator — kun, ✓ soni, ✗ soni. */
@Composable
private fun LedgerWeek(days: List<DayLedger>) {
    val colors = RejaTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        days.forEach { day ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .background(ledgerColor(day), CircleShape)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = TimeFormat.weekday(day.date, locale).replaceFirstChar { it.titlecase(locale) } +
                        ", " + day.date.dayOfMonth,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text("✓ " + day.done, style = MaterialTheme.typography.titleSmall, color = SuccessGreen)
                Spacer(Modifier.width(14.dp))
                Text(
                    "✗ " + day.missed,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (day.missed > 0) MissedRed else colors.textMuted
                )
            }
        }
    }
}

/** Oylik daftar: 30 ta katak, har biri bir kun (eskisi chapda-tepada). */
@Composable
private fun LedgerMonth(days: List<DayLedger>) {
    val colors = RejaTheme.colors
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        maxItemsInEachRow = 7
    ) {
        days.forEach { day ->
            val fill = ledgerColor(day)
            val marked = day.done > 0 || day.missed > 0
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(fill, RoundedCornerShape(9.dp))
                    .semantics { contentDescription = day.date.toString() + ": ✓ " + day.done + ", ✗ " + day.missed },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (marked) Color.White else colors.textMuted
                )
            }
        }
    }
}

/**
 * Kechki muhosaba oynasi: bloklashdan oldingi oxirgi eslatmada ochiladi.
 * Hikmat, qisqa eslatma va "Hisobni boshlash".
 */
@Composable
fun MuhosabaEveningScreen(onStart: () -> Unit, onLater: () -> Unit) {
    val colors = RejaTheme.colors
    val (quote, source) = quoteOfDay(LocalDate.now())
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            text = quote,
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(text = source, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.muhosaba_evening_text),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onStart,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.textPrimary, contentColor = colors.background),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) { Text(stringResource(R.string.muhosaba_evening_start), style = MaterialTheme.typography.titleMedium) }
        TextButton(onClick = onLater, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.muhosaba_evening_later), color = colors.textSecondary)
        }
    }
}
