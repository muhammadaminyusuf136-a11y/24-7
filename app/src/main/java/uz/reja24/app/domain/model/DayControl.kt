package uz.reja24.app.domain.model

import uz.reja24.app.domain.repository.AppRule

/**
 * Kunduzgi nazorat qarori (sof mantiq — testlanadi):
 *  - kunlik chegara tugagan bo'lsa — ilova yopiladi ("bugungi vaqt tugadi");
 *  - pauza yoqilgan bo'lsa — ochishdan oldin "o'ylab ko'r" oynasi (yaqinda o'tilgan bo'lsa — yo'q).
 */
object DayControl {

    /** "O'ylab ko'r" pauzasi necha soniya. */
    const val PAUSE_SECONDS = 10

    /** Pauzadan o'tib ilova ochilgach, shu vaqt ichida qaytib kirsa — qayta so'ralmaydi. */
    const val PAUSE_GRACE_MS = 5 * 60_000L

    enum class Decision { NONE, LIMIT_REACHED, PAUSE }

    fun decide(
        rule: AppRule?,
        usedSeconds: Long,
        /** Pauzadan oxirgi marta qachon o'tilgan (hisoblagich bo'yicha), bo'lmasa null. */
        pausePassedAt: Long?,
        elapsedNow: Long
    ): Decision {
        if (rule == null) return Decision.NONE
        if (rule.hasLimit && usedSeconds >= rule.dailyLimitMinutes * 60L) return Decision.LIMIT_REACHED
        if (rule.pauseOn) {
            val recentlyPassed = pausePassedAt != null && elapsedNow - pausePassedAt in 0 until PAUSE_GRACE_MS
            if (!recentlyPassed) return Decision.PAUSE
        }
        return Decision.NONE
    }
}
