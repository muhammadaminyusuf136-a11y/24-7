package uz.reja24.app.domain.model

import java.time.LocalTime

/** Ilova hozir bloklanishi kerakmi — yagona qoida (bloklash xizmati va testlar shuni ishlatadi). */
object BlockPolicy {

    /** Favqulodda kirishdan keyin beriladigan vaqt. */
    const val BYPASS_DURATION_MINUTES = 5L

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
        allowedPackages: Set<String> = emptySet()
    ): Boolean {
        if (packageName.isBlank()) return false
        if (packageName in whitelist) return false          // qo'ng'iroq, SMS, soat, sozlamalar — hech qachon
        val listed = packageName in blockedPackages
        val notAllowed = allowOnly && packageName in launchablePackages && packageName !in allowedPackages
        if (!listed && !notAllowed) return false
        if (!settings.isInBlockWindow(time)) return false
        return nowMillis >= bypassUntilMillis                 // favqulodda ruxsat muddati tugaganmi
    }
}
