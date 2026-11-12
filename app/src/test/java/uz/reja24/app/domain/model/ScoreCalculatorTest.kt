package uz.reja24.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ScoreCalculatorTest {

    private val date = LocalDate.of(2026, 9, 24)

    private fun calc(
        completed: Int = 4,
        total: Int = 4,
        active: Boolean = true,
        bypass: Int = 0,
        usedAfterSleep: Boolean = false,
        woke: Boolean = true
    ) = ScoreCalculator.calculate(date, completed, total, active, bypass, usedAfterSleep, woke)

    @Test
    fun `perfect day gives 100 points and 5 stars`() {
        val score = calc()
        assertEquals(100, score.points)
        assertEquals(5, score.starsEarned)
        assertTrue(score.isPerfect)
        assertTrue(score.routineKept)
    }

    @Test
    fun `task points are proportional and rounded`() {
        assertEquals(0, ScoreCalculator.taskPoints(0, 4))
        assertEquals(13, ScoreCalculator.taskPoints(1, 4))   // 12.5 → 13
        assertEquals(25, ScoreCalculator.taskPoints(2, 4))
        assertEquals(17, ScoreCalculator.taskPoints(1, 3))   // 16.67 → 17
        assertEquals(50, ScoreCalculator.taskPoints(3, 3))
    }

    @Test
    fun `no planned tasks give zero task points`() {
        assertEquals(0, ScoreCalculator.taskPoints(0, 0))
        assertEquals(50, calc(completed = 0, total = 0).points)
    }

    @Test
    fun `completed more than total is clamped`() {
        assertEquals(50, ScoreCalculator.taskPoints(9, 3))
        assertEquals(3, calc(completed = 9, total = 3).completedTasks)
    }

    @Test
    fun `each bypass removes 30 routine points and breaks routine`() {
        val one = calc(bypass = 1)
        assertEquals(70, one.points)
        assertFalse(one.routineKept)
        assertEquals(3, one.starsEarned)   // 70 ball → 3 yulduz

        // Rejim bali 0 dan pastga tushmaydi
        assertEquals(70, calc(bypass = 3).points)
    }

    @Test
    fun `inactive routine gives no routine and sleep points`() {
        val score = calc(active = false)
        assertEquals(50 + 10, score.points)
        assertFalse(score.routineKept)
        assertFalse(score.sleptOnTime)
    }

    @Test
    fun `phone after bedtime and late wake up lose 10 each`() {
        assertEquals(90, calc(usedAfterSleep = true).points)
        assertEquals(90, calc(woke = false).points)
        assertEquals(80, calc(usedAfterSleep = true, woke = false).points)
    }

    @Test
    fun `star thresholds at every boundary`() {
        mapOf(
            100 to 5, 90 to 5, 89 to 4, 75 to 4, 74 to 3, 60 to 3,
            59 to 2, 40 to 2, 39 to 1, 20 to 1, 19 to 0, 0 to 0
        ).forEach { (points, stars) ->
            assertEquals("$points ball", stars, ScoreCalculator.starsFor(points))
        }
    }
}
