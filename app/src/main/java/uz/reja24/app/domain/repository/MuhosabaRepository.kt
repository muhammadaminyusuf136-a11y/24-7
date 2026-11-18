package uz.reja24.app.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.reja24.app.domain.prayer.Prayer
import java.time.LocalDate

/** Muhosaba ro'yxatidagi band. */
data class MuhosabaItem(val id: Long, val title: String)

interface MuhosabaRepository {
    fun observeItems(): Flow<List<MuhosabaItem>>

    /** Kunlar bo'yicha belgilar: sana → (band id → bajarildimi). */
    fun observeMarks(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Map<Long, Boolean>>>

    suspend fun addItem(title: String)
    suspend fun removeItem(id: Long)

    /** done = null — belgini olib tashlash. */
    suspend fun setMark(itemId: Long, date: LocalDate, done: Boolean?)

    /** Kun tartibidagi "bajarilmadi" (✗) belgilari: sana → reja id'lari. */
    fun observeTaskMisses(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Set<Long>>>

    suspend fun setTaskMissed(entryId: Long, date: LocalDate, missed: Boolean)

    /** Namoz belgilari: sana → (namoz → holat). */
    fun observePrayerMarks(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Map<Prayer, PrayerStatus>>>

    /** status = null — belgini olib tashlash. */
    suspend fun setPrayerMark(date: LocalDate, prayer: Prayer, status: PrayerStatus?)

    /** Kechki yozuv (bo'lmasa — null). */
    fun observeNote(date: LocalDate): Flow<DayNote?>

    /** Ikkala maydon bo'sh bo'lsa — yozuv o'chiriladi. */
    suspend fun saveNote(date: LocalDate, good: String, improve: String)
}

/** Namoz holati: vaqtida, kechikib (vaqti chiqmasdan, lekin kech), qazo. */
enum class PrayerStatus { ON_TIME, LATE, MISSED }

/** Kechki qisqa yozuv. */
data class DayNote(val good: String, val improve: String)