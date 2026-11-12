package uz.reja24.app.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.reja24.app.domain.model.TrophyLevel
import java.time.LocalDate

/** Ilovaning ichki holati (foydalanuvchi sozlamasi emas). */
interface AppStateRepository {
    /** Favqulodda ruxsat qachongacha amal qiladi (millisekund). 0 — ruxsat yo'q. */
    val bypassUntilMillis: Flow<Long>
    suspend fun getBypassUntilMillis(): Long
    suspend fun setBypassUntilMillis(millis: Long)

    /** Yangi olingan, hali tabriklanmagan kubok. */
    val pendingTrophy: Flow<TrophyLevel?>
    suspend fun setPendingTrophy(level: TrophyLevel?)

    /** Oxirgi baholangan kun (o'tkazib yuborilgan kunlarni keyin baholash uchun). */
    suspend fun getLastEvaluatedDate(): LocalDate?
    suspend fun setLastEvaluatedDate(date: LocalDate)
}
