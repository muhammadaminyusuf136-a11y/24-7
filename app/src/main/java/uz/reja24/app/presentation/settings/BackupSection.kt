package uz.reja24.app.presentation.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import uz.reja24.app.R
import uz.reja24.app.core.backup.BackupManager
import uz.reja24.app.core.util.TimeProvider
import uz.reja24.app.presentation.components.ConfirmDialog
import uz.reja24.app.presentation.theme.RejaTheme
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    /** Natija xabari (string resurs id). */
    private val _messages = MutableSharedFlow<Int>(extraBufferCapacity = 2)
    val messages: SharedFlow<Int> = _messages

    fun fileName(): String = BackupManager.fileName(timeProvider.today())

    fun export(uri: Uri) = run {
        val ok = runCatching { backupManager.export(uri) }.isSuccess
        _messages.tryEmit(if (ok) R.string.backup_saved else R.string.backup_failed)
    }

    fun restore(uri: Uri) = run {
        when (runCatching { backupManager.restore(uri) }.getOrDefault(BackupManager.RestoreResult.InvalidFile)) {
            BackupManager.RestoreResult.Ok -> {
                _messages.tryEmit(R.string.backup_restored)
                kotlinx.coroutines.delay(1_200)
                backupManager.restartApp()
            }
            BackupManager.RestoreResult.InvalidFile -> _messages.tryEmit(R.string.backup_invalid)
            BackupManager.RestoreResult.NewerVersion -> _messages.tryEmit(R.string.backup_newer)
        }
    }

    private fun run(block: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                block()
            } finally {
                _busy.value = false
            }
        }
    }
}

/** Sozlamalardagi "Zaxira nusxa": faylga saqlash va fayldan tiklash. */
@Composable
fun BackupSection(viewModel: BackupViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val colors = RejaTheme.colors
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { res -> Toast.makeText(context, res, Toast.LENGTH_LONG).show() }
    }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) viewModel.export(uri)
    }
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingRestore = uri
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.backup_hint),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { saveLauncher.launch(viewModel.fileName()) },
                enabled = !busy,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.textPrimary, contentColor = colors.background),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
            ) {
                Icon(Icons.Outlined.Save, contentDescription = null)
                Text(" " + stringResource(R.string.backup_save), textAlign = TextAlign.Center)
            }
            OutlinedButton(
                onClick = { openLauncher.launch(arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed")) },
                enabled = !busy,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
            ) {
                Icon(Icons.Outlined.Restore, contentDescription = null, tint = colors.textPrimary)
                Text(" " + stringResource(R.string.backup_restore), color = colors.textPrimary, textAlign = TextAlign.Center)
            }
        }
    }

    pendingRestore?.let { uri ->
        ConfirmDialog(
            title = stringResource(R.string.backup_restore_title),
            text = stringResource(R.string.backup_restore_confirm),
            confirmLabel = stringResource(R.string.backup_restore),
            onDismiss = { pendingRestore = null },
            onConfirm = {
                pendingRestore = null
                viewModel.restore(uri)
            }
        )
    }
}
