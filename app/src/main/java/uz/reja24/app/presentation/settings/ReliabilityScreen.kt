package uz.reja24.app.presentation.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import uz.reja24.app.R
import uz.reja24.app.core.util.BrandGuide
import uz.reja24.app.core.util.PermissionChecker
import uz.reja24.app.core.util.PermissionStatus
import uz.reja24.app.core.util.PhoneBrand
import uz.reja24.app.presentation.components.RejaCard
import uz.reja24.app.presentation.components.SectionLabel
import uz.reja24.app.presentation.theme.RejaTheme
import uz.reja24.app.presentation.theme.SuccessGreen

/** "Bloklash ishonchli ishlayaptimi?" — barcha ruxsatlar holati + telefon brendi bo'yicha yo'riqnoma. */
@Composable
fun ReliabilityScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        SubScreenTopBar(title = stringResource(R.string.reliability_title), onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PermissionChecklist()
            BrandGuideCard(PhoneBrand.detect())
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Ruxsatlar ro'yxati (✓ / ✗). Tanishuvning oxirgi qadamida ham ishlatiladi. */
@Composable
fun PermissionChecklist() {
    val context = LocalContext.current
    var status by remember { mutableStateOf(PermissionChecker.status(context)) }
    var showDisclosure by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { status = PermissionChecker.status(context) }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        status = PermissionChecker.status(context)
    }

    SummaryCard(status)
    PermissionRow(
        title = stringResource(R.string.perm_accessibility_title),
        description = stringResource(R.string.perm_accessibility_desc),
        granted = status.accessibility,
        onFix = { showDisclosure = true }
    )
    PermissionRow(
        title = stringResource(R.string.perm_overlay_title),
        description = stringResource(R.string.perm_overlay_desc),
        granted = status.overlay,
        onFix = { PermissionChecker.openSafely(context, PermissionChecker.overlaySettingsIntent(context)) }
    )
    PermissionRow(
        title = stringResource(R.string.perm_battery_title),
        description = stringResource(R.string.perm_battery_desc),
        granted = status.batteryUnrestricted,
        onFix = { PermissionChecker.openSafely(context, PermissionChecker.batteryOptimizationIntent(context)) }
    )
    PermissionRow(
        title = stringResource(R.string.perm_notifications_title),
        description = stringResource(R.string.perm_notifications_desc),
        granted = status.notifications,
        onFix = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                PermissionChecker.openSafely(context, PermissionChecker.notificationSettingsIntent(context))
            }
        }
    )
    PermissionRow(
        title = stringResource(R.string.perm_exact_alarm_title),
        description = stringResource(R.string.perm_exact_alarm_desc),
        granted = status.exactAlarms,
        onFix = { PermissionChecker.openSafely(context, PermissionChecker.exactAlarmIntent(context)) }
    )

    if (showDisclosure) {
        AccessibilityDisclosureDialog(
            onDismiss = { showDisclosure = false },
            onAccept = {
                showDisclosure = false
                PermissionChecker.openSafely(context, PermissionChecker.accessibilitySettingsIntent())
            }
        )
    }
}

@Composable
private fun SummaryCard(status: PermissionStatus) {
    val colors = RejaTheme.colors
    val allGood = status.blockingReady && status.batteryUnrestricted && status.notifications && status.exactAlarms
    RejaCard(
        modifier = Modifier.fillMaxWidth(),
        color = if (allGood) SuccessGreen.copy(alpha = 0.12f) else colors.accent.copy(alpha = 0.10f),
        borderColor = if (allGood) SuccessGreen else colors.accent.copy(alpha = 0.5f)
    ) {
        Text(
            text = stringResource(if (allGood) R.string.reliability_all_good else R.string.reliability_has_issues),
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary
        )
    }
}

@Composable
private fun BrandGuideCard(brand: PhoneBrand) {
    val context = LocalContext.current
    val colors = RejaTheme.colors
    val steps = stringArrayResource(brand.stepsRes)
    RejaCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.BatteryAlert, contentDescription = null, tint = colors.accent)
            Spacer(Modifier.width(10.dp))
            SectionLabel(stringResource(R.string.brand_guide_title, stringResource(brand.titleRes)))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.brand_guide_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary
        )
        Spacer(Modifier.height(8.dp))
        steps.forEachIndexed { index, step ->
            Row(Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "${index + 1}.",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.accentText,
                    modifier = Modifier.width(24.dp)
                )
                Text(step, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { BrandGuide.open(context, brand) },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.textPrimary, contentColor = colors.background),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Text(stringResource(R.string.brand_open_settings))
        }
    }
}
