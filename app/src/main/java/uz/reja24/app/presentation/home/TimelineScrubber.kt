package uz.reja24.app.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import uz.reja24.app.R
import uz.reja24.app.core.util.DurationText
import uz.reja24.app.core.util.TimeFormat
import uz.reja24.app.presentation.components.toComposeColor
import uz.reja24.app.presentation.components.prayerNameRes
import uz.reja24.app.presentation.theme.CategorySleep
import uz.reja24.app.presentation.theme.PrayerGreen
import uz.reja24.app.presentation.theme.RejaTheme
import kotlin.math.abs
import kotlin.math.roundToInt

private val HOUR_WIDTH: Dp = 64.dp
private val TRACK_HEIGHT: Dp = 136.dp
private val BLOCK_TOP: Dp = 24.dp
private val BLOCK_HEIGHT: Dp = 86.dp

/** Igna "hozir"dan shuncha daqiqa uzoqlashsa — "Hozirga" tugmasi chiqadi. */
private const val NOW_TOLERANCE_MINUTES = 8f

/**
 * Surib ko'riladigan kun tartibi lentasi (yumaloq soat o'rniga).
 * Igna o'rtada turadi, lenta chapga-o'ngga suriladi — xuddi videoni oldinga-orqaga surgandek.
 * Surilmagan paytda lenta o'zi hozirgi vaqt bilan birga yuradi.
 */
