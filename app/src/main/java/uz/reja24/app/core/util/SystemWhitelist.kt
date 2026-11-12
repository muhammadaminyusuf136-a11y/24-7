package uz.reja24.app.core.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import android.telecom.TelecomManager

/**
 * HECH QACHON bloklanmaydigan ilovalar: qo'ng'iroq, SMS, favqulodda xizmatlar,
 * soat/budilnik, tizim sozlamalari va 24/7Reja'ning o'zi.
 */
object SystemWhitelist {

    private val STATIC_PACKAGES = setOf(
        // Qo'ng'iroq va telefon
        "com.android.dialer", "com.google.android.dialer", "com.samsung.android.dialer",
        "com.android.phone", "com.android.server.telecom", "com.android.incallui",
        "com.miui.voip", "com.android.contacts", "com.google.android.contacts",
        "com.samsung.android.app.contacts", "com.huawei.contacts",
        // SMS
        "com.android.mms", "com.google.android.apps.messaging",
        "com.samsung.android.messaging", "com.android.messaging",
        // Favqulodda xizmatlar
        "com.android.emergency", "com.google.android.apps.safetyhub",
        "com.samsung.android.emergency", "com.android.cellbroadcastreceiver",
        "com.google.android.cellbroadcastreceiver",
        // Soat / budilnik
        "com.android.deskclock", "com.google.android.deskclock",
        "com.sec.android.app.clockpackage", "com.huawei.deskclock",
        "com.oneplus.deskclock", "com.coloros.alarmclock", "com.vivo.alarmclock",
        // Tizim
        "com.android.settings", "com.android.systemui", "com.miui.securitycenter",
        "com.android.permissioncontroller", "com.google.android.permissioncontroller",
        "com.android.packageinstaller", "com.google.android.packageinstaller"
    )

    /** Oq ro'yxat: statik + telefondagi standart qo'ng'iroq va SMS ilovasi + o'zimiz. */
    fun packages(context: Context): Set<String> {
        val result = STATIC_PACKAGES.toMutableSet()
        result += context.packageName
        try {
            context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage?.let(result::add)
        } catch (e: SecurityException) {
            // ba'zi qurilmalarda ruxsatsiz o'qib bo'lmaydi — statik ro'yxat yetarli
        }
        try {
            Telephony.Sms.getDefaultSmsPackage(context)?.let(result::add)
        } catch (e: Exception) {
            // SMS moduli yo'q qurilmalar (planshet)
        }
        // Bosh ekran ilovalari (launcher): bloklansa, "bosh ekranga qaytarish" cheksiz aylanib qolardi
        try {
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            context.packageManager.queryIntentActivities(home, PackageManager.MATCH_DEFAULT_ONLY)
                .forEach { info -> info.activityInfo?.packageName?.let(result::add) }
        } catch (e: Exception) {
            // aniqlab bo'lmasa — statik ro'yxat qoladi
        }
        return result
    }
}
