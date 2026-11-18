package uz.reja24.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.reja24.app.domain.prayer.Prayer
import uz.reja24.app.domain.prayer.PrayerDay
import uz.reja24.app.domain.prayer.PrayerSettings
import java.time.LocalDate
import java.time.LocalTime

class PrayerAlarmPlannerTest {

    private val today = LocalDate.of(2026, 9, 27)

    private fun day(date: LocalDate) = PrayerDay(
        date,
        mapOf(
            Prayer.FAJR to date.atTime(4, 43),
            Prayer.SUNRISE to date.atTime(6, 15),
            Prayer.DHUHR to date.atTime(12, 14),
            Prayer.ASR to date.atTime(16, 26),
            Prayer.MAGHRIB to date.atTime(18, 12),
            Prayer.ISHA to date.atTime(19, 39)
        )
    )

    private val days = listOf(day(today), day(today.plusDays(1)))

    @Test
    fun reminders_fifteen_minutes_before_every_prayer_only_in_future() {
        val now = today.atTime(LocalTime.of(12, 0))
        val alarms = AlarmPlanner.prayerAlarms(now, days, PrayerSettings())
        val reminders = alarms.filter { it.kind == PrayerAlarmKind.REMINDER }
        // Bugun: peshin (11:59 — o'tib ketgan), asr, shom, xufton = 3; ertaga: 5
        assertEquals(8, reminders.size)
        assertTrue(reminders.none { it.prayer == Prayer.SUNRISE })
        val asr = reminders.first { it.prayer == Prayer.ASR && it.dayIndex == 0 }
        assertEquals(today.atTime(16, 11), asr.triggerAt)
        assertTrue(alarms.all { it.triggerAt.isAfter(now) })
    }

    @Test
    fun alarm_replaces_at_time_notification_and_muted_prayer_keeps_alarm() {
        val settings = PrayerSettings(alarmPrayers = setOf(Prayer.FAJR), mutedPrayers = setOf(Prayer.FAJR, Prayer.ASR))
        val alarms = AlarmPlanner.prayerAlarms(today.atTime(0, 0), days, settings)
        // Zikr eslatmasi namoz bildirishnomasidan alohida (namoz ovozsiz bo'lsa ham zikr qoladi)
        val fajrToday = alarms.filter { it.prayer == Prayer.FAJR && it.dayIndex == 0 && it.kind != PrayerAlarmKind.AZKAR }
        assertEquals(listOf(PrayerAlarmKind.ALARM), fajrToday.map { it.kind })
        assertTrue(alarms.none { it.prayer == Prayer.ASR && it.kind != PrayerAlarmKind.AZKAR })
    }

    @Test
    fun azkar_after_fajr_and_asr_only_and_can_be_turned_off() {
        val alarms = AlarmPlanner.prayerAlarms(today.atTime(0, 0), days, PrayerSettings())
        val azkar = alarms.filter { it.kind == PrayerAlarmKind.AZKAR }
        assertEquals(setOf(Prayer.FAJR, Prayer.ASR), azkar.map { it.prayer }.toSet())
        assertEquals(4, azkar.size)
        azkar.forEach { a -> assertEquals(days[a.dayIndex].time(a.prayer).plusMinutes(20), a.triggerAt) }
        assertTrue(AlarmPlanner.prayerAlarms(today.atTime(0, 0), days, PrayerSettings(azkarOn = false)).none { it.kind == PrayerAlarmKind.AZKAR })
    }

    @Test
    fun disabled_gives_nothing_and_zero_reminder_skips_reminders() {
        assertTrue(AlarmPlanner.prayerAlarms(today.atTime(0, 0), days, PrayerSettings(isEnabled = false)).isEmpty())
        val noReminders = AlarmPlanner.prayerAlarms(today.atTime(0, 0), days, PrayerSettings(reminderMinutes = 0))
        assertTrue(noReminders.none { it.kind == PrayerAlarmKind.REMINDER })
        assertEquals(10, noReminders.count { it.kind == PrayerAlarmKind.AT_TIME })
    }

    @Test
    fun task_start_rings_only_when_alarm_enabled() {
        val entries = listOf(
            ScheduleEntry(id = 1, title = "Tahajjud", startTime = LocalTime.of(4, 0), endTime = LocalTime.of(4, 30), alarmEnabled = true),
            ScheduleEntry(id = 2, title = "Ish", startTime = LocalTime.of(9, 0), endTime = LocalTime.of(18, 0))
        )
        val planned = AlarmPlanner.taskAlarms(today.atTime(0, 0), today, listOf(entries, emptyList()))
        val starts = planned.filter { it.kind == TaskAlarmKind.START }.associateBy { it.entryId }
        assertTrue(starts.getValue(1).rings)
        assertTrue(!starts.getValue(2).rings)
        assertTrue(planned.filter { it.kind != TaskAlarmKind.START }.none { it.rings })
    }
}
