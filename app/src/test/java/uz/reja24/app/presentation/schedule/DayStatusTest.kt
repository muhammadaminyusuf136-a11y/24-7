package uz.reja24.app.presentation.schedule

import org.junit.Assert.assertEquals
import org.junit.Test

/** Oy kalendaridagi katak rangi. */
class DayStatusTest {
    @Test
    fun colors_by_completion() {
        assertEquals(DayStatus.DONE, dayStatus(3, 3, isPast = true, isToday = false))
        assertEquals(DayStatus.PARTIAL, dayStatus(3, 1, isPast = true, isToday = false))
        assertEquals(DayStatus.MISSED, dayStatus(3, 0, isPast = true, isToday = false))
        // Bugun hali tugamagan — bajarilmaganlar qizil emas
        assertEquals(DayStatus.NONE, dayStatus(3, 0, isPast = false, isToday = true))
        assertEquals(DayStatus.PARTIAL, dayStatus(3, 2, isPast = false, isToday = true))
        // Kelajak va rejasiz kunlar — rangsiz
        assertEquals(DayStatus.NONE, dayStatus(3, 0, isPast = false, isToday = false))
        assertEquals(DayStatus.NONE, dayStatus(0, 0, isPast = true, isToday = false))
    }
}
