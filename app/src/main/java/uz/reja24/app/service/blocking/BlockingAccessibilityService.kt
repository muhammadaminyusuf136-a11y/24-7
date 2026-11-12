package uz.reja24.app.service.blocking

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import uz.reja24.app.core.util.InstalledAppsProvider
import uz.reja24.app.core.util.SystemWhitelist
import uz.reja24.app.core.util.TimeProvider
import uz.reja24.app.domain.model.AppLanguage
import uz.reja24.app.domain.model.BackgroundStyle
import uz.reja24.app.domain.model.BlockPolicy
import uz.reja24.app.domain.model.RoutineSettings
import uz.reja24.app.domain.model.ThemeMode
import uz.reja24.app.domain.repository.AppStateRepository
import uz.reja24.app.domain.repository.BlockedAppRepository
import uz.reja24.app.domain.repository.RoutineSettingsRepository
import uz.reja24.app.domain.repository.StreakRepository
import uz.reja24.app.domain.repository.UserPreferencesRepository
import uz.reja24.app.presentation.block.BlockOverlayState
import uz.reja24.app.presentation.emergency.EmergencyActivity
import javax.inject.Inject
import kotlin.random.Random

/**
 * Bloklash xizmati.
 *
 * MAXFIYLIK: xizmat faqat "qaysi ilova ekranda ochildi" (paket nomi) degan hodisani oladi.
 * Ekrandagi matnlar, yozuvlar, parollar, xabarlar O'QILMAYDI
 * (accessibility_service_config.xml da canRetrieveWindowContent="false").
 */
@OptIn(ExperimentalCoroutinesApi::class)
@AndroidEntryPoint
class BlockingAccessibilityService : AccessibilityService() {

    @Inject lateinit var routineSettingsRepository: RoutineSettingsRepository
    @Inject lateinit var blockedAppRepository: BlockedAppRepository
    @Inject lateinit var appStateRepository: AppStateRepository
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository
    @Inject lateinit var streakRepository: StreakRepository
    @Inject lateinit var timeProvider: TimeProvider
    @Inject lateinit var installedAppsProvider: InstalledAppsProvider

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())

    // Tez ishlashi uchun oxirgi qiymatlar xotirada saqlanadi
    private var settings = RoutineSettings()
    private var blockedPackages: Set<String> = emptySet()
    private var bypassUntilMillis = 0L
    private var language: AppLanguage? = null
    private var themeMode = ThemeMode.SYSTEM
    private var backgroundStyle = BackgroundStyle.PURE
    private var allowOnly = false
    private var allowedPackages: Set<String> = emptySet()
    private var launchablePackages: Set<String> = emptySet()
    private var streakDays = 0
    private var whitelist: Set<String> = emptySet()
    private var lastPackage: String? = null
    private var overlay: BlockOverlayManager? = null

    /** Har 15 soniyada tekshiruv: masalan 22:00 bo'lganda foydalanuvchi allaqachon Instagram'da bo'lsa. */
    private val periodicCheck = object : Runnable {
        override fun run() {
            lastPackage?.let { evaluate(it) }
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        whitelist = SystemWhitelist.packages(this)
        overlay = BlockOverlayManager(this)

        scope.launch { routineSettingsRepository.observeSettings().collect { settings = it } }
        scope.launch { blockedAppRepository.observeBlockedPackages().collect { blockedPackages = it } }
        scope.launch { appStateRepository.bypassUntilMillis.collect { bypassUntilMillis = it } }
        scope.launch {
            userPreferencesRepository.preferences.collect { prefs ->
                language = if (prefs.isLanguageChosen) prefs.language else null
                themeMode = prefs.themeMode
                backgroundStyle = prefs.backgroundStyle
                allowOnly = prefs.nightAllowOnly
                allowedPackages = prefs.allowedPackages
            }
        }
        scope.launch {
            // Xizmat haftalab ishlaydi — "bugun" har daqiqada qayta aniqlanadi, streak eskirmasin
            flow {
                while (true) {
                    emit(timeProvider.today())
                    delay(DAY_CHECK_MS)
                }
            }
                .distinctUntilChanged()
                .flatMapLatest { streakRepository.observeSummary(it) }
                .collect { streakDays = it.current }
        }
        scope.launch {
            // "Faqat kerakli" rejim uchun: ochsa bo'ladigan ilovalar ro'yxati (yangi o'rnatilganlar ham kirsin)
            while (true) {
                launchablePackages = installedAppsProvider.launcherApps().map { it.packageName }.toSet()
                whitelist = SystemWhitelist.packages(this@BlockingAccessibilityService)
                delay(LAUNCHABLE_REFRESH_MS)
            }
        }
        handler.postDelayed(periodicCheck, CHECK_INTERVAL_MS)
        BlockingMonitorService.start(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        // O'zimizning oynalar, bildirishnoma paneli va klaviatura hisobga olinmaydi
        if (pkg == packageName || pkg in IGNORED_PACKAGES) return
        lastPackage = pkg
        evaluate(pkg)
    }

    private fun evaluate(pkg: String) {
        val now = timeProvider.now()
        val block = BlockPolicy.shouldBlock(
            packageName = pkg,
            time = now.toLocalTime(),
            nowMillis = timeProvider.currentMillis(),
            settings = settings,
            blockedPackages = blockedPackages,
            whitelist = whitelist,
            bypassUntilMillis = bypassUntilMillis,
            allowOnly = allowOnly,
            launchablePackages = launchablePackages,
            allowedPackages = allowedPackages
        )
        if (block) {
            // Bloklangan ilovadan chiqarib, bosh ekranga qaytaramiz va blok ekranini ko'rsatamiz
            performGlobalAction(GLOBAL_ACTION_HOME)
            showOverlay()
        } else if (pkg in whitelist) {
            // Qo'ng'iroq kelsa yoki soat ochilsa — blok ekrani xalaqit bermasin
            overlay?.hide()
        }
    }

    private fun showOverlay() {
        overlay?.show(
            overlayState = BlockOverlayState(
                unlockTime = settings.wakeTime,
                streakDays = streakDays,
                messageSeed = Random.nextInt(0, 1_000),
                // Blok ekrani ilovada tanlangan mavzu va fonda chiqadi
                themeMode = themeMode,
                backgroundStyle = backgroundStyle
            ),
            language = language,
            onEmergency = {
                overlay?.hide()
                openEmergency()
            },
            onClose = { overlay?.hide() }
        )
    }

    private fun openEmergency() {
        try {
            startActivity(
                Intent(this, EmergencyActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        } catch (e: Exception) {
            // ochib bo'lmasa — foydalanuvchi 24/7Reja ilovasidan kirishi mumkin
        }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        cleanup()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    private fun cleanup() {
        handler.removeCallbacks(periodicCheck)
        overlay?.hide()
        scope.cancel()
    }

    private companion object {
        const val CHECK_INTERVAL_MS = 15_000L
        const val DAY_CHECK_MS = 60_000L
        const val LAUNCHABLE_REFRESH_MS = 10 * 60_000L
        val IGNORED_PACKAGES = setOf(
            "com.android.systemui",
            "com.google.android.inputmethod.latin",
            "com.samsung.android.honeyboard",
            "com.touchtype.swiftkey",
            "com.miui.home.launcher.assistant"
        )
    }
}
