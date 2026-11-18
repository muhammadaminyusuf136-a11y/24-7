package uz.reja24.app.presentation.muhosaba

import org.junit.Assert.assertEquals
import org.junit.Test
import uz.reja24.app.domain.prayer.Prayer
import uz.reja24.app.domain.repository.PrayerStatus
import java.time.LocalDate

class PrayerTallyTest {

    private val today = LocalDate.of(2026, 11, 19)

    @Test
    fun `counts on time, late and missed prayers inside the period only`() {
        val marks = mapOf(
            today to mapOf(Prayer.FAJR to PrayerStatus.ON_TIME, Prayer.DHUHR to PrayerStatus.LATE),
            today.minusDays(6) to mapOf(Prayer.ISHA to PrayerStatus.MISSED),
            // Haftadan tashqarida — hisoblanmaydi
            today.minusDays(7) to mapOf(Prayer.ASR to PrayerStatus.MISSED),
            // Quyosh chiqishi farz emas — hisoblanmaydi
            today.minusDays(1) to mapOf(Prayer.SUNRISE to PrayerStatus.ON_TIME)
        )
        val week = MuhosabaSummary.prayerTally(marks, today, 7)
        assertEquals(PrayerTally(onTime = 1, late = 1, missed = 1, total = 35), week)

        val month = MuhosabaSummary.prayerTally(marks, today, 30)
        assertEquals(2, month.missed)
        assertEquals(150, month.total)
    }
}
