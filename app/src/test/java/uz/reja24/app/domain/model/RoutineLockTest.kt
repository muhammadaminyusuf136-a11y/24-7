package uz.reja24.app.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class RoutineLockTest {

    private val routine = RoutineSettings(blockStartTime = LocalTime.of(22, 0), wakeTime = LocalTime.of(6, 0))

    @Test
    fun locked_from_six_hours_before_block_until_wake() {
        assertTrue(RoutineLock.isLocked(routine, LocalTime.of(2, 0)))
        assertTrue(RoutineLock.isLocked(routine, LocalTime.of(22, 0)))
        // Bloklashdan 5 daqiqa oldin ro'yxatdan chiqarib olish — endi mumkin emas
        assertTrue(RoutineLock.isLocked(routine, LocalTime.of(21, 55)))
        assertTrue(RoutineLock.isLocked(routine, LocalTime.of(16, 0)))
        assertFalse(RoutineLock.isLocked(routine, LocalTime.of(15, 59)))
        assertFalse(RoutineLock.isLocked(routine, LocalTime.of(6, 0)))
        assertFalse(RoutineLock.isLocked(routine, LocalTime.of(10, 0)))
        // Uyg'oq vaqt qisqa (06:00–09:00): qulf uning yarmidan boshlanadi, ertalab o'zgartirish oynasi qoladi
        val longBlock = RoutineSettings(blockStartTime = LocalTime.of(9, 0), wakeTime = LocalTime.of(6, 0))
        assertTrue(RoutineLock.isLocked(longBlock, LocalTime.of(5, 0)))
        assertTrue(RoutineLock.isLocked(longBlock, LocalTime.of(8, 0)))
        assertFalse(RoutineLock.isLocked(longBlock, LocalTime.of(7, 0)))
        assertFalse(RoutineLock.isLocked(routine.copy(isRoutineActive = false), LocalTime.of(2, 0)))
    }

    @Test
    fun weakening_changes_are_detected() {
        assertTrue(RoutineLock.weakens(routine, routine.copy(isRoutineActive = false)))
        assertTrue(RoutineLock.weakens(routine, routine.copy(blockStartTime = LocalTime.of(23, 30))))
        assertTrue(RoutineLock.weakens(routine, routine.copy(wakeTime = LocalTime.of(1, 0))))
        assertTrue(RoutineLock.weakens(routine.copy(isStrictModeOn = true), routine.copy(isStrictModeOn = false)))
    }

    @Test
    fun harmless_or_stronger_changes_are_allowed() {
        assertFalse(RoutineLock.weakens(routine, routine.copy(isStrictModeOn = true)))
        assertFalse(RoutineLock.weakens(routine, routine.copy(isVoiceReminderOn = true)))
        assertFalse(RoutineLock.weakens(routine, routine.copy(ringtoneUri = "content://x")))
        assertFalse(RoutineLock.weakens(routine, routine.copy(reminderStage1Minutes = 90)))
    }
}