@Composable
fun TimelineSection(state: HomeUiState, modifier: Modifier = Modifier) {
    val colors = RejaTheme.colors
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val resources = LocalContext.current.resources
    val coroutineScope = rememberCoroutineScope()

    val pxPerMinute = with(density) { HOUR_WIDTH.toPx() } / 60f
    val trackStart = TimelineModel.trackStart(state.today)
    val blocks = remember(state.tasksByDate, state.settings, state.prayer.days, trackStart) {
        TimelineModel.build(trackStart, state.tasksByDate, state.settings, state.prayer.days)
    }
    val nowMinute = TimelineModel.MINUTES_IN_DAY +
        state.now.hour * 60 + state.now.minute + state.now.second / 60f

    val scroll = rememberScrollState()
    var viewportPx by remember { mutableIntStateOf(0) }
    var following by rememberSaveable { mutableStateOf(true) }
    val isDragged by scroll.interactionSource.collectIsDraggedAsState()

    fun scrollTargetFor(minute: Float): Int =
        (minute * pxPerMinute - viewportPx / 2f).roundToInt().coerceIn(0, scroll.maxValue)

    // Foydalanuvchi o'zi sursa — "hozir"ga ergashish to'xtaydi
    LaunchedEffect(isDragged) { if (isDragged) following = false }
    // Ergashish rejimida lenta hozirgi vaqt bilan birga siljiydi
    LaunchedEffect(following, viewportPx, nowMinute.toInt(), scroll.maxValue) {
        if (following && viewportPx > 0) scroll.scrollTo(scrollTargetFor(nowMinute))
    }

    val viewedMinute by remember(pxPerMinute) {
        derivedStateOf { (scroll.value + viewportPx / 2f) / pxPerMinute }
    }
    // Suriganda har soat chegarasida yengil titrash — soat g'ildiragini aylantirgandek
    LaunchedEffect(Unit) {
        snapshotFlow { (viewedMinute / 60).toInt() }
            .distinctUntilChanged()
            .drop(1)
            .collect { if (!following) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }

    val atNow = following || abs(viewedMinute - nowMinute) < NOW_TOLERANCE_MINUTES
    val dayLabels = listOf(
        stringResource(R.string.timeline_yesterday),
        stringResource(R.string.timeline_today),
        stringResource(R.string.timeline_tomorrow)
    )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Katta soat: hozir yoki ko'rilayotgan vaqt
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                text = TimeFormat.time(if (atNow) state.now.toLocalTime() else TimelineModel.timeOf(viewedMinute)),
                style = MaterialTheme.typography.displayMedium,
                color = colors.textPrimary
            )
            Spacer(Modifier.width(10.dp))
            val diff = (viewedMinute - nowMinute).roundToInt()
            val dayIndex = TimelineModel.dayIndexOf(viewedMinute)
            Text(
                text = when {
                    atNow -> stringResource(R.string.home_viewing_now)
                    diff < 0 -> stringResource(R.string.home_time_ago, DurationText.format(resources, -diff))
                    else -> stringResource(R.string.home_time_later, DurationText.format(resources, diff))
                } + if (!atNow && dayIndex != 1) " · " + dayLabels[dayIndex].lowercase() else "",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 8.dp)
            )
        }

        // Lenta — har doim chapdan o'ngga (arab tilida ham, soat kabi)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TRACK_HEIGHT)
                    .onSizeChanged { viewportPx = it.width }
                    .semantics {
                        contentDescription = resources.getString(R.string.cd_timeline, state.tasks.size.toString())
                    }
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .horizontalScroll(scroll)
                ) {
                    Box(
                        Modifier
                            .width(HOUR_WIDTH * (TimelineModel.TRACK_MINUTES / 60))
                            .fillMaxHeight()
                    ) {
                        HourGrid(dayLabels = dayLabels, trackStartDay = trackStart.dayOfWeek.value)
                        blocks.forEach { block ->
                            if (block.isPrayer) {
                                PrayerMarker(block = block, nowMinute = nowMinute, pxPerMinute = pxPerMinute)
                            } else {
                                TimelineBlockView(block = block, nowMinute = nowMinute, pxPerMinute = pxPerMinute)
                            }
                        }
                    }
                }
                // Chetlari xira — lenta davom etayotgani sezilsin
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .width(24.dp)
                        .fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(colors.background, Color.Transparent)))
                )
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .width(24.dp)
                        .fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(Color.Transparent, colors.background)))
                )
                // Igna — joyida turadi
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = BLOCK_TOP - 8.dp)
                        .size(10.dp)
                        .background(colors.accent, CircleShape)
                )
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = BLOCK_TOP - 4.dp)
                        .width(2.dp)
                        .height(BLOCK_HEIGHT + 8.dp)
                        .background(colors.accent)
                )
                Text(
                    // Hozirni ko'rsatayotganda — katta soat bilan bir xil (piksel yaxlitlanishi 1 daqiqa orqada qoldirmasin)
                    text = TimeFormat.time(if (atNow) state.now.toLocalTime() else TimelineModel.timeOf(viewedMinute)),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(colors.accent, RoundedCornerShape(9.dp))
                        .padding(horizontal = 8.dp, vertical = 1.dp)
                )
                // "Hozirga" tugmasi
                BackToNowButton(
                    visible = !atNow,
                    onClick = {
                        coroutineScope.launch {
                            scroll.animateScrollTo(scrollTargetFor(nowMinute))
                            following = true
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 8.dp)
                )
            }
        }
        // Bugungi kunning kichik xaritasi — bosilsa shu vaqtga o'tadi
        TodayMiniMap(
            blocks = blocks,
            nowMinute = nowMinute,
            viewedMinute = viewedMinute,
            onPick = { minute ->
                following = false
                coroutineScope.launch { scroll.animateScrollTo(scrollTargetFor(minute)) }
            }
        )

        ViewedInfoCard(
            block = TimelineModel.blockAt(blocks, viewedMinute),
            nowMinute = nowMinute,
            atNow = atNow
        )
    }
}

