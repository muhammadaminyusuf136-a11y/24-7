package uz.reja24.app.core.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import uz.reja24.app.core.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton

/** Telefondagi ilova (bosh ekranda ko'rinadiganlari). */
data class InstalledApp(
    val packageName: String,
    val label: String
)

/**
 * O'rnatilgan ilovalar ro'yxati. QUERY_ALL_PACKAGES o'rniga manifestdagi <queries>
 * ishlatiladi — Google Play talabiga mos va maxfiyroq.
 */
@Singleton
class InstalledAppsProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun launcherApps(): List<InstalledApp> = withContext(ioDispatcher) {
        val pm = context.packageManager
        val whitelist = SystemWhitelist.packages(context)
        try {
            queryLauncher(pm)
                .asSequence()
                .mapNotNull { info ->
                    val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                    if (pkg in whitelist) return@mapNotNull null
                    InstalledApp(pkg, info.loadLabel(pm).toString())
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }
                .toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Ilova ikonkasi (ro'yxatda ko'rsatish uchun). Topilmasa null. */
    suspend fun icon(packageName: String, sizePx: Int): ImageBitmap? = withContext(ioDispatcher) {
        try {
            context.packageManager.getApplicationIcon(packageName)
                .toBitmap(sizePx, sizePx)
                .asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    fun isInstalled(packageName: String): Boolean = try {
        context.packageManager.getLaunchIntentForPackage(packageName) != null
    } catch (e: Exception) {
        false
    }

    private fun queryLauncher(pm: PackageManager): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
    }

    companion object {
        /**
         * Birinchi marta taklif qilinadigan chalg'ituvchi ilovalar
         * (Instagram, YouTube, TikTok, Telegram, Facebook, brauzerlar).
         */
        val SUGGESTED_DISTRACTIONS = setOf(
            "com.instagram.android",
            "com.google.android.youtube",
            "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
            "org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram",
            "com.facebook.katana", "com.twitter.android", "com.snapchat.android",
            "com.android.chrome", "org.mozilla.firefox", "com.opera.browser", "com.opera.mini.native",
            "com.sec.android.app.sbrowser", "com.yandex.browser", "com.microsoft.emmx",
            "com.mi.globalbrowser", "com.brave.browser"
        )
    }
}
