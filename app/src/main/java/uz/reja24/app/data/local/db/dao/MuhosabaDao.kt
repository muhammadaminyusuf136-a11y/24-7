package uz.reja24.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import uz.reja24.app.data.local.db.entity.MuhosabaItemEntity
import uz.reja24.app.data.local.db.entity.MuhosabaMarkEntity
import java.time.LocalDate

@Dao
interface MuhosabaDao {

    @Query("SELECT * FROM muhosaba_items WHERE isArchived = 0 ORDER BY sortOrder, id")
    fun observeActiveItems(): Flow<List<MuhosabaItemEntity>>

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM muhosaba_items")
    suspend fun maxSortOrder(): Int

    @Insert
    suspend fun insertItem(item: MuhosabaItemEntity): Long

    @Query("UPDATE muhosaba_items SET isArchived = 1 WHERE id = :id")
    suspend fun archiveItem(id: Long)

    @Query("SELECT * FROM muhosaba_marks WHERE date BETWEEN :from AND :to")
    fun observeMarks(from: LocalDate, to: LocalDate): Flow<List<MuhosabaMarkEntity>>

    @Upsert
    suspend fun upsertMark(mark: MuhosabaMarkEntity)

    @Query("DELETE FROM muhosaba_marks WHERE itemId = :itemId AND date = :date")
    suspend fun deleteMark(itemId: Long, date: LocalDate)
}