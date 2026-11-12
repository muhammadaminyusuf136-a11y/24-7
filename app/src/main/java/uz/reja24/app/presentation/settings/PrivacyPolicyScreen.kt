package uz.reja24.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uz.reja24.app.R
import uz.reja24.app.presentation.theme.RejaTheme

/** Maxfiylik siyosati (ilova ichida, internetsiz o'qiladi). */
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val colors = RejaTheme.colors
    Column(Modifier.fillMaxSize()) {
        SubScreenTopBar(title = stringResource(R.string.privacy_title), onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(stringResource(R.string.privacy_summary), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            listOf(
                R.string.privacy_section_data_title to R.string.privacy_section_data_body,
                R.string.privacy_section_accessibility_title to R.string.privacy_section_accessibility_body,
                R.string.privacy_section_permissions_title to R.string.privacy_section_permissions_body,
                R.string.privacy_section_sharing_title to R.string.privacy_section_sharing_body,
                R.string.privacy_section_delete_title to R.string.privacy_section_delete_body,
                R.string.privacy_section_contact_title to R.string.privacy_section_contact_body
            ).forEach { (title, body) ->
                Text(stringResource(title), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            }
            Text(
                text = stringResource(R.string.privacy_updated),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}
