package uz.reja24.app.domain.model

import java.time.LocalDate

/** Kunlik baho. */
data class DailyScore(
    val date: LocalDate,
    val points: Int = 0,              // 0–100
    val starsEarned: Int = 0,         // 0–5
    val completedTasks: Int = 0,
    val totalTasks: Int = 0,
    val routineKept: Boolean = true,  // blok buzilmaganmi
    val sleptOnTime: Boolean = false,
    val wokeOnTime: Boolean = false,
    val bypassCount: Int = 0,         // favqulodda kirishlar soni
    val usedPhoneAfterSleep: Boolean = false // uyqu vaqtidan keyin telefon ochilganmi
) {
    /** Ball va yulduz — "mukammal kun" (haftalik kubok sharti). */
    val isPerfect: Boolean get() = points == MAX_POINTS && starsEarned == MAX_STARS

    companion object {
        const val MAX_POINTS = 100
        const val MAX_STARS = 5
    }
}

/** Kubok darajalari. requiredPerfectWeeks — nechta mukammal hafta kerak. */
enum class TrophyLevel(val requiredPerfectWeeks: Int) {
    NONE(0),
    BRONZE(1),
    SILVER(4),
    GOLD(12),
    DIAMOND(52);

    companion object {
        fun fromName(name: String): TrophyLevel = entries.firstOrNull { it.name == name } ?: NONE

        /** Jami mukammal haftalar soniga qarab erishilgan eng yuqori daraja. */
        fun forPerfectWeeks(count: Int): TrophyLevel =
            entries.lastOrNull { it != NONE && count >= it.requiredPerfectWeeks } ?: NONE
    }
}

/** Haftalik natija (mukammal bo'lmagan hafta uchun ham saqlanadi — xulosa ko'rsatish uchun). */
data class WeeklyAward(
    val weekStartDate: LocalDate,     // haftaning dushanbasi
    val totalPoints: Int,             // 0–700
    val averageStars: Float,          // 0.0–5.0
    val isPerfectWeek: Boolean,
    val trophyLevel: TrophyLevel      // shu hafta oxirida erishilgan daraja
)

/** Ketma-ketlik yozuvi: shu kuni rejim saqlanganmi. */
data class StreakRecord(
    val date: LocalDate,
    val wasSuccessful: Boolean
)

/** Streak xulosasi: joriy va eng uzun. */
data class StreakSummary(
    val current: Int,
    val longest: Int
)
