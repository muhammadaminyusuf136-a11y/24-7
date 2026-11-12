package uz.reja24.app.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class BlockPolicyTest {

    private val settings = RoutineSettings()          // 22:00 → 06:00
    private val blocked = setOf("com.instagram.android")
    private val whitelist = setOf("com.android.dialer", "com.instagram.android.fake")
    private val night = LocalTime.of(23, 30)
    private val now = 1_000_000L

    private fun check(
        pkg: String = "com.instagram.android",
        time: LocalTime = night,
        bypassUntil: Long = 0L,
        routine: RoutineSettings = settings,
        white: Set<String> = whitelist
    ) = BlockPolicy.shouldBlock(pkg, time, now, routine, blocked, white, bypassUntil)

    @Test
    fun `blocked app at night is blocked`() = assertTrue(check())

    @Test
    fun `blocked app during the day is allowed`() = assertFalse(check(time = LocalTime.NOON))

    @Test
    fun `app not in the list is allowed`() = assertFalse(check(pkg = "com.whatsapp"))

    @Test
    fun `whitelist always wins even if also in blocked list`() =
        assertFalse(check(white = whitelist + "com.instagram.android"))

    @Test
    fun `active bypass allows the app`() = assertFalse(check(bypassUntil = now + 60_000))

    @Test
    fun `expired bypass blocks again`() = assertTrue(check(bypassUntil = now - 1))

    @Test
    fun `inactive routine never blocks`() = assertFalse(check(routine = settings.copy(isRoutineActive = false)))

    @Test
    fun `allow-only mode blocks every launchable app except allowed and system ones`() {
        val launchable = setOf("com.whatsapp", "com.quran.app", "com.some.game")
        fun allowOnly(pkg: String, time: LocalTime = night) = BlockPolicy.shouldBlock(
            pkg, time, now, settings, blocked, whitelist, 0L,
            allowOnly = true, launchablePackages = launchable, allowedPackages = setOf("com.quran.app")
        )
        assertTrue(allowOnly("com.whatsapp"))
        assertTrue(allowOnly("com.some.game"))
        assertFalse(allowOnly("com.quran.app"))
        // Tizim qismlari (bosh ekranda yo'q) va oq ro'yxat — hech qachon
        assertFalse(allowOnly("com.android.systemui.something"))
        assertFalse(allowOnly("com.android.dialer"))
        // Ro'yxatdagi ilova baribir bloklanadi; kunduzi — hech narsa
        assertTrue(allowOnly("com.instagram.android"))
        assertFalse(allowOnly("com.whatsapp", LocalTime.NOON))
    }

    @Test
    fun `blank package is ignored`() = assertFalse(check(pkg = ""))
}
