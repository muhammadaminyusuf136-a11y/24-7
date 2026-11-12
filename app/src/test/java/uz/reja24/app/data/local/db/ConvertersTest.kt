package uz.reja24.app.data.local.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `local time round trip`() {
        listOf(LocalTime.MIDNIGHT, LocalTime.of(8, 30), LocalTime.of(23, 59)).forEach { time ->
            assertEquals(time, converters.minutesToLocalTime(converters.localTimeToMinutes(time)))
        }
        assertEquals(510, converters.localTimeToMinutes(LocalTime.of(8, 30)))
        assertNull(converters.localTimeToMinutes(null))
    }

    @Test
    fun `local date round trip`() {
        val date = LocalDate.of(2026, 9, 24)
        assertEquals(date, converters.epochDayToLocalDate(converters.localDateToEpochDay(date)))
        assertNull(converters.epochDayToLocalDate(null))
    }

    @Test
    fun `days of week mask round trip`() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY)
        val mask = converters.daysOfWeekToMask(days)
        assertEquals(1 or 4 or 64, mask)
        assertEquals(days, converters.maskToDaysOfWeek(mask))
        assertEquals(emptySet<DayOfWeek>(), converters.maskToDaysOfWeek(0))
        assertEquals(DayOfWeek.values().toSet(), converters.maskToDaysOfWeek(127))
    }
}
