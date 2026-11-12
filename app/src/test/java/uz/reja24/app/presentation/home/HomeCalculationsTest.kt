package uz.reja24.app.presentation.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uz.reja24.app.domain.model.DailyTask
import uz.reja24.app.domain.model.RoutineSettings
import uz.reja24.app.domain.model.ScheduleEntry
import java.time.LocalDate
import java.time.LocalTime

class HomeCalculationsTest {

    private val today = LocalDate.of(2026, 9, 24)

    private fun task(title: String, start: Int, end: Int, done: Boolean = false) = DailyTask(
        entry = ScheduleEntry(id = start.toLong(), title = title, startTime = LocalTime.of(start, 0), endTime = LocalTime.of(end, 0)),
        date = today,
        isCompleted = done
    )

    private val tasks = listOf(task("Ish", 9, 13), task("Dars", 14, 16), task("Sport", 18, 19))

    @Test
    fun `current task is the one running now`() {
        assertEquals("Ish", HomeCalculations.currentTask(tasks, today.atTime(10, 0))?.entry?.title)
        assertNull(HomeCalculations.currentTask(tasks, today.atTime(13, 30)))
        assertEquals(30L, HomeCalculations.minutesLeft(tasks[0], today.atTime(12, 30)))
    }

    @Test
    fun `next is the nearest upcoming unfinished task`() {
        val next = HomeCalculations.nextItem(tasks, RoutineSettings(), today.atTime(13, 30))
        assertEquals("Dars", next?.title)
        assertEquals(today.atTime(14, 0), next?.startsAt)
    }

    @Test
    fun `completed upcoming task is skipped`() {
        val list = listOf(task("Dars", 14, 16, done = true), task("Sport", 18, 19))
        assertEquals("Sport", HomeCalculations.nextItem(list, RoutineSettings(), today.atTime(13, 0))?.title)
    }

    @Test
    fun `after the last task the routine start is next`() {
        val next = HomeCalculations.nextItem(tasks, RoutineSettings(), today.atTime(20, 0))
        assertNull(next?.title)
        assertEquals(today.atTime(22, 0), next?.startsAt)
    }

    @Test
    fun `nothing next when routine is off and no tasks remain`() {
        assertNull(HomeCalculations.nextItem(tasks, RoutineSettings(isRoutineActive = false), today.atTime(20, 0)))
    }
}
