package uz.reja24.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TrophyLevelTest {

    @Test
    fun `trophy thresholds`() {
        assertEquals(TrophyLevel.NONE, TrophyLevel.forPerfectWeeks(0))
        assertEquals(TrophyLevel.BRONZE, TrophyLevel.forPerfectWeeks(1))
        assertEquals(TrophyLevel.BRONZE, TrophyLevel.forPerfectWeeks(3))
        assertEquals(TrophyLevel.SILVER, TrophyLevel.forPerfectWeeks(4))
        assertEquals(TrophyLevel.SILVER, TrophyLevel.forPerfectWeeks(11))
        assertEquals(TrophyLevel.GOLD, TrophyLevel.forPerfectWeeks(12))
        assertEquals(TrophyLevel.GOLD, TrophyLevel.forPerfectWeeks(51))
        assertEquals(TrophyLevel.DIAMOND, TrophyLevel.forPerfectWeeks(52))
        assertEquals(TrophyLevel.DIAMOND, TrophyLevel.forPerfectWeeks(500))
    }

    @Test
    fun `unknown stored name falls back to none`() {
        assertEquals(TrophyLevel.NONE, TrophyLevel.fromName("PLATINUM"))
    }

    @Test
    fun `perfect day requires 100 points and 5 stars`() {
        val date = LocalDate.of(2026, 9, 24)
        assertTrue(DailyScore(date, points = 100, starsEarned = 5).isPerfect)
        assertFalse(DailyScore(date, points = 99, starsEarned = 5).isPerfect)
        assertFalse(DailyScore(date, points = 100, starsEarned = 4).isPerfect)
    }
}
