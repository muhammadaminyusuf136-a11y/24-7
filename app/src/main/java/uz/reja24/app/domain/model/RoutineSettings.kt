package uz.reja24.app.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Rejim sozlamalari: qachon bloklash boshlanadi, qachon uxlash va uyg'onish.
 *
 * "Rejim kuni": tungi rejim yarim tundan o'tadi, shuning uchun kun kalendar bo'yicha
 * 00:00 da emas, ertasi kuni UYG'ONISH vaqtida yopiladi. Masalan uyg'onish 06:00 bo'lsa,
 * 25-sentabr 01:30 dagi favqulodda kirish 24-sentabr bahosiga yoziladi.
 */
data class RoutineSettings(
    val blockStartTime: LocalTime = LocalTime.of(22, 0),
    val sleepTime: LocalTime = LocalTime.of(23, 0),
    val wakeTime: LocalTime = LocalTime.of(6, 0),
    val isRoutineActive: Boolean = true,
    val reminderStage1Minutes: Int = 60,
    val reminderStage2Minutes: Int = 30,
    val reminderStage3Minutes: Int = 15,
    val finalWarningMinutes: Int = 1,
    val isVoiceReminderOn: Boolean = false,
    val ringtoneUri: String? = null,
    val isStrictModeOn: Boolean = false
) {
    /**
     * Berilgan vaqt blok oynasi ichidami.
     * Blok oynasi blockStartTime dan wakeTime gacha (odatda yarim tundan o'tadi).
     */
    fun isInBlockWindow(time: LocalTime): Boolean {
        if (!isRoutineActive) return false
        return inCircularRange(time.toMinutes(), blockStartTime.toMinutes(), wakeTime.toMinutes())
    }

    /** Favqulodda kirishga bir kechada necha marta ruxsat. */
    val maxDailyBypasses: Int get() = if (isStrictModeOn) 1 else 3

    /**
     * Telefon shu vaqtda ochilsa — "uyqu vaqtidan keyin ishlatildi" hisoblanadimi.
     * Uyqu vaqtidan 15 daqiqa keyin boshlanadi va uyg'onish oynasi boshlanguncha
     * (uyg'onishdan 1 soat oldingacha) davom etadi — yarim tundan keyingi soatlar ham kiradi.
     */
    fun isAfterBedtime(time: LocalTime): Boolean {
        if (!isRoutineActive) return false
        val start = Math.floorMod(sleepTime.toMinutes() + SLEEP_GRACE_MINUTES, MINUTES_IN_DAY)
        val end = Math.floorMod(wakeTime.toMinutes() - WAKE_EARLY_MINUTES, MINUTES_IN_DAY)
        return inCircularRange(time.toMinutes(), start, end)
    }

    /**
     * Telefon shu vaqtda ochilsa — "o'z vaqtida uyg'ondi" (uyg'onishdan 1 soat oldin — 30 daqiqa keyin).
     * Oyna kengroq bo'lsa, tunda (masalan 03:00 da) telefon ochilishi ham ball berib yuborardi.
     */
    fun isInWakeWindow(time: LocalTime): Boolean {
        val from = Math.floorMod(wakeTime.toMinutes() - WAKE_EARLY_MINUTES, MINUTES_IN_DAY)
        val to = Math.floorMod(wakeTime.toMinutes() + WAKE_LATE_MINUTES + 1, MINUTES_IN_DAY)
        return inCircularRange(time.toMinutes(), from, to)
    }

    /**
     * Shu paytdagi voqea (favqulodda kirish, uyqudan keyin telefon) qaysi kunning bahosiga yoziladi.
     * Uyg'onish vaqtigacha hali kechagi "rejim kuni" davom etmoqda.
     */
    fun routineDayFor(dateTime: LocalDateTime): LocalDate =
        if (dateTime.toLocalTime().isBefore(wakeTime)) dateTime.toLocalDate().minusDays(1)
        else dateTime.toLocalDate()

    /**
     * "Vaqtida uyg'ondi" qaysi kunga yoziladi — o'sha tongning kuniga.
     * Uyg'onish yarim tunga juda yaqin bo'lsa (masalan 00:30), oyna kechqurun boshlanadi —
     * u holda ertangi kunga yoziladi.
     */
    fun wakeDayFor(dateTime: LocalDateTime): LocalDate {
        val t = dateTime.toLocalTime().toMinutes()
        val from = Math.floorMod(wakeTime.toMinutes() - WAKE_EARLY_MINUTES, MINUTES_IN_DAY)
        val wrapsMidnight = from > wakeTime.toMinutes()
        return if (wrapsMidnight && t >= from) dateTime.toLocalDate().plusDays(1) else dateTime.toLocalDate()
    }

    companion object {
        const val SLEEP_GRACE_MINUTES = 15
        const val WAKE_EARLY_MINUTES = 60
        const val WAKE_LATE_MINUTES = 30
        private const val MINUTES_IN_DAY = 24 * 60

        private fun LocalTime.toMinutes(): Int = hour * 60 + minute

        /** [start, end) oralig'i — yarim tundan o'tishi mumkin (masalan 22:00 → 06:00). */
        private fun inCircularRange(t: Int, start: Int, end: Int): Boolean = when {
            start == end -> false
            start < end -> t in start until end
            else -> t >= start || t < end
        }
    }
}

/**
 * Rejim qulfi: bloklash vaqti davomida (masalan 22:00–06:00) ilova ichida blokni BO'SHASHTIRADIGAN
 * o'zgarishlar taqiqlanadi — aks holda odam 24/7 ga kirib, bloklangan ilovani ro'yxatdan chiqarib
 * yuborishi mumkin edi. Blokni kuchaytirish (yangi ilova qo'shish, qattiq rejimni yoqish) ruxsat.
 */
object RoutineLock {

    /**
     * Qulf bloklashdan PRELOCK_HOURS soat OLDIN boshlanadi va uyg'onishgacha davom etadi.
     * Sabab: odam bloklashga 5 daqiqa qolganda ilovani ro'yxatdan chiqarib olardi. Endi ro'yxat
     * va rejimni bo'shashtirish faqat ertalab/kunduzi — hushyor qaror bilan mumkin.
     */
    const val PRELOCK_HOURS = 6

    fun isLocked(settings: RoutineSettings, now: LocalTime): Boolean {
        if (!settings.isRoutineActive) return false
        val day = 24 * 60
        val block = settings.blockStartTime.hour * 60 + settings.blockStartTime.minute
        val wake = settings.wakeTime.hour * 60 + settings.wakeTime.minute
        val t = now.hour * 60 + now.minute
        // Uyg'oq vaqt qisqa bo'lsa, qulf uning yarmidan oshmaydi — o'zgartirish uchun har kuni oyna qoladi
        val awake = Math.floorMod(block - wake, day)
        val start = Math.floorMod(block - minOf(PRELOCK_HOURS * 60, awake / 2), day)
        return when {
            start == wake -> false
            start < wake -> t in start until wake
            else -> t >= start || t < wake
        }
    }

    /** Yangi sozlama blokni zaiflashtiradimi (o'chirish, vaqtlarni o'zgartirish, qattiq rejimni o'chirish). */
    fun weakens(old: RoutineSettings, new: RoutineSettings): Boolean =
        (old.isRoutineActive && !new.isRoutineActive) ||
            old.blockStartTime != new.blockStartTime ||
            old.sleepTime != new.sleepTime ||
            old.wakeTime != new.wakeTime ||
            (old.isStrictModeOn && !new.isStrictModeOn)
}
