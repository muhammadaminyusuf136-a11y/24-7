@file:OptIn(ExperimentalComposeUiApi::class)

package uz.reja24.app.presentation.emergency

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import uz.reja24.app.R
import uz.reja24.app.domain.model.BackgroundStyle
import uz.reja24.app.domain.model.ThemeMode
import uz.reja24.app.presentation.components.RejaBackground
import uz.reja24.app.presentation.theme.RejaTheme
import java.util.Locale

/**
 * Favqulodda kirish oqimi — alohida oynada (blok ekranidan ochiladi).
 * Bloky tajribasi: butunlay chetlab o'tib bo'lmaydigan bloklovchi 4-kuni o'chirib tashlanadi,
 * shuning uchun chiqish yo'li bor — lekin sekin va qiyin.
 */
@AndroidEntryPoint
class EmergencyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RejaTheme(themeMode = ThemeMode.DARK, backgroundStyle = BackgroundStyle.POWER) {
                RejaBackground(style = BackgroundStyle.POWER) {
                    EmergencyScreen(onFinish = { finish() })
                }
            }
        }
    }
}

@Composable
private fun EmergencyScreen(
    onFinish: () -> Unit,
    viewModel: EmergencyViewModel = hiltViewModel()
) {
    val stage by viewModel.stage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(stage) {
        if (stage == EmergencyStage.Granted) {
            Toast.makeText(context, context.getString(R.string.emergency_granted_toast), Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = stage,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
            // Sanoq har soniyada yangilanadi — animatsiya faqat bosqich almashganda
            contentKey = { it::class },
            label = "emergency-stage"
        ) { current ->
            when (current) {
                EmergencyStage.Loading -> CircularProgressIndicator(color = RejaTheme.colors.accent)
                is EmergencyStage.Intro -> IntroStage(current, onContinue = viewModel::startCountdown, onCancel = onFinish)
                is EmergencyStage.Countdown -> CountdownStage(current.secondsLeft, onCancel = onFinish)
                is EmergencyStage.Question -> QuestionStage(current, onSubmit = viewModel::submitAnswer, onCancel = onFinish)
                EmergencyStage.Granted -> ResultStage(
                    title = stringResource(R.string.emergency_granted_title),
                    text = stringResource(R.string.emergency_granted_text),
                    onDone = onFinish
                )
                EmergencyStage.LimitReached -> ResultStage(
                    title = stringResource(R.string.emergency_limit_title),
                    text = stringResource(R.string.emergency_limit_text),
                    onDone = onFinish
                )
            }
        }
    }
}

@Composable
private fun IntroStage(stage: EmergencyStage.Intro, onContinue: () -> Unit, onCancel: () -> Unit) {
    val colors = RejaTheme.colors
    val messages = stringArrayResource(R.array.emergency_messages)
    val template = messages[stage.messageIndex.coerceIn(0, messages.lastIndex)]
    val streakText = pluralStringResource(R.plurals.days_count, stage.streakDays, stage.streakDays.toString())
    val message = String.format(Locale.ROOT, template, streakText)

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = message,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.emergency_intro_note, stage.remaining.toString()),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        // Asosiy (katta) tugma — ortga qaytish
        Button(
            onClick = onCancel,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.textPrimary, contentColor = colors.background),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(stringResource(R.string.emergency_back_to_rest))
        }
        TextButton(onClick = onContinue, modifier = Modifier.height(48.dp)) {
            Text(stringResource(R.string.emergency_continue_anyway), color = colors.textMuted)
        }
    }
}

@Composable
private fun CountdownStage(secondsLeft: Int, onCancel: () -> Unit) {
    val colors = RejaTheme.colors
    val progress by animateFloatAsState(
        targetValue = secondsLeft / EmergencyViewModel.COUNTDOWN_SECONDS.toFloat(),
        animationSpec = tween(300),
        label = "countdown"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = stringResource(R.string.emergency_countdown_title),
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(180.dp),
                color = colors.accent,
                trackColor = colors.timelineEmpty,
                strokeWidth = 10.dp,
                strokeCap = StrokeCap.Round
            )
            Text(
                text = secondsLeft.toString(),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp),
                color = colors.textPrimary
            )
        }
        Text(
            text = stringResource(R.string.emergency_countdown_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
        TextButton(onClick = onCancel, modifier = Modifier.height(48.dp)) {
            Text(stringResource(R.string.emergency_back_to_rest), color = colors.textPrimary)
        }
    }
}

@Composable
private fun QuestionStage(stage: EmergencyStage.Question, onSubmit: (String) -> Unit, onCancel: () -> Unit) {
    val colors = RejaTheme.colors
    var answer by rememberSaveable(stage.problem) { mutableStateOf("") }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.emergency_question_title),
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary
        )
        Text(
            text = "${stage.problem.left} × ${stage.problem.right} = ?",
            style = MaterialTheme.typography.displayMedium,
            color = colors.textPrimary
        )
        OutlinedTextField(
            value = answer,
            onValueChange = { value -> answer = value.filter { it.isDigit() }.take(5) },
            singleLine = true,
            isError = stage.wrongAttempt,
            supportingText = if (stage.wrongAttempt) {
                { Text(stringResource(R.string.emergency_wrong_answer)) }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit(answer) }),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                cursorColor = colors.accent,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            ),
            textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { onSubmit(answer) },
            enabled = answer.isNotBlank(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(stringResource(R.string.emergency_submit))
        }
        Text(
            text = stringResource(R.string.emergency_penalty_note),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
            textAlign = TextAlign.Center
        )
        TextButton(onClick = onCancel, modifier = Modifier.height(48.dp)) {
            Text(stringResource(R.string.emergency_back_to_rest), color = colors.textPrimary)
        }
    }
}

@Composable
private fun ResultStage(title: String, text: String, onDone: () -> Unit) {
    val colors = RejaTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary, textAlign = TextAlign.Center)
        Text(text, style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary, textAlign = TextAlign.Center)
        Button(
            onClick = onDone,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.textPrimary, contentColor = colors.background),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(stringResource(R.string.action_close))
        }
    }
}
