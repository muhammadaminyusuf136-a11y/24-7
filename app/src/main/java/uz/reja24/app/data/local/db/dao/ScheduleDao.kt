package uz.reja24.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import uz.reja24.app.data.local.db.entity.ScheduleEntryEntity
import uz.reja24.app.data.local.db.entity.TaskCompletionEntity
import java.time.LocalDate

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM schedule_entries ORDER BY startTime, id")
    fun observeAll(): Flow<List<ScheduleEntryEntity>>

    @Query("SELECT * FROM schedule_entries WHERE isActive = 1 ORDER BY startTime, id")
    fun observeActive(): Flow<List<ScheduleEntryEntity>>

    @Query("SELECT * FROM schedule_entries WHERE isActive = 1 ORDER BY startTime, id")
    suspend fun getActive(): List<ScheduleEntryEntity>

    @Query("SELECT * FROM schedule_entries WHERE id = :id")
    suspend fun getById(id: Long): ScheduleEntryEntity?

    /** Yangi bo'lsa qo'shadi (yangi id qaytaradi), bor bo'lsa yangilaydi (-1 qaytaradi). */
    @Upsert
    suspend fun upsert(entry: ScheduleEntryEntity): Long

    @Query("DELETE FROM schedule_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE schedule_entries SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: Long, isActive: Boolean)
}

@Dao
interface TaskCompletionDao {

    @Query("SELECT * FROM task_completions WHERE date = :date")
    fun observeForDate(date: LocalDate): Flow<List<TaskCompletionEntity>>

    @Query("SELECT * FROM task_completions WHERE date BETWEEN :from AND :to")
    fun observeForRange(from: LocalDate, to: LocalDate): Flow<List<TaskCompletionEntity>>

    @Query("SELECT * FROM task_completions WHERE date = :date")
    suspend fun getForDate(date: LocalDate): List<TaskCompletionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(completion: TaskCompletionEntity)

    @Query("DELETE FROM task_completions WHERE entryId = :entryId AND date = :date")
    suspend fun delete(entryId: Long, date: LocalDate)
}
