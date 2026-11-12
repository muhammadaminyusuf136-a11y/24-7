package uz.reja24.app.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.reja24.app.domain.model.DailyTask
import uz.reja24.app.domain.model.ScheduleEntry
import uz.reja24.app.domain.prayer.Prayer
import uz.reja24.app.domain.prayer.PrayerDay
import java.time.LocalDate
import java.time.LocalTime

class WidgetItemsTest {

    private val today = LocalDate.of(2026, 9, 27)

    private fun task(id: Long, title: String, from: LocalTime, to: LocalTime, done: Boolean = false) = DailyTask(
        ScheduleEntry(id = id, title = title, startTime = from, endTime = to), today, done
    )

    private val prayers = PrayerDay(
        today,
        mapOf(
            Prayer.FAJR to today.atTime(4, 47),
            Prayer.SUNRISE to today.atTime(6, 4),
            Prayer.DHUHR to today.atTime(12, 3),
            Prayer.ASR to today.atTime(16, 16),
            Prayer.MAGHRIB to today.atTime(18, 8),
            Prayer.ISHA to today.atTime(19, 17)
        )
    )

    @Test
    fun shows_running_and_upcoming_items_in_time_order_and_hides_finished() {
        val tasks = listOf(
            task(1, "Ish", LocalTime.of(9, 0), LocalTime.of(13, 0)),
            task(2, "Dars", LocalTime.of(14, 0), LocalTime.of(15, 30), done = true),
            task(3, "Tahajjud", LocalTime.of(4, 0), LocalTime.of(4, 30)),
            task(4, "Uyqu", LocalTime.of(23, 0), LocalTime.of(6, 0))
        )
        val items = WidgetDataLoader.buildItems(tasks, prayers, today.atTime(12, 10)) { it.name }
        assertEquals(listOf("Ish", "DHUHR", "Dars", "ASR", "MAGHRIB", "ISHA", "Uyqu"), items.map { it.title })
        // 12:10 — Ish davom etmoqda, Peshin vaqti kirgan (20 daqiqa ichida)
        assertTrue(items.first { it.title == "Ish" }.isNow)
        assertTrue(items.first { it.title == "DHUHR" }.isNow)
        assertTrue(items.first { it.title == "Dars" }.isDone)
        assertTrue(items.none { it.title == "Tahajjud" || it.title == "FAJR" || it.title == "SUNRISE" })
    }

    @Test
    fun late_evening_shows_only_night_items() {
        val tasks = listOf(task(4, "Uyqu", LocalTime.of(23, 0), LocalTime.of(6, 0)))
        val items = WidgetDataLoader.buildItems(tasks, prayers, today.atTime(23, 30)) { it.name }
        assertEquals(listOf("Uyqu"), items.map { it.title })
        assertTrue(items.single().isNow)
    }

    @Test
    fun without_prayers_only_tasks() {
        val items = WidgetDataLoader.buildItems(
            listOf(task(1, "Ish", LocalTime.of(9, 0), LocalTime.of(13, 0))), null, today.atTime(8, 0)
        ) { it.name }
        assertEquals(1, items.size)
        assertTrue(!items.single().isNow)
    }
}
