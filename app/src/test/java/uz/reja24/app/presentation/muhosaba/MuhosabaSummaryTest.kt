package uz.reja24.app.presentation.muhosaba

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uz.reja24.app.domain.model.DailyTask
import uz.reja24.app.domain.model.ScheduleEntry
import uz.reja24.app.domain.repository.MuhosabaItem
import java.time.LocalDate
import java.time.LocalTime

class MuhosabaSummaryTest {

    private val today = LocalDate.of(2026, 11, 12)
    private val quran = MuhosabaItem(1, "Qur'on")
    private val prayer = MuhosabaItem(2, "Namoz")

    private val revit = ScheduleEntry(id = 10, title = "Revit", startTime = LocalTime.of(11, 0), endTime = LocalTime.of(13, 0))
    private val sport = ScheduleEntry(id = 11, title = "Sport", startTime = LocalTime.of(6, 0), endTime = LocalTime.of(7, 0))

    private fun day(date: LocalDate, vararg done: Pair<ScheduleEntry, Boolean>) =
        date to done.map { (entry, completed) -> DailyTask(entry, date, completed) }

    @Test
    fun plan_marks_follow_completion_and_misses_and_are_time_ordered() {
        val tasks = listOf(DailyTask(revit, today, false), DailyTask(sport, today, true))
        val marks = MuhosabaSummary.planMarks(tasks, setOf(10L))
        assertEquals(listOf("Sport", "Revit"), marks.map { it.task.entry.title })
        assertEquals(true, marks[0].mark)
        assertEquals(false, marks[1].mark)
        assertNull(MuhosabaSummary.planMarks(listOf(DailyTask(revit, today, false)), emptySet()).single().mark)
    }

    @Test
    fun plan_summary_counts_scheduled_done_and_missed_days_in_period() {
        val tasks = mapOf(
            day(today, revit to true, sport to false),
            day(today.minusDays(1), revit to false, sport to true),
            day(today.minusDays(2), revit to false),
            // 7 kunlik davrdan tashqarida
            day(today.minusDays(8), revit to true)
        )
        val misses = mapOf(today to setOf(11L), today.minusDays(1) to setOf(10L))
        val week = MuhosabaSummary.summarizePlans(tasks, misses, today, 7).associateBy { it.title }
        assertEquals(PlanSummary(10, "Revit", revit.colorHex, done = 1, missed = 1, scheduled = 3), week["Revit"])
        assertEquals(PlanSummary(11, "Sport", sport.colorHex, done = 1, missed = 1, scheduled = 2), week["Sport"])
        assertEquals(4, MuhosabaSummary.summarizePlans(tasks, misses, today, 30).first { it.title == "Revit" }.scheduled)
    }

    @Test
    fun ledger_has_one_row_per_day_oldest_first_with_plans_and_items() {
        val tasks = mapOf(day(today, revit to true, sport to false))
        val misses = mapOf(today to setOf(11L))
        val marks = mapOf(today to mapOf(1L to true, 2L to false), today.minusDays(1) to mapOf(1L to true))
        val ledger = MuhosabaSummary.ledger(tasks, misses, listOf(quran, prayer), marks, today, 7)
        assertEquals(7, ledger.size)
        assertEquals(today, ledger.last().date)
        assertEquals(DayLedger(today, done = 2, missed = 2, total = 4), ledger.last())
        assertEquals(DayLedger(today.minusDays(1), done = 1, missed = 0, total = 2), ledger[5])
    }

    @Test
    fun counts_done_and_missed_days_inside_the_period_only() {
        val marks = mapOf(
            today to mapOf(1L to true, 2L to true),
            today.minusDays(1) to mapOf(1L to false, 2L to true),
            today.minusDays(6) to mapOf(1L to true),
            // 7 kunlik davrdan tashqarida
            today.minusDays(7) to mapOf(1L to true, 2L to false),
            today.minusDays(20) to mapOf(2L to false)
        )
        val week = MuhosabaSummary.summarize(listOf(quran, prayer), marks, today, 7)
        assertEquals(ItemSummary(quran, done = 2, missed = 1, days = 7), week[0])
        assertEquals(ItemSummary(prayer, done = 2, missed = 0, days = 7), week[1])
        val month = MuhosabaSummary.summarize(listOf(quran, prayer), marks, today, 30)
        assertEquals(3, month[0].done)
        assertEquals(2, month[1].missed)
    }

    @Test
    fun item_without_marks_has_zeroes() {
        val result = MuhosabaSummary.summarize(listOf(quran), emptyMap(), today, 7).single()
        assertEquals(0, result.done)
        assertEquals(0, result.missed)
    }
}