package uz.reja24.app.core.util

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * Kechasi ekranni oq-qora qilish (Android'ning "rang tuzatish → monoxrom" sozlamasi).
 * Oddiy ilovalarga bu ruxsat berilmaydi — u telefonga USB orqali o'rnatishda bir marta beriladi:
 *   adb shell pm grant uz.reja24.app android.permission.WRITE_SECURE_SETTINGS
 * Ruxsat bo'lmasa — hech narsa qilinmaydi (sozlamalarda shu haqda yoziladi).
 *
 * Faqat o'zimiz yoqqan holatni o'chiramiz: foydalanuvchi o'zi yoqqan rang tuzatishiga tegmaymiz.
 */
object NightGrayscale {

    private const val KEY_ENABLED = "accessibility_display_daltonizer_enabled"
    private const val KEY_MODE = "accessibility_display_daltonizer"
    private const val MODE_MONOCHROME = 0
    private const val PREF_APPLIED = "grayscale_applied"
    private const val PREF_OLD_MODE = "grayscale_old_mode"

    fun isPermitted(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    /** Holatni qo'llaydi: on = true — oq-qora, false — o'zimiz yoqqan bo'lsak, avvalgiga qaytaramiz. */
    fun apply(context: Context, prefs: SharedPreferences, on: Boolean) {
        if (!isPermitted(context)) return
        val resolver = context.contentResolver
        val applied = prefs.getBoolean(PREF_APPLIED, false)
        try {
            if (on && !applied) {
                if (Settings.Secure.getInt(resolver, KEY_ENABLED, 0) == 1) return   // foydalanuvchi o'zi yoqqan
                prefs.edit().putInt(PREF_OLD_MODE, Settings.Secure.getInt(resolver, KEY_MODE, MODE_MONOCHROME)).apply()
                Settings.Secure.putInt(resolver, KEY_MODE, MODE_MONOCHROME)
                Settings.Secure.putInt(resolver, KEY_ENABLED, 1)
                prefs.edit().putBoolean(PREF_APPLIED, true).apply()
            } else if (!on && applied) {
                Settings.Secure.putInt(resolver, KEY_ENABLED, 0)
                Settings.Secure.putInt(resolver, KEY_MODE, prefs.getInt(PREF_OLD_MODE, MODE_MONOCHROME))
                prefs.edit().putBoolean(PREF_APPLIED, false).apply()
            }
        } catch (e: SecurityException) {
            // ruxsat olib qo'yilgan — jim o'tamiz
        }
    }
}
