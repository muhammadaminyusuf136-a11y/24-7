package uz.reja24.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class AlarmPlannerTest {

    private val today = LocalDate.of(2026, 9, 24)

    @Test
    fun `next occurrence is today if still ahead, otherwise tomorrow`() {
        val now = today.atTime(21, 0)
        assertEquals(today.atTime(22, 0), AlarmPlanner.nextOccurrence(now, LocalTime.of(22, 0)))
        assertEquals(today.plusDays(1).atTime(6, 0), AlarmPlanner.nextOccurrence(now, LocalTime.of(6, 0)))
        // Aynan shu daqiqa — ertaga
        assertEquals(today.plusDays(1).atTime(21, 0), AlarmPlanner.nextOccurrence(now, LocalTime.of(21, 0)))
    }

    @Test
    fun `routine stages are 60, 30, 15 and 1 minutes before blocking`() {
        val alarms = AlarmPlanner.routineAlarms(today.atTime(12, 0), RoutineSettings())
        assertEquals(today.atTime(21, 0), alarms[RoutineAlarmKind.STAGE_1])
        assertEquals(today.atTime(21, 30), alarms[RoutineAlarmKind.STAGE_2])
        assertEquals(today.atTime(21, 45), alarms[RoutineAlarmKind.STAGE_3])
        assertEquals(today.atTime(21, 59), alarms[RoutineAlarmKind.FINAL_WARNING])
        assertEquals(today.atTime(22, 0), alarms[RoutineAlarmKind.BLOCK_START])
        assertEquals(today.plusDays(1).atTime(6, 0), alarms[RoutineAlarmKind.WAKE_UP])
    }

    @Test
    fun `stage before midnight block wraps to previous evening`() {
        val settings = RoutineSettings(blockStartTime = LocalTime.of(0, 30))
        val alarms = AlarmPlanner.routineAlarms(today.atTime(12, 0), settings)
        assertEquals(today.atTime(23, 30), alarms[RoutineAlarmKind.STAGE_1])
        assertEquals(today.plusDays(1).atTime(0, 30), alarms[RoutineAlarmKind.BLOCK_START])
    }

    @Test
    fun `inactive routine has no alarms`() {
        assertTrue(AlarmPlanner.routineAlarms(today.atTime(12, 0), RoutineSettings(isRoutineActive = false)).isEmpty())
    }

    @Test
    fun `task alarms skip the past and include reminder, start and end`() {
        val entry = ScheduleEntry(
            id = 7, title = "Dars",
            startTime = LocalTime.of(13, 0), endTime = LocalTime.of(14, 0),
            reminderMinutesBefore = 15
        )
        val now: LocalDateTime = today.atTime(12, 50)   // eslatma (12:45) o'tib ketgan
        val alarms = AlarmPlanner.taskAlarms(now, today, listOf(listOf(entry), listOf(entry)))
        val todayKinds = alarms.filter { it.dayIndex == 0 }.map { it.kind }
        assertEquals(listOf(TaskAlarmKind.START, TaskAlarmKind.END), todayKinds)
        assertEquals(3, alarms.count { it.dayIndex == 1 })
        assertEquals(today.plusDays(1).atTime(12, 45), alarms.first { it.dayIndex == 1 }.triggerAt)
    }

    @Test
    fun `reminder off means no reminder alarm`() {
        val entry = ScheduleEntry(
            id = 1, title = "Sport",
            startTime = LocalTime.of(18, 0), endTime = LocalTime.of(19, 0),
            reminderMinutesBefore = 0
        )
        val alarms = AlarmPlanner.taskAlarms(today.atTime(8, 0), today, listOf(listOf(entry)))
        assertTrue(alarms.none { it.kind == TaskAlarmKind.REMINDER })
    }
}
