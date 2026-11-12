package uz.reja24.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {

    private val today = LocalDate.of(2026, 9, 24)

    private fun ok(daysAgo: Long) = StreakRecord(today.minusDays(daysAgo), true)
    private fun fail(daysAgo: Long) = StreakRecord(today.minusDays(daysAgo), false)

    @Test
    fun `empty records give zero`() {
        assertEquals(StreakSummary(0, 0), StreakCalculator.calculate(emptyList(), today))
    }

    @Test
    fun `counts consecutive days ending today`() {
        val r = listOf(ok(0), ok(1), ok(2), fail(3), ok(4))
        assertEquals(StreakSummary(current = 3, longest = 3), StreakCalculator.calculate(r, today))
    }

    @Test
    fun `today not yet recorded continues from yesterday`() {
        val r = listOf(ok(1), ok(2))
        assertEquals(2, StreakCalculator.calculate(r, today).current)
    }

    @Test
    fun `failed today resets current`() {
        val r = listOf(fail(0), ok(1), ok(2))
        assertEquals(0, StreakCalculator.calculate(r, today).current)
        assertEquals(2, StreakCalculator.calculate(r, today).longest)
    }

    @Test
    fun `gap without record breaks streak`() {
        val r = listOf(ok(0), ok(1), ok(3), ok(4), ok(5), ok(6))
        val summary = StreakCalculator.calculate(r, today)
        assertEquals(2, summary.current)
        assertEquals(4, summary.longest)
    }

    @Test
    fun `order of records does not matter`() {
        val r = listOf(ok(2), ok(0), ok(1))
        assertEquals(StreakSummary(3, 3), StreakCalculator.calculate(r, today))
    }
}
