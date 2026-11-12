package uz.reja24.app.domain.repository

import kotlinx.coroutines.flow.Flow
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
}