@Composable
private fun HourGrid(dayLabels: List<String>, trackStartDay: Int) {
    val colors = RejaTheme.colors
    val measurer = rememberTextMeasurer()
    val hourStyle = TextStyle(color = colors.textMuted, fontSize = 10.sp)
    val dayStyle = TextStyle(color = colors.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
    val weekdays = remember(trackStartDay) {
        (0 until TimelineModel.DAYS).map { java.time.DayOfWeek.of(Math.floorMod(trackStartDay - 1 + it, 7) + 1) }
    }
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    Canvas(Modifier.fillMaxSize()) {
        val hourPx = HOUR_WIDTH.toPx()
        for (hour in 0 until TimelineModel.TRACK_MINUTES / 60) {
            val x = hour * hourPx
            val dayStart = hour % 24 == 0
            drawLine(
                color = if (dayStart) colors.textSecondary.copy(alpha = 0.5f) else colors.border,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = if (dayStart) 2.dp.toPx() else 1.dp.toPx()
            )
            if (dayStart) {
                val day = hour / 24
                val weekday = weekdays[day].getDisplayName(java.time.format.TextStyle.FULL, locale)
                val label = "${dayLabels[day]} · $weekday".uppercase(locale)
                val layout = measurer.measure(label, dayStyle)
                drawText(layout, topLeft = Offset(x + 6.dp.toPx(), size.height - layout.size.height - 18.dp.toPx()))
            } else {
                val layout = measurer.measure(String.format(java.util.Locale.ROOT, "%02d:00", hour % 24), hourStyle)
                drawText(layout, topLeft = Offset(x + 4.dp.toPx(), 4.dp.toPx()))
            }
        }
    }
}

@Composable
private fun TimelineBlockView(block: TimelineBlock, nowMinute: Float, pxPerMinute: Float) {
    val density = LocalDensity.current
    val colors = RejaTheme.colors
    val x = with(density) { (block.startMinute * pxPerMinute).toDp() } + 2.dp
    val width = with(density) { ((block.endMinute - block.startMinute) * pxPerMinute).toDp() } - 4.dp
    if (width <= 0.dp) return
    val color = if (block.isSleep) CategorySleep else block.colorHex.orEmpty().toComposeColor()
    val isPast = block.endMinute <= nowMinute
    val isRunning = block.contains(nowMinute)
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .offset(x = x, y = BLOCK_TOP)
            .width(width)
            .height(BLOCK_HEIGHT)
            .alpha(if (isPast) 0.55f else 1f)
            .then(if (isRunning) Modifier.border(2.dp, colors.textPrimary, shape) else Modifier)
            .clip(shape)
            .background(color)
    ) {
        if (block.isSleep) SleepStars()
        if (isRunning) {
            // Qancha vaqt o'tgani — oq "to'lish"
            val fraction = ((nowMinute - block.startMinute) / (block.endMinute - block.startMinute)).coerceIn(0f, 1f)
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(Color.White.copy(alpha = 0.18f))
            )
        }
        if (width >= 36.dp) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = block.title ?: stringResource(R.string.sleep_block_title),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${TimeFormat.time(TimelineModel.timeOf(block.startMinute.toFloat()))}–" +
                        TimeFormat.time(TimelineModel.timeOf(block.endMinute.toFloat())),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1
                )
            }
        }
        if (block.isCompleted) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = stringResource(R.string.cd_mark_done),
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(14.dp)
            )
        }
    }
}

