package uz.reja24.app.domain.model

import java.time.LocalDate

/**
 * Streak (ketma-ketlik) hisoblash — sof mantiq, testlanadi.
 */
object StreakCalculator {

    /**
     * @param records istalgan tartibdagi yozuvlar
     * @param today bugungi sana
     *
     * Joriy streak: bugundan (yoki bugun hali yozilmagan bo'lsa — kechadan) orqaga
     * uzluksiz muvaffaqiyatli kunlar soni. Oradagi yozuvsiz kun ketma-ketlikni uzadi.
     */
    fun calculate(records: List<StreakRecord>, today: LocalDate): StreakSummary {
        if (records.isEmpty()) return StreakSummary(0, 0)
        val byDate = records.associateBy { it.date }

        // Joriy streak
        var cursor = if (byDate.containsKey(today)) today else today.minusDays(1)
        var current = 0
        while (byDate[cursor]?.wasSuccessful == true) {
            current++
            cursor = cursor.minusDays(1)
        }

        // Eng uzun streak
        var longest = 0
        var run = 0
        var previous: LocalDate? = null
        for (record in records.sortedBy { it.date }) {
            run = when {
                !record.wasSuccessful -> 0
                previous != null && record.date == previous.plusDays(1) && run > 0 -> run + 1
                else -> 1
            }
            longest = maxOf(longest, run)
            previous = record.date
        }
        return StreakSummary(current = current, longest = maxOf(longest, current))
    }
}
