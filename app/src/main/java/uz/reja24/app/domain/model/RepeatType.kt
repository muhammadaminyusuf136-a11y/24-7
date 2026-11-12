package uz.reja24.app.domain.model

/**
 * Reja qanchalik tez-tez takrorlanadi.
 *
 * ONCE      — bir marta, `specificDate` kuni
 * DAILY     — har kuni
 * WEEKDAYS  — ish kunlari (dushanba–juma)
 * WEEKLY    — har hafta, `daysOfWeek` da tanlangan kunlarda (bir yoki bir nechta)
 * MONTHLY   — har oy, `specificDate` ning sanasida (masalan har oyning 15-sida)
 * YEARLY    — har yili, `specificDate` ning oy va kunida
 */
enum class RepeatType {
    ONCE,
    DAILY,
    WEEKDAYS,
    WEEKLY,
    MONTHLY,
    YEARLY;

    companion object {
        fun fromName(name: String): RepeatType = entries.firstOrNull { it.name == name } ?: ONCE
    }
}
