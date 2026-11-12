package uz.reja24.app.core.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import uz.reja24.app.R

/**
 * Telefon brendi. Xiaomi, Samsung, Huawei va boshqalar fondagi ilovalarni o'chirib
 * qo'yadi — har biri uchun alohida yo'riqnoma ko'rsatiladi.
 */
enum class PhoneBrand(@StringRes val titleRes: Int, @ArrayRes val stepsRes: Int) {
    XIAOMI(R.string.brand_xiaomi, R.array.brand_steps_xiaomi),
    SAMSUNG(R.string.brand_samsung, R.array.brand_steps_samsung),
    HUAWEI(R.string.brand_huawei, R.array.brand_steps_huawei),
    OPPO(R.string.brand_oppo, R.array.brand_steps_oppo),
    VIVO(R.string.brand_vivo, R.array.brand_steps_vivo),
    ONEPLUS(R.string.brand_oneplus, R.array.brand_steps_oneplus),
    OTHER(R.string.brand_other, R.array.brand_steps_other);

    companion object {
        fun detect(manufacturer: String = Build.MANUFACTURER, brand: String = Build.BRAND): PhoneBrand {
            val value = "$manufacturer $brand".lowercase()
            return when {
                listOf("xiaomi", "redmi", "poco").any { it in value } -> XIAOMI
                "samsung" in value -> SAMSUNG
                listOf("huawei", "honor").any { it in value } -> HUAWEI
                listOf("oppo", "realme").any { it in value } -> OPPO
                listOf("vivo", "iqoo").any { it in value } -> VIVO
                "oneplus" in value -> ONEPLUS
                else -> OTHER
            }
        }
    }
}

object BrandGuide {

    /** Brendning "avtomatik ishga tushish / batareya" sahifasiga olib boruvchi intentlar (ketma-ket sinaladi). */
    fun intentsFor(brand: PhoneBrand): List<Intent> = when (brand) {
        PhoneBrand.XIAOMI -> listOf(
            component("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            component("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")
        )
        PhoneBrand.SAMSUNG -> listOf(
            component("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
            component("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity")
        )
        PhoneBrand.HUAWEI -> listOf(
            component("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            component("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
        )
        PhoneBrand.OPPO -> listOf(
            component("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            component("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")
        )
        PhoneBrand.VIVO -> listOf(
            component("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            component("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")
        )
        PhoneBrand.ONEPLUS -> listOf(
            component("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity")
        )
        PhoneBrand.OTHER -> emptyList()
    }

    /** Brend sahifasini ochadi; topilmasa — ilova ma'lumotlari sahifasi. */
    fun open(context: Context, brand: PhoneBrand) {
        // Android 11+ da boshqa ilovalarni "ko'rish" cheklangan, shuning uchun
        // oldindan tekshirmasdan har birini ochib ko'ramiz — xato bo'lsa keyingisiga o'tamiz.
        for (candidate in intentsFor(brand)) {
            try {
                context.startActivity(candidate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (e: Exception) {
                // bu model/versiyada sahifa yo'q — keyingisini sinaymiz
            }
        }
        PermissionChecker.openSafely(context, PermissionChecker.appDetailsIntent(context))
    }

    private fun component(pkg: String, cls: String): Intent =
        Intent().setComponent(ComponentName(pkg, cls))
}
