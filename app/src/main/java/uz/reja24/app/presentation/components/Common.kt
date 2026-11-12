@file:OptIn(ExperimentalMaterial3Api::class)

package uz.reja24.app.presentation.components

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.material3.TimeInput
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.toColorInt
import uz.reja24.app.R
import uz.reja24.app.domain.model.Category
import uz.reja24.app.domain.model.TrophyLevel
import uz.reja24.app.presentation.theme.CategoryOther
import uz.reja24.app.presentation.theme.RejaTheme
import java.time.LocalTime

/** "#D9663D" → Color. Noto'g'ri qiymatda "boshqa" toifa rangi. */
fun String.toComposeColor(): Color = try {
    Color(this.toColorInt())
} catch (e: RuntimeException) {
    // Bo'sh yoki buzilgan qiymat (masalan "") — IllegalArgument yoki StringIndexOutOfBounds
    CategoryOther
}

@StringRes
fun categoryNameRes(category: Category): Int = when (category) {
    Category.WORK -> R.string.category_work
    Category.STUDY -> R.string.category_study
    Category.SPORT -> R.string.category_sport
    Category.FAMILY -> R.string.category_family
    Category.READING -> R.string.category_reading
    Category.REST -> R.string.category_rest
    Category.SLEEP -> R.string.category_sleep
    Category.MORNING -> R.string.category_morning
    Category.OTHER -> R.string.category_other
}

@StringRes
fun trophyNameRes(level: TrophyLevel): Int = when (level) {
    TrophyLevel.NONE -> R.string.trophy_none
    TrophyLevel.BRONZE -> R.string.trophy_bronze
    TrophyLevel.SILVER -> R.string.trophy_silver
    TrophyLevel.GOLD -> R.string.trophy_gold
    TrophyLevel.DIAMOND -> R.string.trophy_diamond
}

fun trophyColor(level: TrophyLevel): Color = when (level) {
    TrophyLevel.NONE -> CategoryOther
    TrophyLevel.BRONZE -> Color(0xFFB0703C)
    TrophyLevel.SILVER -> Color(0xFF8E9AA6)
    TrophyLevel.GOLD -> Color(0xFFD4A72C)
    TrophyLevel.DIAMOND -> Color(0xFF4FA3C7)
}

/** Oq (yoki mavzuga mos) karta — chegara bilan. */
@Composable
fun RejaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = RejaTheme.colors.surface,
    borderColor: Color = RejaTheme.colors.border,
    cornerRadius: Dp = 18.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val border = BorderStroke(1.dp, borderColor)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, border = border) {
            Column(Modifier.padding(contentPadding)) { content() }
        }
    } else {
        Surface(modifier = modifier, shape = shape, color = color, border = border) {
            Column(Modifier.padding(contentPadding)) { content() }
        }
    }
}

/** Kichik, katta harfli bo'lim sarlavhasi. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = RejaTheme.colors.textMuted,
        modifier = modifier
    )
}

/** 5 ta yulduzcha. */
@Composable
fun StarsRow(stars: Int, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    val description = stringResource(R.string.cd_stars, stars.toString())
    Row(
        modifier = modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(5) { index ->
            val filled = index < stars
            Icon(
                imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = if (filled) Color(0xFFE0A44A) else RejaTheme.colors.textMuted,
                modifier = Modifier.size(size)
            )
        }
    }
}

/**
 * Soat tanlash oynasi (24 soatlik format). Ikki usul: raqamlarni yozish (telefon raqam
 * klaviaturasi chiqadi) yoki aylanadigan soat. Pastda tez to'g'rilash: −15, −5, +5, +15 daqiqa.
 */
@Composable
fun RejaTimePickerDialog(
    title: String,
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true
    )
    // Standart — raqam yozish: tezroq va aniqroq
    var typing by rememberSaveable { mutableStateOf(true) }
    val colors = TimePickerDefaults.colors(
        selectorColor = RejaTheme.colors.accent,
        clockDialColor = RejaTheme.colors.background,
        timeSelectorSelectedContainerColor = RejaTheme.colors.accent.copy(alpha = 0.18f),
        timeSelectorUnselectedContainerColor = RejaTheme.colors.background,
        timeSelectorSelectedContentColor = RejaTheme.colors.accentText,
        timeSelectorUnselectedContentColor = RejaTheme.colors.textPrimary
    )
    fun shift(minutes: Int) {
        val total = Math.floorMod(state.hour * 60 + state.minute + minutes, 24 * 60)
        state.hour = total / 60
        state.minute = total % 60
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = RejaTheme.colors.surface
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = RejaTheme.colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    // Klaviatura ↔ aylanadigan soat
                    IconButton(onClick = { typing = !typing }) {
                        Icon(
                            imageVector = if (typing) Icons.Outlined.Schedule else Icons.Outlined.Keyboard,
                            contentDescription = stringResource(if (typing) R.string.time_mode_dial else R.string.time_mode_keyboard),
                            tint = RejaTheme.colors.textSecondary
                        )
                    }
                }
                if (typing) {
                    TimeInput(state = state, colors = colors)
                } else {
                    TimePicker(state = state, colors = colors)
                }
                // Tez to'g'rilash
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(-15, -5, 5, 15).forEach { delta ->
                        OutlinedButton(
                            onClick = { shift(delta) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.heightIn(min = 40.dp)
                        ) {
                            Text(
                                text = if (delta > 0) "+$delta" else "−${-delta}",
                                style = MaterialTheme.typography.labelLarge,
                                color = RejaTheme.colors.textPrimary
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                    TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                        Text(stringResource(R.string.action_ok))
                    }
                }
            }
        }
    }
}
/** Tasdiqlash oynasi (o'chirish va h.k.). */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = RejaTheme.colors.accentText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        containerColor = RejaTheme.colors.surface
    )
}

/** Rejim qulfi haqida ogohlantirish: qulf belgisi va matn. */
@Composable
fun LockBanner(text: String, modifier: Modifier = Modifier) {
    val colors = RejaTheme.colors
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
        }
    }
}
