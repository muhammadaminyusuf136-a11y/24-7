package uz.reja24.app.domain.repository

/** Eslatmalarni (budilniklarni) qayta rejalashtirish. Reja yoki rejim o'zgarganda chaqiriladi. */
interface ReminderScheduler {
    suspend fun rescheduleAll()
}
