package uz.reja24.app.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Kunlik ball hisoblash (Bosqich 7).
 *
 * | Mezon                              | Ball |
 * | Rejalarning bajarilishi            | 50 (bajarilgan ÷ jami × 50) |
 * | Rejim saqlangani (blok buzilmagan) | 30 (har favqulodda kirish −30) |
 * | Vaqtida uyquga yotish              | 10 |
 * | Vaqtida uyg'onish                  | 10 |
 */
object ScoreCalculator {

    const val TASKS_MAX = 50
    const val ROUTINE_MAX = 30
    const val BYPASS_PENALTY = 30
    const val SLEEP_MAX = 10
    const val WAKE_MAX = 10

    /** Faqat reja bajarilishidan kelgan ball. Rejasiz kun — 0 ball (rejalashtirish rag'batlanadi). */
    fun taskPoints(completed: Int, total: Int): Int {
        if (total <= 0) return 0
        val safeCompleted = completed.coerceIn(0, total)
        return (TASKS_MAX * safeCompleted.toDouble() / total).roundToInt()
    }

    fun routinePoints(isRoutineActive: Boolean, bypassCount: Int): Int {
        if (!isRoutineActive) return 0
        return (ROUTINE_MAX - BYPASS_PENALTY * bypassCount.coerceAtLeast(0)).coerceAtLeast(0)
    }

    fun starsFor(points: Int): Int = when {
        points >= 90 -> 5
        points >= 75 -> 4
        points >= 60 -> 3
        points >= 40 -> 2
        points >= 20 -> 1
        else -> 0
    }

    fun calculate(
        date: LocalDate,
        completedTasks: Int,
        totalTasks: Int,
        isRoutineActive: Boolean,
        bypassCount: Int,
        usedPhoneAfterSleep: Boolean,
        wokeOnTime: Boolean
    ): DailyScore {
        val sleptOnTime = isRoutineActive && !usedPhoneAfterSleep
        val points = (
            taskPoints(completedTasks, totalTasks) +
                routinePoints(isRoutineActive, bypassCount) +
                (if (sleptOnTime) SLEEP_MAX else 0) +
                (if (wokeOnTime) WAKE_MAX else 0)
            ).coerceIn(0, DailyScore.MAX_POINTS)
        return DailyScore(
            date = date,
            points = points,
            starsEarned = starsFor(points),
            completedTasks = completedTasks.coerceIn(0, totalTasks.coerceAtLeast(0)),
            totalTasks = totalTasks.coerceAtLeast(0),
            routineKept = isRoutineActive && bypassCount <= 0,
            sleptOnTime = sleptOnTime,
            wokeOnTime = wokeOnTime,
            bypassCount = bypassCount.coerceAtLeast(0),
            usedPhoneAfterSleep = usedPhoneAfterSleep
        )
    }
}

/** Haftalik xulosa va kubok (yakshanba kechasi). */
object WeeklyEvaluator {

    const val DAYS_IN_WEEK = 7
    const val MAX_WEEK_POINTS = DailyScore.MAX_POINTS * DAYS_IN_WEEK

    /** Berilgan kun joylashgan haftaning dushanbasi. */
    fun weekStartOf(date: LocalDate): LocalDate =
        date.minusDays((date.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())

    /**
     * @param scores haftadagi kunlik baholar (yo'q kunlar 0 ball hisoblanadi)
     * @param previousPerfectWeeks shu haftagacha bo'lgan mukammal haftalar soni
     */
    fun evaluate(
        weekStart: LocalDate,
        scores: List<DailyScore>,
        previousPerfectWeeks: Int
    ): WeeklyAward {
        val weekEnd = weekStart.plusDays(DAYS_IN_WEEK - 1L)
        val inWeek = scores.filter { !it.date.isBefore(weekStart) && !it.date.isAfter(weekEnd) }
            .associateBy { it.date }
            .values
        val totalPoints = inWeek.sumOf { it.points }
        val averageStars = inWeek.sumOf { it.starsEarned } / DAYS_IN_WEEK.toFloat()
        val isPerfect = inWeek.size == DAYS_IN_WEEK && inWeek.all { it.isPerfect }
        val perfectCount = previousPerfectWeeks.coerceAtLeast(0) + if (isPerfect) 1 else 0
        return WeeklyAward(
            weekStartDate = weekStart,
            totalPoints = totalPoints,
            averageStars = averageStars,
            isPerfectWeek = isPerfect,
            trophyLevel = TrophyLevel.forPerfectWeeks(perfectCount)
        )
    }
}
