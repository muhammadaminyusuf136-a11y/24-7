package uz.reja24.app.presentation.block

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import uz.reja24.app.R
import uz.reja24.app.domain.model.BackgroundStyle
import uz.reja24.app.presentation.components.RejaBackground
import uz.reja24.app.presentation.theme.RejaTheme
import uz.reja24.app.presentation.theme.SpaceGrotesk

/** Kunduzgi nazorat oynasi holati. */
sealed interface DayControlState {
    val packageName: String
    val appName: String
    val backgroundStyle: BackgroundStyle

    /** "O'ylab ko'r" pauzasi: ilovani ochishdan oldin bir necha soniya kutiladi. */
    data class Pause(
        override val packageName: String,
        override val appName: String,
        override val backgroundStyle: BackgroundStyle,
        val seconds: Int,
        /** Kunlik chegara bo'lsa: bugun ishlatilgan / chegara (daqiqa). */
        val usedMinutes: Int? = null,
        val limitMinutes: Int? = null
    ) : DayControlState

    /** Kunlik vaqt chegarasi tugadi — ertagacha ochilmaydi. */
    data class LimitReached(
        override val packageName: String,
        override val appName: String,
        override val backgroundStyle: BackgroundStyle,
        val limitMinutes: Int
    ) : DayControlState
}

@Composable
fun DayControlScreen(state: DayControlState, onOpen: () -> Unit, onClose: () -> Unit) {
    RejaTheme(backgroundStyle = state.backgroundStyle) {
        RejaBackground(style = state.backgroundStyle) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (state) {
                    is DayControlState.Pause -> PauseContent(state, onOpen, onClose)
                    is DayControlState.LimitReached -> LimitContent(state, onClose)
                }
            }
        }
    }
}

@Composable
private fun PauseContent(state: DayControlState.Pause, onOpen: () -> Unit, onClose: () -> Unit) {
    val colors = RejaTheme.colors
    var left by remember(state.packageName) { mutableIntStateOf(state.seconds) }
    LaunchedEffect(state.packageName) {
        while (left > 0) {
            delay(1_000)
            left -= 1
        }
    }
    Text(
        text = stringResource(R.string.pause_title),
        style = MaterialTheme.typography.headlineSmall,
        color = colors.textPrimary,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.pause_text, state.appName),
        style = MaterialTheme.typography.bodyLarge,
        color = colors.textSecondary,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(32.dp))
    // Sanoq: aylana ichida qolgan soniyalar
    Box(
        modifier = Modifier
            .size(120.dp)
            .border(3.dp, if (left > 0) colors.accent else colors.border, CircleShape)
            .background(colors.surface, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (left > 0) left.toString() else "✓",
            style = MaterialTheme.typography.displayMedium.copy(fontFamily = SpaceGrotesk, fontSize = 44.sp),
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary
        )
    }
    if (state.limitMinutes != null && state.usedMinutes != null) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.pause_usage, state.usedMinutes.toString(), state.limitMinutes.toString()),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textMuted,
            textAlign = TextAlign.Center
        )
    }
    Spacer(Modifier.height(36.dp))
    Button(
        onClick = onClose,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = androidx.compose.ui.graphics.Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
    ) { Text(stringResource(R.string.pause_skip), style = MaterialTheme.typography.titleSmall) }
    Spacer(Modifier.height(10.dp))
    OutlinedButton(
        onClick = onOpen,
        enabled = left == 0,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, if (left == 0) colors.textSecondary else colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        Text(
            text = if (left > 0) stringResource(R.string.pause_open_wait, left.toString())
            else stringResource(R.string.pause_open, state.appName),
            color = if (left > 0) colors.textMuted else colors.textPrimary
        )
    }
}

@Composable
private fun LimitContent(state: DayControlState.LimitReached, onClose: () -> Unit) {
    val colors = RejaTheme.colors
    Text(
        text = stringResource(R.string.limit_title),
        style = MaterialTheme.typography.headlineSmall,
        color = colors.textPrimary,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.limit_text, state.appName, state.limitMinutes.toString()),
        style = MaterialTheme.typography.bodyLarge,
        color = colors.textSecondary,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(36.dp))
    Button(
        onClick = onClose,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = androidx.compose.ui.graphics.Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
    ) { Text(stringResource(R.string.limit_home), style = MaterialTheme.typography.titleSmall) }
}
