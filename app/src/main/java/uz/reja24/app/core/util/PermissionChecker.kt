package uz.reja24.app.core.util

import android.Manifest
import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import uz.reja24.app.service.blocking.BlockingAccessibilityService

/** Barcha kerakli ruxsatlarning holati. */
data class PermissionStatus(
    val accessibility: Boolean,
    val overlay: Boolean,
    val batteryUnrestricted: Boolean,
    val notifications: Boolean,
    val exactAlarms: Boolean
) {
    /** Bloklash ishlashi uchun eng muhim ikkitasi. */
    val blockingReady: Boolean get() = accessibility && overlay
}

object PermissionChecker {

    fun status(context: Context) = PermissionStatus(
        accessibility = isAccessibilityEnabled(context),
        overlay = canDrawOverlays(context),
        batteryUnrestricted = isIgnoringBatteryOptimizations(context),
        notifications = hasNotificationPermission(context),
        exactAlarms = canScheduleExactAlarms(context)
    )

    fun isAccessibilityEnabled(context: Context): Boolean {
        val expected = ComponentName(context, BlockingAccessibilityService::class.java)
        val enabled: String? = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        } catch (e: SecurityException) {
            null
        }
        if (enabled.isNullOrEmpty()) return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }

    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(PowerManager::class.java) ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(AlarmManager::class.java) ?: return false
        return am.canScheduleExactAlarms()
    }

    // ---------- Sozlamalarni ochuvchi intentlar ----------

    fun accessibilitySettingsIntent() = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    fun overlaySettingsIntent(context: Context) =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))

    @android.annotation.SuppressLint("BatteryLife")
    fun batteryOptimizationIntent(context: Context) =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))

    fun exactAlarmIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
        } else {
            appDetailsIntent(context)
        }

    fun notificationSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    fun appDetailsIntent(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

    /**
     * Intentni xavfsiz ochadi: ba'zi telefonlarda (ayniqsa Xiaomi, Huawei) standart
     * sozlama sahifasi bo'lmasligi mumkin — shunda ilova ma'lumotlari sahifasi ochiladi.
     */
    fun openSafely(context: Context, intent: Intent): Boolean {
        val flagged = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(flagged)
            true
        } catch (e: ActivityNotFoundException) {
            fallback(context)
        } catch (e: SecurityException) {
            fallback(context)
        }
    }

    private fun fallback(context: Context): Boolean = try {
        context.startActivity(appDetailsIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        false
    }
}
