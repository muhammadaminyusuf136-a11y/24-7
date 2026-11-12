package uz.reja24.app.presentation.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.reja24.app.domain.model.DailyTask
import uz.reja24.app.domain.model.RoutineSettings
import uz.reja24.app.domain.model.ScheduleEntry
import uz.reja24.app.domain.prayer.Prayer
import uz.reja24.app.domain.prayer.PrayerDay
import java.time.LocalDate
import java.time.LocalTime

class TimelineModelTest {

    private val today = LocalDate.of(2026, 9, 24)
    private val start = TimelineModel.trackStart(today)
    private val day = TimelineModel.MINUTES_IN_DAY

    private fun task(id: Long, date: LocalDate, from: Int, to: Int, title: String = "Reja $id") = DailyTask(
        entry = ScheduleEntry(id = id, title = title, startTime = LocalTime.of(from, 0), endTime = LocalTime.of(to, 0)),
        date = date,
        isCompleted = false
    )

    @Test
    fun `tasks are placed on the correct day of the 72 hour track`() {
        val blocks = TimelineModel.build(
            start,
            mapOf(today to listOf(task(1, today, 11, 13, "Revit"))),
            RoutineSettings(isRoutineActive = false)
        )
        val revit = blocks.single()
        assertEquals(day + 11 * 60, revit.startMinute)
        assertEquals(day + 13 * 60, revit.endMinute)
    }

    @Test
    fun `sleep blocks cross midnight and are clipped to the track`() {
        val blocks = TimelineModel.build(start, emptyMap(), RoutineSettings()).filter { it.isSleep }
        // Birinchi blok — kechagi kun boshidagi tun qismi (00:00–06:00)
        assertEquals(0, blocks.first().startMinute)
        assertEquals(6 * 60, blocks.first().endMinute)
        // Bugungi kechqurun 22:00 → ertaga 06:00
        assertTrue(blocks.any { it.startMinute == day + 22 * 60 && it.endMinute == 2 * day + 6 * 60 })
        assertTrue(blocks.all { it.startMinute >= 0 && it.endMinute <= TimelineModel.TRACK_MINUTES })
    }

    @Test
    fun `task wins over sleep at the same minute and gaps are free`() {
        val blocks = TimelineModel.build(
            start,
            mapOf(today to listOf(task(7, today, 22, 23, "Kitob"))),
            RoutineSettings()
        )
        assertEquals("Kitob", TimelineModel.blockAt(blocks, day + 22 * 60 + 30f)?.title)
        assertTrue(TimelineModel.blockAt(blocks, day + 23 * 60 + 30f)?.isSleep == true)
        assertNull(TimelineModel.blockAt(blocks, day + 12 * 60f))
    }

    @Test
    fun `prayers are placed on the track and win over tasks and sleep`() {
        val prayerDay = PrayerDay(
            today,
            mapOf(
                Prayer.FAJR to today.atTime(4, 43),
                Prayer.SUNRISE to today.atTime(6, 15),
                Prayer.DHUHR to today.atTime(12, 14),
                Prayer.ASR to today.atTime(16, 26),
                Prayer.MAGHRIB to today.atTime(18, 12),
                Prayer.ISHA to today.atTime(19, 39)
            )
        )
        val blocks = TimelineModel.build(
            start,
            mapOf(today to listOf(task(3, today, 9, 18, "Ish"))),
            RoutineSettings(isRoutineActive = false),
            mapOf(today to prayerDay)
        )
        // Quyosh chiqishi namoz emas — lentaga chiqmaydi
        assertEquals(5, blocks.count { it.isPrayer })
        val dhuhr = blocks.single { it.prayer == Prayer.DHUHR }
        assertEquals(day + 12 * 60 + 14, dhuhr.startMinute)
        assertEquals(Prayer.DHUHR, TimelineModel.blockAt(blocks, day + 12 * 60 + 20f)?.prayer)
        assertEquals("Ish", TimelineModel.blockAt(blocks, day + 13 * 60f)?.title)
    }

    @Test
    fun `track minute converts back to clock time and day`() {
        assertEquals(LocalTime.of(17, 30), TimelineModel.timeOf(day + 17 * 60 + 30f))
        assertEquals(0, TimelineModel.dayIndexOf(100f))
        assertEquals(1, TimelineModel.dayIndexOf(day + 1f))
        assertEquals(2, TimelineModel.dayIndexOf(TimelineModel.TRACK_MINUTES.toFloat()))
    }
}
