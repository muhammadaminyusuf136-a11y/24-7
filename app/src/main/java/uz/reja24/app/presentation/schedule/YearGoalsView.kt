package uz.reja24.app.presentation.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.reja24.app.R
import uz.reja24.app.domain.model.Goal
import uz.reja24.app.presentation.components.RejaCard
import uz.reja24.app.presentation.components.toComposeColor
import uz.reja24.app.presentation.theme.RejaTheme
import uz.reja24.app.presentation.theme.SuccessGreen
import kotlin.math.roundToInt

/** Yillik ko'rinish: uzoq muddatli maqsadlar va ularning bajarilish foizi. */
@Composable
internal fun YearGoalsView(viewModel: ScheduleViewModel) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Goal?>(null) }
    val colors = RejaTheme.colors
    val year = viewModel.today.year

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.goals_title, year.toString()),
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { editing = Goal(title = "", year = year) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.textPrimary, contentColor = colors.background)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text(stringResource(R.string.goals_add))
            }
        }
        if (goals.isEmpty()) {
            EmptyHint(stringResource(R.string.goals_empty))
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(goals, key = { it.id }) { goal ->
                RejaCard(onClick = { editing = goal }, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Flag,
                            contentDescription = null,
                            tint = if (goal.isAchieved) SuccessGreen else goal.colorHex.toComposeColor()
                        )
                        Text(
                            text = goal.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        )
                        Text(
                            text = stringResource(R.string.percent_value, goal.progressPercent.toString()),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.accentText
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { goal.progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (goal.isAchieved) SuccessGreen else colors.accent,
                        trackColor = colors.timelineEmpty,
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }
    }

    editing?.let { goal ->
        GoalDialog(
            goal = goal,
            onDismiss = { editing = null },
            onSave = {
                viewModel.saveGoal(it)
                editing = null
            },
            onDelete = if (goal.id != 0L) {
                {
                    viewModel.deleteGoal(goal.id)
                    editing = null
                }
            } else {
                null
            }
        )
    }
}

@Composable
private fun GoalDialog(
    goal: Goal,
    onDismiss: () -> Unit,
    onSave: (Goal) -> Unit,
    onDelete: (() -> Unit)?
) {
    var title by remember { mutableStateOf(goal.title) }
    var progress by remember { mutableFloatStateOf(goal.progressPercent.toFloat()) }
    var error by remember { mutableStateOf(false) }
    val colors = RejaTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text(stringResource(if (goal.id == 0L) R.string.goals_add else R.string.goals_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it.take(80)
                        error = false
                    },
                    label = { Text(stringResource(R.string.goals_field_title)) },
                    isError = error,
                    supportingText = if (error) {
                        { Text(stringResource(R.string.error_title_required)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.goals_progress, progress.roundToInt().toString()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
                Slider(
                    value = progress,
                    onValueChange = { progress = it },
                    valueRange = 0f..100f,
                    steps = 19,
                    colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent)
                )
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, tint = colors.accentText)
                        Text(stringResource(R.string.action_delete), color = colors.accentText)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank()) {
                    error = true
                } else {
                    val percent = progress.roundToInt().coerceIn(0, 100)
                    onSave(goal.copy(title = title.trim(), progressPercent = percent, isAchieved = percent == 100))
                }
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
