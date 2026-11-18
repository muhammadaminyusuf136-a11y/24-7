package uz.reja24.app.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.reja24.app.R
import uz.reja24.app.presentation.components.RejaCard
import uz.reja24.app.presentation.theme.RejaTheme

/** Telegram kanalimiz. Nik shu yerda bir marta yoziladi (masalan "reja24_uz"); bo'sh bo'lsa — "tez orada". */
object ContactInfo {
    const val TELEGRAM_CHANNEL = ""

    val channelUrl: String? get() = TELEGRAM_CHANNEL.takeIf { it.isNotBlank() }?.let { "https://t.me/$it" }
}

/** Sozlamalar oxiri: "Biz bilan bog'lanish" — bosilsa iliq so'z va kanalga o'tish tugmasi. */
@Composable
fun ContactSection() {
    val colors = RejaTheme.colors
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }

    RejaCard(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, tint = colors.accent)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.contact_row), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Text(stringResource(R.string.contact_row_desc), style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.textMuted)
        }
    }

    if (open) {
        val url = ContactInfo.channelUrl
        AlertDialog(
            onDismissRequest = { open = false },
            containerColor = colors.surface,
            title = { Text(stringResource(R.string.contact_section), color = colors.textPrimary) },
            text = {
                Column {
                    Text(stringResource(R.string.contact_dialog_text), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    Text(
                        text = if (url != null) "@" + ContactInfo.TELEGRAM_CHANNEL else stringResource(R.string.contact_soon),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accentText,
                        modifier = Modifier.padding(top = 14.dp)
                    )
                }
            },
            confirmButton = {
                if (url != null) {
                    TextButton(onClick = {
                        open = false
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }) { Text(stringResource(R.string.contact_open)) }
                }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.action_close)) } }
        )
    }
}

