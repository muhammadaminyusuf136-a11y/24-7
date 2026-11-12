package uz.reja24.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeeklyEvaluatorTest {

    private val monday = LocalDate.of(2026, 9, 21)

    private fun week(vararg points: Int) = points.mapIndexed { i, p ->
        DailyScore(date = monday.plusDays(i.toLong()), points = p, starsEarned = ScoreCalculator.starsFor(p))
    }

    @Test
    fun `week start is monday`() {
        assertEquals(monday, WeeklyEvaluator.weekStartOf(monday))
        assertEquals(monday, WeeklyEvaluator.weekStartOf(monday.plusDays(6)))   // yakshanba
        assertEquals(monday.plusDays(7), WeeklyEvaluator.weekStartOf(monday.plusDays(7)))
    }

    @Test
    fun `seven perfect days earn bronze on first perfect week`() {
        val award = WeeklyEvaluator.evaluate(monday, week(100, 100, 100, 100, 100, 100, 100), 0)
        assertTrue(award.isPerfectWeek)
        assertEquals(700, award.totalPoints)
        assertEquals(5f, award.averageStars)
        assertEquals(TrophyLevel.BRONZE, award.trophyLevel)
    }

    @Test
    fun `one imperfect day breaks the perfect week but keeps summary`() {
        val award = WeeklyEvaluator.evaluate(monday, week(100, 100, 99, 100, 100, 100, 100), 3)
        assertFalse(award.isPerfectWeek)
        assertEquals(699, award.totalPoints)
        assertEquals(TrophyLevel.BRONZE, award.trophyLevel) // oldingi 3 ta mukammal hafta
    }

    @Test
    fun `missing days count as zero`() {
        val award = WeeklyEvaluator.evaluate(monday, week(100, 100, 100), 0)
        assertFalse(award.isPerfectWeek)
        assertEquals(300, award.totalPoints)
        assertEquals(15f / 7f, award.averageStars, 0.001f)
    }

    @Test
    fun `scores outside the week are ignored`() {
        val outside = DailyScore(date = monday.minusDays(1), points = 100, starsEarned = 5)
        val award = WeeklyEvaluator.evaluate(monday, week(100) + outside, 0)
        assertEquals(100, award.totalPoints)
    }

    @Test
    fun `fourth perfect week reaches silver`() {
        val award = WeeklyEvaluator.evaluate(monday, week(100, 100, 100, 100, 100, 100, 100), 3)
        assertEquals(TrophyLevel.SILVER, award.trophyLevel)
    }
}
