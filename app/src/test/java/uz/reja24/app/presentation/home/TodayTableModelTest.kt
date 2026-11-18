package uz.reja24.app.presentation.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.reja24.app.domain.model.DailyTask
import uz.reja24.app.domain.model.RoutineSettings
import uz.reja24.app.domain.model.ScheduleEntry
import uz.reja24.app.domain.prayer.Prayer
import uz.reja24.app.domain.prayer.PrayerDay
import java.time.LocalDate
import java.time.LocalTime

class TodayTableModelTest {

    private val today = LocalDate.of(2026, 11, 18)
    private val settings = RoutineSettings(blockStartTime = LocalTime.of(22, 30), wakeTime = LocalTime.of(6, 0))

    private fun task(id: Long, title: String, from: Int, to: Int, done: Boolean = false) = DailyTask(
        ScheduleEntry(id = id, title = title, startTime = LocalTime.of(from, 0), endTime = LocalTime.of(to, 0)), today, done
    )

    private val prayers = PrayerDay(
        today,
        mapOf(
            Prayer.FAJR to today.atTime(5, 50),
            Prayer.SUNRISE to today.atTime(7, 15),
            Prayer.DHUHR to today.atTime(12, 10),
            Prayer.ASR to today.atTime(15, 0),
            Prayer.MAGHRIB to today.atTime(17, 0),
            Prayer.ISHA to today.atTime(18, 30)
        )
    )

    @Test
    fun rows_are_time_ordered_with_states_and_past_rows_kept() {
        val tasks = listOf(task(1, "Revit", 11, 13), task(2, "Sport", 6, 7, done = true), task(3, "Ingliz", 17, 19))
        val rows = TodayTableModel.rows(tasks, settings, prayers, today.atTime(12, 0))
        assertEquals(
            listOf("prayer-FAJR", "task-2", "task-1", "prayer-DHUHR", "prayer-ASR", "task-3", "prayer-MAGHRIB", "prayer-ISHA", "sleep"),
            rows.map { it.key }
        )
        val byKey = rows.associateBy { it.key }
        // O'tganlar ro'yxatda qoladi
        assertEquals(TableRowState.PAST, byKey.getValue("task-2").state)
        assertTrue(byKey.getValue("task-2").isDone)
        assertEquals(TableRowState.NOW, byKey.getValue("task-1").state)
        assertEquals(TableRowState.LATER, byKey.getValue("task-3").state)
        // Uyqu rejimi 22:30 – 06:00
        assertEquals(LocalTime.of(22, 30), byKey.getValue("sleep").startTime)
        assertEquals(LocalTime.of(6, 0), byKey.getValue("sleep").endTime)
    }

    @Test
    fun after_midnight_shows_running_routine_from_yesterday_first() {
        val rows = TodayTableModel.rows(emptyList(), settings, null, today.atTime(2, 0))
        assertEquals("sleep-yesterday", rows.first().key)
        assertEquals(TableRowState.NOW, rows.first().state)
        assertEquals(TableRowState.LATER, rows.last().state)
    }

    @Test
    fun routine_off_has_no_sleep_row() {
        val rows = TodayTableModel.rows(listOf(task(1, "Revit", 11, 13)), settings.copy(isRoutineActive = false), null, today.atTime(9, 0))
        assertEquals(listOf("task-1"), rows.map { it.key })
    }
}
