package uz.reja24.app.domain.model

import java.time.LocalDateTime
import java.time.LocalTime

/** Ilova hozir bloklanishi kerakmi — yagona qoida (bloklash xizmati va testlar shuni ishlatadi). */
object BlockPolicy {

    /** Favqulodda kirishdan keyin beriladigan vaqt. */
    const val BYPASS_DURATION_MINUTES = 5L

    /**
     * "Qattiq rejim"da tungi rejim vaqtida bular ham yopiladi: Sozlamalar (bloklash xizmatini o'chirib
     * qo'ymaslik uchun), ilova o'chirish oynasi va Play Market. Qo'ng'iroq, SMS, soat va yuqoridan
     * tushadigan tez sozlamalar paneli (Wi-Fi, ovoz) ochiq qoladi.
     */
    val STRICT_PROTECTED_PACKAGES: Set<String> = setOf(
        "com.android.settings",
        "com.miui.securitycenter",
        "com.huawei.systemmanager",
        "com.samsung.android.lool",
        "com.coloros.safecenter",
        "com.oplus.safecenter",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.vending"
    )

    fun shouldBlock(
        packageName: String,
        time: LocalTime,
        nowMillis: Long,
        settings: RoutineSettings,
        blockedPackages: Set<String>,
        whitelist: Set<String>,
        bypassUntilMillis: Long,
        /** Tungi "faqat kerakli" rejim: ruxsat etilganlardan boshqa barcha ilovalar yopiladi. */
        allowOnly: Boolean = false,
        /** Bosh ekranda ko'rinadigan (ochsa bo'ladigan) ilovalar — tizim qismlari bunga kirmaydi. */
        launchablePackages: Set<String> = emptySet(),
        allowedPackages: Set<String> = emptySet(),
        /** Soat surilgan bo'lsa ham rejim davom etyapti (ClockGuard). */
        forceWindow: Boolean = false
    ): Boolean {
        if (packageName.isBlank()) return false
        val strictProtected = settings.isStrictModeOn && packageName in STRICT_PROTECTED_PACKAGES
        // qo'ng'iroq, SMS, soat — hech qachon; sozlamalar — faqat qattiq rejimda yopiladi
        if (packageName in whitelist && !strictProtected) return false
        val listed = packageName in blockedPackages
        val notAllowed = allowOnly && packageName in launchablePackages && packageName !in allowedPackages
        if (!listed && !notAllowed && !strictProtected) return false
        val inWindow = settings.isInBlockWindow(time) || (forceWindow && settings.isRoutineActive)
        if (!inWindow) return false
        return nowMillis >= bypassUntilMillis                 // favqulodda ruxsat muddati tugaganmi
    }
}

/**
 * Soatni surib qo'yishdan himoya. Rejim boshlanganda uning tugash vaqti telefon soatiga emas,
 * telefon yoqilgandan beri o'tgan vaqtga (o'zgartirib bo'lmaydigan hisoblagich) bog'lab qo'yiladi.
 * Shunda soat 06:00 ga surilsa ham bloklash haqiqiy uyg'onish vaqtigacha davom etadi.
 */
object ClockGuard {

    /** Soat shundan ko'p surilsa — qo'lda o'zgartirilgan deb hisoblanadi (internetdan to'g'rilash bir necha soniya). */
    const val TAMPER_THRESHOLD_MS = 2 * 60_000L

    /** Rejim oynasi ichida bo'lsa — tugashigacha qolgan millisekund, aks holda null. */
    fun remainingMillis(settings: RoutineSettings, now: LocalDateTime): Long? {
        if (!settings.isInBlockWindow(now.toLocalTime())) return null
        val nowSeconds = now.toLocalTime().toSecondOfDay()
        val wakeSeconds = settings.wakeTime.toSecondOfDay()
        val left = Math.floorMod(wakeSeconds - nowSeconds, 24 * 3600)
        return left * 1000L
    }

    /** Hisoblagich bo'yicha rejim hali tugamaganmi. */
    fun isLocked(lockUntilElapsed: Long, elapsedNow: Long): Boolean = elapsedNow < lockUntilElapsed

    /**
     * Soat qo'lda surildimi: telefon soati va hisoblagich orasidagi farq qanchaga o'zgargan.
     * offset = soat (millis) − hisoblagich (millis).
     */
    fun isTampered(oldOffset: Long, newOffset: Long): Boolean =
        kotlin.math.abs(newOffset - oldOffset) > TAMPER_THRESHOLD_MS
}
