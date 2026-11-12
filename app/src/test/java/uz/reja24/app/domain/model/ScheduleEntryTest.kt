package uz.reja24.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class ScheduleEntryTest {

    private fun entry(
        repeatType: RepeatType,
        specificDate: LocalDate? = null,
        daysOfWeek: Set<DayOfWeek> = emptySet(),
        isActive: Boolean = true,
        start: LocalTime = LocalTime.of(9, 0),
        end: LocalTime = LocalTime.of(10, 0)
    ) = ScheduleEntry(
        title = "Test",
        startTime = start,
        endTime = end,
        repeatType = repeatType,
        specificDate = specificDate,
        daysOfWeek = daysOfWeek,
        isActive = isActive
    )

    // 2026-09-21 — dushanba
    private val monday = LocalDate.of(2026, 9, 21)

    @Test
    fun `once occurs only on its date`() {
        val e = entry(RepeatType.ONCE, specificDate = monday)
        assertTrue(e.occursOn(monday))
        assertFalse(e.occursOn(monday.plusDays(1)))
        assertFalse(e.occursOn(monday.minusDays(1)))
    }

    @Test
    fun `once without date never occurs`() {
        assertFalse(entry(RepeatType.ONCE).occursOn(monday))
    }

    @Test
    fun `inactive entry never occurs`() {
        assertFalse(entry(RepeatType.DAILY, isActive = false).occursOn(monday))
    }

    @Test
    fun `daily respects start date`() {
        val e = entry(RepeatType.DAILY, specificDate = monday)
        assertFalse(e.occursOn(monday.minusDays(1)))
        assertTrue(e.occursOn(monday))
        assertTrue(e.occursOn(monday.plusDays(100)))
    }

    @Test
    fun `weekdays are monday to friday only`() {
        val e = entry(RepeatType.WEEKDAYS)
        (0L..4L).forEach { assertTrue(e.occursOn(monday.plusDays(it))) }
        assertFalse(e.occursOn(monday.plusDays(5))) // shanba
        assertFalse(e.occursOn(monday.plusDays(6))) // yakshanba
    }

    @Test
    fun `weekly occurs on selected days`() {
        val e = entry(RepeatType.WEEKLY, daysOfWeek = setOf(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY))
        assertFalse(e.occursOn(monday))
        assertTrue(e.occursOn(monday.plusDays(1)))
        assertTrue(e.occursOn(monday.plusDays(5)))
        assertTrue(e.occursOn(monday.plusDays(8)))
    }

    @Test
    fun `monthly on 31st falls back to last day of short month`() {
        val e = entry(RepeatType.MONTHLY, specificDate = LocalDate.of(2026, 1, 31))
        assertTrue(e.occursOn(LocalDate.of(2026, 1, 31)))
        assertTrue(e.occursOn(LocalDate.of(2026, 2, 28)))
        assertTrue(e.occursOn(LocalDate.of(2026, 4, 30)))
        assertFalse(e.occursOn(LocalDate.of(2026, 4, 29)))
        assertTrue(e.occursOn(LocalDate.of(2026, 5, 31)))
        assertFalse(e.occursOn(LocalDate.of(2026, 5, 30)))
    }

    @Test
    fun `monthly does not occur before anchor`() {
        val e = entry(RepeatType.MONTHLY, specificDate = LocalDate.of(2026, 3, 15))
        assertFalse(e.occursOn(LocalDate.of(2026, 2, 15)))
        assertTrue(e.occursOn(LocalDate.of(2026, 3, 15)))
    }

    @Test
    fun `yearly on feb 29 falls back to feb 28 in non leap year`() {
        val e = entry(RepeatType.YEARLY, specificDate = LocalDate.of(2028, 2, 29))
        assertTrue(e.occursOn(LocalDate.of(2028, 2, 29)))
        assertTrue(e.occursOn(LocalDate.of(2029, 2, 28)))
        assertTrue(e.occursOn(LocalDate.of(2032, 2, 29)))
        assertFalse(e.occursOn(LocalDate.of(2032, 2, 28)))
        assertFalse(e.occursOn(LocalDate.of(2029, 3, 1)))
    }

    @Test
    fun `duration handles crossing midnight`() {
        val sleep = entry(RepeatType.DAILY, start = LocalTime.of(23, 0), end = LocalTime.of(6, 0))
        assertTrue(sleep.crossesMidnight)
        assertEquals(7 * 60, sleep.durationMinutes)

        val work = entry(RepeatType.DAILY, start = LocalTime.of(9, 0), end = LocalTime.of(17, 30))
        assertFalse(work.crossesMidnight)
        assertEquals(8 * 60 + 30, work.durationMinutes)
    }
}
