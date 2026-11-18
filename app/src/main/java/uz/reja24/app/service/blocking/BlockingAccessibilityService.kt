package uz.reja24.app.service.blocking

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
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
import uz.reja24.app.domain.model.ClockGuard
import uz.reja24.app.domain.model.DayControl
import uz.reja24.app.domain.model.RoutineSettings
import uz.reja24.app.domain.repository.AppRule
import uz.reja24.app.domain.repository.AppRuleRepository
import uz.reja24.app.domain.repository.AppStateRepository
import uz.reja24.app.domain.repository.BlockedAppRepository
import uz.reja24.app.domain.repository.RoutineSettingsRepository
import uz.reja24.app.domain.repository.ScoreRepository
import uz.reja24.app.domain.repository.StreakRepository
import uz.reja24.app.domain.repository.UserPreferencesRepository
import uz.reja24.app.presentation.block.BlockOverlayState
import uz.reja24.app.presentation.block.DayControlState
import uz.reja24.app.presentation.emergency.EmergencyActivity
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.random.Random

/**
 * Bloklash xizmati: tungi rejim, soatni surib qo'yishdan himoya va kunduzgi nazorat
 * (ilova uchun kunlik vaqt chegarasi, ochishdan oldin "o'ylab ko'r" pauzasi).
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
    @Inject lateinit var scoreRepository: ScoreRepository
    @Inject lateinit var appRuleRepository: AppRuleRepository
    @Inject lateinit var timeProvider: TimeProvider
    @Inject lateinit var installedAppsProvider: InstalledAppsProvider

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())

    // Tez ishlashi uchun oxirgi qiymatlar xotirada saqlanadi
    private var settings = RoutineSettings()
    private var blockedPackages: Set<String> = emptySet()
    private var bypassUntilMillis = 0L
    private var language: AppLanguage? = null
    private var backgroundStyle = BackgroundStyle.PURE
    private var allowOnly = false
    private var allowedPackages: Set<String> = emptySet()
    private var launchablePackages: Set<String> = emptySet()
    private var streakDays = 0
    private var whitelist: Set<String> = emptySet()
    private var lastPackage: String? = null
    private var overlay: BlockOverlayManager? = null

    // ---- Kunduzgi nazorat ----
    private var rules: Map<String, AppRule> = emptyMap()
    /** Bugun ilovalar necha millisekund ochiq turgani (faqat qoidasi borlar). */
    private val usageMs = HashMap<String, Long>()
    private var usageDate: LocalDate? = null
    /** Hozir ekrandagi ilova va u qachondan beri hisoblanmoqda (hisoblagich bo'yicha). */
    private var currentPkg: String? = null
    private var currentSince = 0L
    private var screenOn = true
    /** Ilova oxirgi marta qachon ochiq bo'lgan (pauzadan keyin) — yaqinda bo'lsa, pauza qayta so'ralmaydi. */
    private val pausePassed = HashMap<String, Long>()

    // ---- Soat himoyasi ----
    private val guardPrefs by lazy { getSharedPreferences(GUARD_PREFS, MODE_PRIVATE) }
    private var lockUntilElapsed = 0L
    private var lockDay: LocalDate? = null
    private var clockOffset = 0L

    /** Har 15 soniyada tekshiruv: masalan 22:00 bo'lganda foydalanuvchi allaqachon Instagram'da bo'lsa. */
    private val periodicCheck = object : Runnable {
        override fun run() {
            accountUsage(persist = true)
            lastPackage?.let { evaluate(it, justOpened = false) }
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> onClockChanged()
                Intent.ACTION_SCREEN_OFF -> {
                    accountUsage(persist = true)
                    screenOn = false
                }
                Intent.ACTION_SCREEN_ON -> {
                    screenOn = true
                    currentSince = SystemClock.elapsedRealtime()
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        whitelist = SystemWhitelist.packages(this)
        overlay = BlockOverlayManager(this)
        loadGuard()
        clockOffset = currentOffset()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(this, systemReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        scope.launch { routineSettingsRepository.observeSettings().collect { settings = it } }
        scope.launch { blockedAppRepository.observeBlockedPackages().collect { blockedPackages = it } }
        scope.launch { appStateRepository.bypassUntilMillis.collect { bypassUntilMillis = it } }
        scope.launch { appRuleRepository.observeRules().collect { list -> rules = list.associateBy { it.packageName } } }
        scope.launch { ensureUsageDay() }
        scope.launch {
            userPreferencesRepository.preferences.collect { prefs ->
                language = if (prefs.isLanguageChosen) prefs.language else null
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
        val justOpened = pkg != currentPkg
        if (justOpened) {
            accountUsage(persist = true)
            currentPkg = pkg
            currentSince = SystemClock.elapsedRealtime()
        }
        evaluate(pkg, justOpened)
    }

    private fun evaluate(pkg: String, justOpened: Boolean) {
        val now = timeProvider.now()
        refreshLock(now)
        val elapsed = SystemClock.elapsedRealtime()
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
            allowedPackages = allowedPackages,
            forceWindow = ClockGuard.isLocked(lockUntilElapsed, elapsed)
        )
        if (block) {
            // Bloklangan ilovadan chiqarib, bosh ekranga qaytaramiz va blok ekranini ko'rsatamiz
            performGlobalAction(GLOBAL_ACTION_HOME)
            showOverlay()
            return
        }
        if (pkg in whitelist && overlay?.shown == BlockOverlayManager.Shown.Night) {
            // Qo'ng'iroq kelsa yoki soat ochilsa — blok ekrani xalaqit bermasin
            overlay?.hide()
        }
        dayControl(pkg, justOpened, elapsed)
    }

    // ------------------------------------------------------------------
    // Kunduzgi nazorat
    // ------------------------------------------------------------------

    private fun dayControl(pkg: String, justOpened: Boolean, elapsed: Long) {
        val shownDay = (overlay?.shown as? BlockOverlayManager.Shown.Day)?.state
        val rule = rules[pkg]
        val used = usageMs[pkg] ?: 0L
        when (DayControl.decide(rule, used / 1000, pausePassed[pkg], elapsed)) {
            DayControl.Decision.LIMIT_REACHED -> {
                performGlobalAction(GLOBAL_ACTION_HOME)
                if (rule != null && (shownDay !is DayControlState.LimitReached || shownDay.packageName != pkg)) {
                    overlay?.hide()
                    overlay?.showDay(
                        DayControlState.LimitReached(pkg, rule.appName, backgroundStyle, rule.dailyLimitMinutes),
                        language,
                        onOpen = { overlay?.hide() },
                        onClose = { overlay?.hide() }
                    )
                }
            }
            DayControl.Decision.PAUSE -> {
                // Faqat ilova endi ochilganda (ichida sahifa almashganda emas)
                if (!justOpened || rule == null) return
                if (shownDay is DayControlState.Pause && shownDay.packageName == pkg) return
                performGlobalAction(GLOBAL_ACTION_HOME)
                overlay?.hide()
                overlay?.showDay(
                    DayControlState.Pause(
                        packageName = pkg,
                        appName = rule.appName,
                        backgroundStyle = backgroundStyle,
                        seconds = DayControl.PAUSE_SECONDS,
                        usedMinutes = if (rule.hasLimit) (used / 60_000).toInt() else null,
                        limitMinutes = rule.dailyLimitMinutes.takeIf { rule.hasLimit }
                    ),
                    language,
                    onOpen = {
                        pausePassed[pkg] = SystemClock.elapsedRealtime()
                        overlay?.hide()
                        openApp(pkg)
                    },
                    onClose = { overlay?.hide() }
                )
            }
            DayControl.Decision.NONE -> {
                if (rule != null) pausePassed[pkg] = elapsed      // ilova ishlatilmoqda — qaytib kirsa ham so'ralmaydi
                // Boshqa ilova ochildi — eski pauza oynasi kerak emas (bosh ekran va tizim ilovalarida qoladi)
                if (shownDay is DayControlState.Pause && shownDay.packageName != pkg && pkg !in whitelist) overlay?.hide()
            }
        }
    }

    private fun openApp(pkg: String) {
        try {
            packageManager.getLaunchIntentForPackage(pkg)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ?.let(::startActivity)
        } catch (e: Exception) {
            // ochib bo'lmasa — foydalanuvchi o'zi ochadi (pauza endi so'ralmaydi)
        }
    }

    /** Hozirgi ilovaning ochiq turgan vaqtini qo'shadi (ekran yoniq bo'lsa va qoidasi bo'lsa). */
    private fun accountUsage(persist: Boolean) {
        val pkg = currentPkg ?: return
        val now = SystemClock.elapsedRealtime()
        val since = currentSince
        currentSince = now
        if (!screenOn || pkg !in rules) return
        val today = timeProvider.today()
        if (usageDate != today) {
            scope.launch { ensureUsageDay() }
            return
        }
        val add = (now - since).coerceIn(0L, MAX_ACCOUNT_MS)
        val total = (usageMs[pkg] ?: 0L) + add
        usageMs[pkg] = total
        if (persist) scope.launch { runCatching { appRuleRepository.setUsage(today, pkg, total / 1000) } }
    }

    /** Yangi kun boshlanganda hisob noldan boshlanadi; bazadagi bugungi hisob yuklanadi. */
    private suspend fun ensureUsageDay() {
        val today = timeProvider.today()
        if (usageDate == today) return
        val loaded = runCatching { appRuleRepository.getUsage(today) }.getOrDefault(emptyMap())
        if (usageDate != today) usageMs.clear()
        loaded.forEach { (pkg, seconds) -> usageMs[pkg] = maxOf(usageMs[pkg] ?: 0L, seconds * 1000) }
        usageDate = today
        runCatching { appRuleRepository.clearUsageBefore(today) }
    }

    // ------------------------------------------------------------------
    // Soat himoyasi
    // ------------------------------------------------------------------

    private fun currentOffset(): Long = System.currentTimeMillis() - SystemClock.elapsedRealtime()

    private fun bootCount(): Int =
        runCatching { Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT) }.getOrDefault(-1)

    /** Xizmat qayta ishga tushsa ham rejim tugash vaqti yo'qolmasin (telefon o'chib-yonsa — hisoblagich noldan). */
    private fun loadGuard() {
        if (guardPrefs.getInt(KEY_BOOT, -2) != bootCount()) return
        lockUntilElapsed = guardPrefs.getLong(KEY_UNTIL, 0L)
        lockDay = guardPrefs.getString(KEY_DAY, null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    }

    /** Rejim vaqtida tugash vaqtini hisoblagichga bog'laydi. Hech qachon qisqartirilmaydi. */
    private fun refreshLock(now: LocalDateTime) {
        val remaining = ClockGuard.remainingMillis(settings, now) ?: return
        val elapsed = SystemClock.elapsedRealtime()
        val until = elapsed + remaining
        val locked = ClockGuard.isLocked(lockUntilElapsed, elapsed)
        if (locked && until <= lockUntilElapsed + LOCK_EXTEND_STEP_MS) return
        if (!locked) lockDay = settings.routineDayFor(now)
        lockUntilElapsed = maxOf(lockUntilElapsed, until)
        guardPrefs.edit()
            .putInt(KEY_BOOT, bootCount())
            .putLong(KEY_UNTIL, lockUntilElapsed)
            .putString(KEY_DAY, lockDay?.toString())
            .apply()
    }

    /**
     * Telefon soati o'zgardi. Rejim vaqtida qo'lda surilgan bo'lsa: bloklash davom etadi,
     * favqulodda ruxsat bekor qilinadi va Muhosabaga ✗ yoziladi (bir kechada bir marta).
     */
    private fun onClockChanged() {
        val newOffset = currentOffset()
        val tampered = ClockGuard.isTampered(clockOffset, newOffset)
        clockOffset = newOffset
        if (tampered && ClockGuard.isLocked(lockUntilElapsed, SystemClock.elapsedRealtime())) {
            bypassUntilMillis = 0L
            val day = lockDay
            scope.launch {
                runCatching { appStateRepository.setBypassUntilMillis(0L) }
                if (day != null && guardPrefs.getString(KEY_TAMPER_DAY, null) != day.toString()) {
                    guardPrefs.edit().putString(KEY_TAMPER_DAY, day.toString()).apply()
                    runCatching { scoreRepository.registerBypass(day) }
                }
            }
        }
        lastPackage?.let { evaluate(it, justOpened = false) }
    }

    // ------------------------------------------------------------------

    private fun showOverlay() {
        if (overlay?.shown is BlockOverlayManager.Shown.Day) overlay?.hide()
        overlay?.show(
            overlayState = BlockOverlayState(
                unlockTime = settings.wakeTime,
                streakDays = streakDays,
                messageSeed = Random.nextInt(0, 1_000),
                // Blok ekrani ilovada tanlangan rangda chiqadi
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

    private var cleanedUp = false

    private fun cleanup() {
        if (cleanedUp) return
        cleanedUp = true
        accountUsage(persist = true)
        handler.removeCallbacks(periodicCheck)
        runCatching { unregisterReceiver(systemReceiver) }
        overlay?.hide()
        scope.cancel()
    }

    private companion object {
        const val CHECK_INTERVAL_MS = 15_000L
        const val DAY_CHECK_MS = 60_000L
        const val LAUNCHABLE_REFRESH_MS = 10 * 60_000L
        /** Bir hisobda ko'pi bilan shuncha qo'shiladi (xizmat uxlab qolgan bo'lsa, ortiqcha yozilmasin). */
        const val MAX_ACCOUNT_MS = 2 * 60_000L
        const val LOCK_EXTEND_STEP_MS = 60_000L
        const val GUARD_PREFS = "clock_guard"
        const val KEY_BOOT = "boot_count"
        const val KEY_UNTIL = "lock_until_elapsed"
        const val KEY_DAY = "lock_day"
        const val KEY_TAMPER_DAY = "tamper_day"
        val IGNORED_PACKAGES = setOf(
            "com.android.systemui",
            "com.google.android.inputmethod.latin",
            "com.samsung.android.honeyboard",
            "com.touchtype.swiftkey",
            "com.miui.home.launcher.assistant"
        )
    }
}
