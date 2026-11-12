package uz.reja24.app.presentation.components

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import uz.reja24.app.R
import uz.reja24.app.presentation.theme.RejaTheme

/**
 * Budilnik ovozini tanlash qatori (Android'ning o'z ovoz tanlash oynasi ochiladi).
 * Standart budilnik tanlansa — null saqlanadi (telefon ovozi o'zgarsa, shunga ergashadi).
 */
@Composable
fun AlarmSoundRow(title: String, soundUri: String?, onPicked: (String?) -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.let {
                IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            }
            onPicked(uri?.takeIf { it != Settings.System.DEFAULT_ALARM_ALERT_URI }?.toString())
        }
    }
    val soundName = remember(soundUri) {
        soundUri?.let { uri ->
            runCatching { RingtoneManager.getRingtone(context, Uri.parse(uri))?.getTitle(context) }.getOrNull()
        }
    } ?: stringResource(if (soundUri == null) R.string.alarm_sound_default else R.string.settings_ringtone_custom)

    ValueSettingRow(title = title, value = soundName) {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_ALARM_ALERT_URI)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            .putExtra(
                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                soundUri?.let { Uri.parse(it) } ?: Settings.System.DEFAULT_ALARM_ALERT_URI
            )
        runCatching { launcher.launch(intent) }
    }
}

/** Sarlavha va o'ngda qiymat — bosiladigan qator. */
@Composable
fun ValueSettingRow(title: String, value: String, onClick: () -> Unit) {
    val colors = RejaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary, modifier = Modifier.weight(1f))
        // Uzun qiymat (masalan hisoblash usuli nomi) sarlavhani siqib qo'ymasligi uchun joy teng bo'linadi
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = colors.accentText,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(start = 12.dp)
        )
    }
}

/** Sarlavha, izoh va o'ngda kalit. */
@Composable
fun SwitchSettingRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = RejaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = colors.accent)
        )
    }
}
