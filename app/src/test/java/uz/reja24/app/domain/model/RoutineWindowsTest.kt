package uz.reja24.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/** Vaqtida uxlash / uygʻonish oynalari. */
class RoutineWindowsTest {

    @Test
    fun `evening bedtime counts through the night until the wake window`() {
        val s = RoutineSettings(sleepTime = LocalTime.of(23, 0), wakeTime = LocalTime.of(6, 0))
        assertFalse(s.isAfterBedtime(LocalTime.of(23, 14)))  // 15 daqiqa imtiyoz
        assertTrue(s.isAfterBedtime(LocalTime.of(23, 15)))
        assertTrue(s.isAfterBedtime(LocalTime.of(23, 59)))
        assertTrue(s.isAfterBedtime(LocalTime.of(1, 0)))     // yarim tundan keyin ham
        assertTrue(s.isAfterBedtime(LocalTime.of(4, 59)))
        assertFalse(s.isAfterBedtime(LocalTime.of(5, 0)))    // uyg'onish oynasi boshlandi
        assertFalse(s.isAfterBedtime(LocalTime.NOON))
    }

    @Test
    fun `after midnight bedtime counts until the wake window`() {
        val s = RoutineSettings(sleepTime = LocalTime.of(0, 30), wakeTime = LocalTime.of(7, 0))
        assertFalse(s.isAfterBedtime(LocalTime.of(23, 50)))
        assertFalse(s.isAfterBedtime(LocalTime.of(0, 40)))
        assertTrue(s.isAfterBedtime(LocalTime.of(0, 45)))
        assertTrue(s.isAfterBedtime(LocalTime.of(5, 59)))
        assertFalse(s.isAfterBedtime(LocalTime.of(6, 0)))
    }

    @Test
    fun `night events belong to the routine day that started in the evening`() {
        val s = RoutineSettings(wakeTime = LocalTime.of(6, 0))
        val day = LocalDate.of(2026, 9, 24)
        assertEquals(day, s.routineDayFor(day.atTime(22, 30)))
        assertEquals(day, s.routineDayFor(day.plusDays(1).atTime(1, 30)))  // 25-sentabr tuni → 24-sentabr
        assertEquals(day, s.routineDayFor(day.plusDays(1).atTime(5, 59)))
        assertEquals(day.plusDays(1), s.routineDayFor(day.plusDays(1).atTime(6, 0)))
    }

    @Test
    fun `wake up belongs to that morning`() {
        val s = RoutineSettings(wakeTime = LocalTime.of(6, 0))
        val day = LocalDate.of(2026, 9, 25)
        assertEquals(day, s.wakeDayFor(day.atTime(5, 40)))
        val early = RoutineSettings(wakeTime = LocalTime.of(0, 30))
        assertEquals(day, early.wakeDayFor(day.minusDays(1).atTime(23, 45)))  // oyna kechqurun boshlanadi
        assertEquals(day, early.wakeDayFor(day.atTime(0, 40)))
    }

    @Test
    fun `inactive routine never counts bedtime usage`() {
        assertFalse(RoutineSettings(isRoutineActive = false).isAfterBedtime(LocalTime.of(23, 50)))
    }

    @Test
    fun `wake window is 1 hour before to 30 minutes after`() {
        val s = RoutineSettings(wakeTime = LocalTime.of(6, 0))
        assertFalse(s.isInWakeWindow(LocalTime.of(3, 10)))   // tunda telefon ochilishi — ball berilmaydi
        assertFalse(s.isInWakeWindow(LocalTime.of(4, 59)))
        assertTrue(s.isInWakeWindow(LocalTime.of(5, 0)))
        assertTrue(s.isInWakeWindow(LocalTime.of(6, 30)))
        assertFalse(s.isInWakeWindow(LocalTime.of(6, 31)))
    }

    @Test
    fun `wake window wraps around midnight`() {
        val s = RoutineSettings(wakeTime = LocalTime.of(0, 30))
        assertTrue(s.isInWakeWindow(LocalTime.of(23, 40)))
        assertTrue(s.isInWakeWindow(LocalTime.of(0, 50)))
        assertFalse(s.isInWakeWindow(LocalTime.of(23, 20)))
        assertFalse(s.isInWakeWindow(LocalTime.of(1, 5)))
    }
}
