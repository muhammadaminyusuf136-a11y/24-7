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
    fun finished_items_stay_in_the_list_marked_as_past() {
        val tasks = listOf(
            task(1, "Ish", LocalTime.of(9, 0), LocalTime.of(13, 0)),
            task(2, "Dars", LocalTime.of(14, 0), LocalTime.of(15, 30), done = true),
            task(3, "Tahajjud", LocalTime.of(4, 0), LocalTime.of(4, 30)),
            task(4, "Uyqu", LocalTime.of(23, 0), LocalTime.of(6, 0))
        )
        val items = WidgetDataLoader.buildItems(tasks, prayers, today.atTime(12, 10)) { it.name }
        assertEquals(
            listOf("Tahajjud", "FAJR", "Ish", "DHUHR", "Dars", "ASR", "MAGHRIB", "ISHA", "Uyqu"),
            items.map { it.title }
        )
        // O'tganlar o'chmaydi — kulrang (isPast), lekin "hozir" emas
        assertEquals(listOf("Tahajjud", "FAJR"), items.filter { it.isPast }.map { it.title })
        assertTrue(items.filter { it.isPast }.none { it.isNow })
        // 12:10 — Ish davom etmoqda, Peshin vaqti kirgan (20 daqiqa ichida)
        assertTrue(items.first { it.title == "Ish" }.isNow)
        assertTrue(items.first { it.title == "DHUHR" }.isNow)
        assertTrue(items.first { it.title == "Dars" }.isDone)
        assertTrue(items.none { it.title == "SUNRISE" })
    }

    @Test
    fun late_evening_keeps_the_day_and_marks_night_item_as_now() {
        val tasks = listOf(task(4, "Uyqu", LocalTime.of(23, 0), LocalTime.of(6, 0)))
        val items = WidgetDataLoader.buildItems(tasks, prayers, today.atTime(23, 30)) { it.name }
        assertEquals(listOf("FAJR", "DHUHR", "ASR", "MAGHRIB", "ISHA", "Uyqu"), items.map { it.title })
        assertTrue(items.last().isNow)
        assertTrue(items.dropLast(1).all { it.isPast })
    }

    @Test
    fun without_prayers_only_tasks() {
        val items = WidgetDataLoader.buildItems(
            listOf(task(1, "Ish", LocalTime.of(9, 0), LocalTime.of(13, 0))), null, today.atTime(8, 0)
        ) { it.name }
        assertEquals(1, items.size)
        assertTrue(!items.single().isNow)
        assertTrue(!items.single().isPast)
    }

    private fun item(title: String, past: Boolean) =
        WidgetItem(0, "00:00", title, 0, isNow = false, isDone = false, isPrayer = false, isPast = past)

    @Test
    fun window_keeps_current_and_next_items_visible_and_counts_hidden_past() {
        val items = listOf(
            item("a", true), item("b", true), item("c", true), item("d", true),
            item("e", false), item("f", false), item("g", false), item("h", false)
        )
        // 2 ta "yana" qatori bo'lsa 4 qator, 1 ta bo'lsa 5, hech biri bo'lmasa 6 qator sig'adi
        val w = WidgetDataLoader.window(items) { lines -> 6 - lines }
        // Eng yaqin o'tgan (d) va hozirgi-keyingilar ko'rinadi; tepada 3 ta o'tgan, pastda 1 ta yashirilgan
        assertEquals(listOf("d", "e", "f", "g"), w.shown.map { it.title })
        assertEquals(3, w.hiddenBefore)
        assertEquals(1, w.hiddenAfter)
    }

    @Test
    fun window_shows_everything_when_it_fits() {
        val items = listOf(item("a", true), item("b", false))
        val w = WidgetDataLoader.window(items) { 5 }
        assertEquals(2, w.shown.size)
        assertEquals(0, w.hiddenBefore + w.hiddenAfter)
    }

    @Test
    fun window_hides_later_items_when_day_is_long() {
        val items = listOf(item("a", true)) + (1..9).map { item("n$it", false) }
        val w = WidgetDataLoader.window(items) { lines -> 6 - lines }
        assertEquals("a", w.shown.first().title)
        assertEquals(0, w.hiddenBefore)
        assertEquals(10 - w.shown.size, w.hiddenAfter)
    }
}
