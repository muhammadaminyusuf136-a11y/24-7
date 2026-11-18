package uz.reja24.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.reja24.app.domain.repository.AppRule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Qattiq rejimda Sozlamalarni yopish, soat himoyasi va kunduzgi nazorat. */
class NightProtectionTest {

    private val settings = RoutineSettings()                     // 22:00 → 06:00
    private val strict = settings.copy(isStrictModeOn = true)
    private val whitelist = setOf("com.android.settings", "com.android.dialer", "com.google.android.deskclock")
    private val now = 1_000_000L

    private fun block(pkg: String, routine: RoutineSettings, time: LocalTime, forceWindow: Boolean = false, bypassUntil: Long = 0L) =
        BlockPolicy.shouldBlock(pkg, time, now, routine, emptySet(), whitelist, bypassUntil, forceWindow = forceWindow)

    // ---- Qattiq rejim: Sozlamalar ----

    @Test
    fun `settings are closed at night only in strict mode`() {
        assertTrue(block("com.android.settings", strict, LocalTime.of(23, 0)))
        assertFalse(block("com.android.settings", settings, LocalTime.of(23, 0)))
        assertFalse(block("com.android.settings", strict, LocalTime.NOON))
    }

    @Test
    fun `calls and clock stay open in strict mode`() {
        assertFalse(block("com.android.dialer", strict, LocalTime.of(23, 0)))
        assertFalse(block("com.google.android.deskclock", strict, LocalTime.of(23, 0)))
    }

    @Test
    fun `uninstall screen and play store are closed in strict mode`() {
        assertTrue(block("com.google.android.packageinstaller", strict, LocalTime.of(1, 0)))
        assertTrue(block("com.android.vending", strict, LocalTime.of(1, 0)))
    }

    @Test
    fun `emergency access opens settings for its duration`() =
        assertFalse(block("com.android.settings", strict, LocalTime.of(23, 0), bypassUntil = now + 60_000))

    // ---- Soat himoyasi ----

    @Test
    fun `clock moved to morning keeps blocking while the guard is locked`() {
        assertTrue(block("com.android.settings", strict, LocalTime.of(6, 30), forceWindow = true))
        assertFalse(block("com.android.settings", strict, LocalTime.of(6, 30), forceWindow = false))
    }

    @Test
    fun `guard does nothing when the routine is off`() =
        assertFalse(block("com.android.settings", strict.copy(isRoutineActive = false), LocalTime.of(6, 30), forceWindow = true))

    @Test
    fun `remaining time counts to wake up across midnight`() {
        val day = LocalDate.of(2026, 11, 19)
        assertEquals(8 * 3600_000L, ClockGuard.remainingMillis(settings, day.atTime(22, 0)))
        assertEquals(30 * 60_000L, ClockGuard.remainingMillis(settings, day.atTime(5, 30)))
        assertNull(ClockGuard.remainingMillis(settings, LocalDateTime.of(day, LocalTime.NOON)))
    }

    @Test
    fun `small network corrections are not tampering`() {
        assertFalse(ClockGuard.isTampered(0L, 30_000L))
        assertTrue(ClockGuard.isTampered(0L, 3 * 3600_000L))
        assertTrue(ClockGuard.isTampered(0L, -10 * 60_000L))
    }

    @Test
    fun `guard lock is based on the elapsed counter`() {
        assertTrue(ClockGuard.isLocked(lockUntilElapsed = 10_000L, elapsedNow = 9_999L))
        assertFalse(ClockGuard.isLocked(lockUntilElapsed = 10_000L, elapsedNow = 10_000L))
    }

    // ---- Kunduzgi nazorat ----

    private val instagram = AppRule("com.instagram.android", "Instagram", dailyLimitMinutes = 30, pauseOn = true)

    @Test
    fun `no rule means no control`() =
        assertEquals(DayControl.Decision.NONE, DayControl.decide(null, 99_999, null, 0))

    @Test
    fun `limit reached closes the app`() =
        assertEquals(DayControl.Decision.LIMIT_REACHED, DayControl.decide(instagram, 30 * 60L, null, 0))

    @Test
    fun `pause is asked before opening and skipped right after passing it`() {
        assertEquals(DayControl.Decision.PAUSE, DayControl.decide(instagram, 60, null, 1_000_000))
        assertEquals(DayControl.Decision.NONE, DayControl.decide(instagram, 60, 1_000_000 - 60_000, 1_000_000))
        assertEquals(
            DayControl.Decision.PAUSE,
            DayControl.decide(instagram, 60, 1_000_000 - DayControl.PAUSE_GRACE_MS, 1_000_000)
        )
    }

    @Test
    fun `limit only rule without pause lets the app open`() =
        assertEquals(DayControl.Decision.NONE, DayControl.decide(instagram.copy(pauseOn = false), 10 * 60L, null, 0))
}
