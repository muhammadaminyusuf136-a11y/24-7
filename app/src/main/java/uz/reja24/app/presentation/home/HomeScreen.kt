@file:OptIn(ExperimentalComposeUiApi::class)

package uz.reja24.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.reja24.app.R
import uz.reja24.app.core.util.PermissionChecker
import uz.reja24.app.core.util.TimeFormat
import uz.reja24.app.presentation.components.FullDateText
import uz.reja24.app.presentation.components.LocalDateStyle
import uz.reja24.app.presentation.components.RejaCard
import uz.reja24.app.presentation.components.SectionLabel
import uz.reja24.app.presentation.theme.NavyAccent
import uz.reja24.app.presentation.theme.InkBlack
import uz.reja24.app.presentation.theme.RejaTheme
import java.time.Duration

@Composable
fun HomeScreen(
    onOpenReliability: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenPrayer: () -> Unit,
    onOpenMuhosaba: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var blockingReady by remember { mutableStateOf(true) }
    // Ruxsatlar holatini har safar ekranga qaytganda tekshiramiz
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        blockingReady = PermissionChecker.status(context).blockingReady
    }

    val current = state
    if (current == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = RejaTheme.colors.accent)
        }
        return
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HomeHeader(current)
            if (!blockingReady) PermissionBanner(onClick = onOpenReliability)
            if (wide) {
                // Planshet: lenta chapda, kartalar o'ngda
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    TodayTable(current, onOpenPlan, modifier = Modifier.weight(1f))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) { InfoCards(current, onOpenPlan, onOpenPrayer, onOpenMuhosaba) }
                }
            } else {
                TodayTable(current, onOpenPlan)
                InfoCards(current, onOpenPlan, onOpenPrayer, onOpenMuhosaba)
            }
        }
    }
}

/** Hafta kuni, tepada hijriy va pastda milodiy sana (to'liq), o'ngda ketma-ketlik. */
@Composable
private fun HomeHeader(state: HomeUiState) {
    val locale = LocalConfiguration.current.locales[0]
    val resources = LocalContext.current.resources
    val colors = RejaTheme.colors
    val date = state.today
    val hijri = FullDateText.hijri(resources, date, state.prayer.settings.hijriAdjustmentDays)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = TimeFormat.weekday(date, locale).uppercase(locale),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textMuted,
                modifier = Modifier.weight(1f)
            )
            StreakChip(days = state.streakDays)
        }
        val dateStyle = LocalDateStyle.current
        if (hijri != null && dateStyle.showHijri) DateLine(label = stringResource(R.string.date_hijri_label), text = hijri)
        if (dateStyle.showGregorian || hijri == null) {
            DateLine(label = stringResource(R.string.date_gregorian_label), text = FullDateText.gregorian(resources, date))
        }
    }
}

@Composable
private fun DateLine(label: String, text: String) {
    val colors = RejaTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textMuted,
            modifier = Modifier
                .widthIn(min = 72.dp)
                .padding(end = 10.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary
        )
    }
}

@Composable
private fun StreakChip(days: Int) {
    val colors = RejaTheme.colors
    Row(
        modifier = Modifier
            .background(colors.surface, CircleShape)
            .border(1.dp, colors.border, CircleShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.LocalFireDepartment,
            contentDescription = stringResource(R.string.cd_streak),
            tint = colors.accent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = pluralStringResource(R.plurals.days_count, days, days.toString()),
            style = MaterialTheme.typography.labelLarge,
            color = colors.textPrimary
        )
    }
}

@Composable
private fun PermissionBanner(onClick: () -> Unit) {
    val colors = RejaTheme.colors
    RejaCard(
        onClick = onClick,
        color = colors.accent.copy(alpha = 0.12f),
        borderColor = colors.accent.copy(alpha = 0.5f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = colors.accentText)
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.banner_blocking_off),
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = stringResource(R.string.cd_open),
                tint = colors.textSecondary
            )
        }
    }
}

@Composable
private fun InfoCards(
    state: HomeUiState,
    onOpenPlan: () -> Unit,
    onOpenPrayer: () -> Unit,
    onOpenMuhosaba: () -> Unit
) {
    NextCard(state, onOpenPlan)
    PrayerCard(state, onOpenPrayer)
    MuhosabaCard(onOpenMuhosaba)
}

@Composable
private fun NextCard(state: HomeUiState, onOpenPlan: () -> Unit) {
    val next = state.next
    Surface(
        onClick = onOpenPlan,
        shape = RoundedCornerShape(20.dp),
        color = InkBlack,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_next).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = when {
                        next == null -> stringResource(R.string.home_no_next)
                        next.title == null -> stringResource(R.string.home_next_routine)
                        else -> next.title
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (next != null) {
                val minutes = Duration.between(state.now, next.startsAt).toMinutes()
                Text(
                    text = TimeFormat.hoursMinutes(minutes),
                    style = MaterialTheme.typography.titleLarge,
                    color = NavyAccent,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/** Kunni hisob qilishga chaqiruvchi karta — "Muhosaba" bo'limini ochadi. */
@Composable
private fun MuhosabaCard(onOpen: () -> Unit) {
    val colors = RejaTheme.colors
    RejaCard(onClick = onOpen, modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionLabel(stringResource(R.string.muhosaba_title))
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_muhosaba_hint),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary
                )
            }
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = stringResource(R.string.cd_open),
                tint = colors.textMuted
            )
        }
    }
}