/** Namoz vaqti: yashil chiziq va nomi (reja bloklari ustida, o'rtada — reja nomini yopmaydi). */
@Composable
private fun PrayerMarker(block: TimelineBlock, nowMinute: Float, pxPerMinute: Float) {
    val prayer = block.prayer ?: return
    val density = LocalDensity.current
    val x = with(density) { (block.startMinute * pxPerMinute).toDp() }
    val fade = if (block.endMinute <= nowMinute) 0.55f else 1f
    Box(
        Modifier
            .offset(x = x - 1.dp, y = BLOCK_TOP)
            .width(2.dp)
            .height(BLOCK_HEIGHT)
            .alpha(fade)
            .background(PrayerGreen)
    )
    Text(
        text = stringResource(prayerNameRes(prayer)),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
        color = Color.White,
        maxLines = 1,
        modifier = Modifier
            .offset(x = x + 3.dp, y = BLOCK_TOP + BLOCK_HEIGHT / 2 - 9.dp)
            .alpha(fade)
            .background(PrayerGreen, RoundedCornerShape(6.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    )
}

/** Uyqu bloki ustidagi mayda yulduzchalar. */
@Composable
private fun SleepStars() {
    Canvas(Modifier.fillMaxSize()) {
        val step = 46.dp.toPx()
        var x = 12.dp.toPx()
        var i = 0
        while (x < size.width) {
            val y = size.height * (if (i % 2 == 0) 0.35f else 0.7f)
            drawCircle(Color.White.copy(alpha = 0.35f), radius = 1.2.dp.toPx(), center = Offset(x, y))
            x += step
            i++
        }
    }
}

@Composable
private fun TodayMiniMap(
    blocks: List<TimelineBlock>,
    nowMinute: Float,
    viewedMinute: Float,
    onPick: (Float) -> Unit
) {
    val colors = RejaTheme.colors
    val day = TimelineModel.MINUTES_IN_DAY.toFloat()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset -> onPick(day + offset.x / size.width * day) }
                }
        ) {
            val barTop = 3.dp.toPx()
            val barHeight = 8.dp.toPx()
            val radius = androidx.compose.ui.geometry.CornerRadius(barHeight / 2)
            drawRoundRect(colors.timelineEmpty, topLeft = Offset(0f, barTop),
                size = androidx.compose.ui.geometry.Size(size.width, barHeight), cornerRadius = radius)
            blocks.forEach { block ->
                val s = ((block.startMinute - day) / day).coerceIn(0f, 1f)
                val e = ((block.endMinute - day) / day).coerceIn(0f, 1f)
                if (e > s) {
                    drawRect(
                        color = if (block.isSleep) CategorySleep else block.colorHex.orEmpty().toComposeColor(),
                        topLeft = Offset(s * size.width, barTop),
                        size = androidx.compose.ui.geometry.Size((e - s) * size.width, barHeight)
                    )
                }
            }
            val nowX = ((nowMinute - day) / day).coerceIn(0f, 1f) * size.width
            drawCircle(colors.accent, radius = 3.5.dp.toPx(), center = Offset(nowX, barTop + barHeight / 2))
            if (viewedMinute in day..(2 * day)) {
                val viewX = (viewedMinute - day) / day * size.width
                drawLine(colors.textPrimary, Offset(viewX, 0f), Offset(viewX, size.height), strokeWidth = 2.5.dp.toPx())
            }
        }
    }
}

@Composable
private fun ViewedInfoCard(block: TimelineBlock?, nowMinute: Float, atNow: Boolean) {
    val colors = RejaTheme.colors
    val resources = LocalContext.current.resources
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val stripe = when {
                block == null -> colors.border
                block.isSleep -> CategorySleep
                else -> block.colorHex.orEmpty().toComposeColor()
            }
            Box(
                Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(stripe, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(if (atNow) R.string.home_now_label else R.string.home_at_this_time).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
                if (block == null) {
                    Text(stringResource(R.string.home_free_time), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Text(stringResource(R.string.home_free_hint), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                } else {
                    Text(
                        text = block.prayer?.let { stringResource(prayerNameRes(it)) }
                            ?: block.title ?: stringResource(R.string.sleep_block_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val status = when {
                        block.isPrayer && block.startMinute <= nowMinute -> stringResource(R.string.home_prayer_entered)
                        block.endMinute <= nowMinute -> stringResource(R.string.home_state_done)
                        block.contains(nowMinute) -> stringResource(
                            R.string.home_time_left,
                            DurationText.format(resources, (block.endMinute - nowMinute).roundToInt())
                        )
                        else -> stringResource(
                            R.string.home_state_starts_in,
                            DurationText.format(resources, (block.startMinute - nowMinute).roundToInt())
                        )
                    }
                    val range = if (block.isPrayer) {
                        TimeFormat.time(TimelineModel.timeOf(block.startMinute.toFloat()))
                    } else {
                        "${TimeFormat.time(TimelineModel.timeOf(block.startMinute.toFloat()))}–" +
                            TimeFormat.time(TimelineModel.timeOf(block.endMinute.toFloat()))
                    }
                    Text(
                        text = "$range · $status",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.accentText
                    )
                }
            }
        }
    }
}

/**
 * Alohida funksiya: AnimatedVisibility Column ichidagi Box'da to'g'ridan-to'g'ri chaqirilsa,
 * Kotlin ColumnScope versiyasini tanlab, kompilyatsiya xatosi beradi.
 */
@Composable
private fun BackToNowButton(visible: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = RejaTheme.colors
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            color = colors.textPrimary,
            modifier = Modifier.heightIn(min = 32.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.MyLocation,
                    contentDescription = null,
                    tint = colors.background,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.home_back_to_now),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.background
                )
            }
        }
    }
}