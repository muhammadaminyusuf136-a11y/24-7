package uz.reja24.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class RoutineSettingsTest {

    @Test
    fun `default overnight window 22 to 06`() {
        val s = RoutineSettings()
        assertFalse(s.isInBlockWindow(LocalTime.of(21, 59)))
        assertTrue(s.isInBlockWindow(LocalTime.of(22, 0)))   // boshlanish — ichida
        assertTrue(s.isInBlockWindow(LocalTime.of(23, 59)))
        assertTrue(s.isInBlockWindow(LocalTime.MIDNIGHT))
        assertTrue(s.isInBlockWindow(LocalTime.of(5, 59)))
        assertFalse(s.isInBlockWindow(LocalTime.of(6, 0)))   // uyg'onish — tashqarida
        assertFalse(s.isInBlockWindow(LocalTime.NOON))
    }

    @Test
    fun `daytime window does not wrap`() {
        val s = RoutineSettings(blockStartTime = LocalTime.of(13, 0), wakeTime = LocalTime.of(15, 0))
        assertFalse(s.isInBlockWindow(LocalTime.of(12, 59)))
        assertTrue(s.isInBlockWindow(LocalTime.of(14, 0)))
        assertFalse(s.isInBlockWindow(LocalTime.of(15, 0)))
        assertFalse(s.isInBlockWindow(LocalTime.of(23, 0)))
    }

    @Test
    fun `inactive routine never blocks`() {
        val s = RoutineSettings(isRoutineActive = false)
        assertFalse(s.isInBlockWindow(LocalTime.of(23, 30)))
    }

    @Test
    fun `strict mode allows one bypass`() {
        assertEquals(3, RoutineSettings().maxDailyBypasses)
        assertEquals(1, RoutineSettings(isStrictModeOn = true).maxDailyBypasses)
    }
}
