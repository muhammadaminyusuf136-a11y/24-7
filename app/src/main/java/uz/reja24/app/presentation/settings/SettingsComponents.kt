package uz.reja24.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uz.reja24.app.R
import uz.reja24.app.presentation.components.RejaCard
import uz.reja24.app.presentation.theme.RejaTheme
import uz.reja24.app.presentation.theme.SuccessGreen

/** Ichki ekranlar sarlavhasi: orqaga tugmasi (RTL da avtomatik aks etadi) + nom. */
@Composable
fun SubScreenTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                tint = RejaTheme.colors.textPrimary
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = RejaTheme.colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        actions()
    }
}

/** Ruxsat qatori: nomi, oddiy tushuntirish, holat belgisi va "Yoqish" tugmasi. */
@Composable
fun PermissionRow(
    title: String,
    description: String,
    granted: Boolean,
    onFix: () -> Unit
) {
    val colors = RejaTheme.colors
    RejaCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (granted) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
                contentDescription = stringResource(if (granted) R.string.cd_status_ok else R.string.cd_status_missing),
                tint = if (granted) SuccessGreen else colors.accentText,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Text(description, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
        if (!granted) {
            OutlinedButton(
                onClick = onFix,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .heightIn(min = 48.dp)
                    .fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_grant), color = colors.textPrimary)
            }
        }
    }
}

/**
 * Google Play talabi (Prominent Disclosure): Accessibility ruxsatini so'rashdan OLDIN
 * foydalanuvchiga nima uchun kerakligi va nima o'qilmasligi aniq tushuntiriladi.
 */
@Composable
fun AccessibilityDisclosureDialog(onDismiss: () -> Unit, onAccept: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RejaTheme.colors.surface,
        title = { Text(stringResource(R.string.disclosure_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.disclosure_body))
                Text(
                    text = stringResource(R.string.disclosure_privacy),
                    style = MaterialTheme.typography.bodyMedium,
                    color = RejaTheme.colors.textSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text(stringResource(R.string.disclosure_accept), color = RejaTheme.colors.accentText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.disclosure_decline)) }
        }
    )
}
