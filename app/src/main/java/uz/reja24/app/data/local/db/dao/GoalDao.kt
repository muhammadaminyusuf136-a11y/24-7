package uz.reja24.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import uz.reja24.app.data.local.db.entity.GoalEntity

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals WHERE year = :year ORDER BY isAchieved, id")
    fun observeByYear(year: Int): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getById(id: Long): GoalEntity?

    /** Yangi bo'lsa yangi id, bor bo'lsa -1 qaytaradi. */
    @Upsert
    suspend fun upsert(goal: GoalEntity): Long

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: Long)
}
