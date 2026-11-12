package uz.reja24.app.presentation.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import uz.reja24.app.R
import uz.reja24.app.core.util.TimeFormat
import uz.reja24.app.domain.model.BackgroundStyle
import uz.reja24.app.domain.model.ThemeMode
import uz.reja24.app.presentation.theme.RejaTheme
import uz.reja24.app.service.alarm.AlarmRingService
import java.time.LocalTime

/**
 * Budilnik oynasi: telefon qulflangan bo'lsa ham ochiladi va ekranni yoqadi.
 * Ovozni AlarmRingService chaladi — oyna yopilsa ham ovoz to'xtamaydi, faqat tugmalar to'xtatadi.
 */
class AlarmActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val alarm by AlarmRingService.ringing.collectAsStateWithLifecycle()
            // Budilnik boshqa joydan (bildirishnoma tugmasi, 10 daqiqa o'tishi) to'xtasa — oyna ham yopiladi
            LaunchedEffect(alarm) { if (alarm == null) finish() }
            val current = alarm ?: return@setContent
            RejaTheme(themeMode = ThemeMode.DARK, backgroundStyle = BackgroundStyle.PURE) {
                AlarmScreen(
                    title = current.title,
                    text = current.text,
                    onSnooze = { AlarmRingService.send(this, AlarmRingService.ACTION_SNOOZE) },
                    onStop = { AlarmRingService.send(this, AlarmRingService.ACTION_STOP) }
                )
            }
        }
    }
}

@Composable
private fun AlarmScreen(title: String, text: String, onSnooze: () -> Unit, onStop: () -> Unit) {
    val colors = RejaTheme.colors
    val now by produceState(initialValue = LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(1_000)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(colors.accent.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Alarm, contentDescription = null, tint = colors.accent, modifier = Modifier.size(44.dp))
            }
            Text(
                text = TimeFormat.time(now),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp, lineHeight = 76.sp),
                color = colors.textPrimary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            if (text.isNotBlank() && text != title) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onSnooze,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text(
                    stringResource(R.string.alarm_snooze, AlarmRingService.SNOOZE_MINUTES.toString()),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
            }
            Button(
                onClick = onStop,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Text(stringResource(R.string.alarm_stop), style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
