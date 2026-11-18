package uz.reja24.app.domain.prayer

import java.time.LocalDateTime

/**
 * Ertalabki va kechki zikrlar. Namoz vaqtlari kabi avtomatik: bomdod va asr namozidan keyin
 * kun tartibiga qo'shiladi va vaqti kelganda eslatma (oyat yoki hadis bilan) chiqadi.
 */
enum class AzkarTime(val afterPrayer: Prayer) {
    MORNING(Prayer.FAJR),
    EVENING(Prayer.ASR);

    companion object {
        /** Namoz vaqti kirgandan shuncha daqiqa keyin (namoz o'qib bo'lingach) zikr boshlanadi. */
        const val OFFSET_MINUTES = 20L
        /** Kun tartibida zikrga ajratiladigan vaqt. */
        const val DURATION_MINUTES = 15L

        fun fromName(name: String?): AzkarTime? = entries.firstOrNull { it.name == name }
    }

    /** Shu kundagi zikr boshlanish vaqti. */
    fun startAt(day: PrayerDay): LocalDateTime = day.time(afterPrayer).plusMinutes(OFFSET_MINUTES)
}
