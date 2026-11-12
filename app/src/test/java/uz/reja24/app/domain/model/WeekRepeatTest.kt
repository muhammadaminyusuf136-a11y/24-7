package uz.reja24.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class WeekRepeatTest {

    private val monday = LocalDate.of(2026, 10, 26)

    private fun once(id: Long, title: String, date: LocalDate, from: Int, to: Int) = ScheduleEntry(
        id = id, title = title, startTime = LocalTime.of(from, 0), endTime = LocalTime.of(to, 0),
        repeatType = RepeatType.ONCE, specificDate = date
    )

    @Test
    fun once_entries_become_weekly_on_their_own_weekday() {
        val week = listOf(
            once(1, "Ish", monday, 9, 13),
            once(2, "Dars", monday.plusDays(1), 14, 16),
            once(3, "Sport", monday.plusDays(6), 7, 8)
        )
        val plan = WeekRepeat.plan(week, week)
        assertEquals(3, plan.converted.size)
        assertTrue(plan.converted.all { it.repeatType == RepeatType.WEEKLY })
        assertEquals(setOf(DayOfWeek.MONDAY), plan.converted.first { it.id == 1L }.daysOfWeek)
        assertEquals(setOf(DayOfWeek.SUNDAY), plan.converted.first { it.id == 3L }.daysOfWeek)
        // Keyingi haftalarda ko'rinadi, oldingi haftalarda — yo'q
        val ish = plan.converted.first { it.id == 1L }
        assertTrue(ish.occursOn(monday.plusWeeks(1)))
        assertTrue(ish.occursOn(monday.plusWeeks(5)))
        assertTrue(!ish.occursOn(monday.minusWeeks(1)))
        assertTrue(!ish.occursOn(monday.plusDays(1)))
        assertTrue(plan.duplicatesToDelete.isEmpty())
    }

    @Test
    fun later_manual_copies_are_removed_but_different_plans_stay() {
        val source = once(1, "Ish", monday, 9, 13)
        val copyNextWeek = once(10, " ish ", monday.plusWeeks(1), 9, 13)
        val otherTime = once(11, "Ish", monday.plusWeeks(1), 10, 13)
        val otherDay = once(12, "Ish", monday.plusWeeks(1).plusDays(1), 9, 13)
        val earlier = once(13, "Ish", monday.minusWeeks(1), 9, 13)
        val plan = WeekRepeat.plan(listOf(source), listOf(source, copyNextWeek, otherTime, otherDay, earlier))
        assertEquals(listOf(10L), plan.duplicatesToDelete)
    }

    @Test
    fun only_once_entries_are_collected_from_the_week() {
        val weekly = ScheduleEntry(
            id = 5, title = "Namoz", startTime = LocalTime.of(12, 0), endTime = LocalTime.of(12, 20),
            repeatType = RepeatType.DAILY
        )
        val tasks = listOf(
            DailyTask(once(1, "Ish", monday, 9, 13), monday, false),
            DailyTask(weekly, monday, false),
            DailyTask(weekly, monday.plusDays(1), false)
        )
        assertEquals(listOf(1L), WeekRepeat.onceEntries(tasks).map { it.id })
    }
}
