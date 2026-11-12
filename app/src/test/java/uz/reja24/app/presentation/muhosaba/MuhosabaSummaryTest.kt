package uz.reja24.app.presentation.muhosaba

import org.junit.Assert.assertEquals
import org.junit.Test
import uz.reja24.app.domain.repository.MuhosabaItem
import java.time.LocalDate

class MuhosabaSummaryTest {

    private val today = LocalDate.of(2026, 11, 12)
    private val quran = MuhosabaItem(1, "Qur'on")
    private val prayer = MuhosabaItem(2, "Namoz")

    @Test
    fun counts_done_and_missed_days_inside_the_period_only() {
        val marks = mapOf(
            today to mapOf(1L to true, 2L to true),
            today.minusDays(1) to mapOf(1L to false, 2L to true),
            today.minusDays(6) to mapOf(1L to true),
            // 7 kunlik davrdan tashqarida
            today.minusDays(7) to mapOf(1L to true, 2L to false),
            today.minusDays(20) to mapOf(2L to false)
        )
        val week = MuhosabaSummary.summarize(listOf(quran, prayer), marks, today, 7)
        assertEquals(ItemSummary(quran, done = 2, missed = 1, days = 7), week[0])
        assertEquals(ItemSummary(prayer, done = 2, missed = 0, days = 7), week[1])
        val month = MuhosabaSummary.summarize(listOf(quran, prayer), marks, today, 30)
        assertEquals(3, month[0].done)
        assertEquals(2, month[1].missed)
    }

    @Test
    fun item_without_marks_has_zeroes() {
        val result = MuhosabaSummary.summarize(listOf(quran), emptyMap(), today, 7).single()
        assertEquals(0, result.done)
        assertEquals(0, result.missed)
    }
}